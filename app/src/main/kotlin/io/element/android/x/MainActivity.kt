/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.x

import android.content.Intent
import androidx.core.content.FileProvider
import android.content.ClipData
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.element.android.libraries.matrix.api.diagnostics.StartupTrace
import io.element.android.libraries.matrix.api.user.SyncMeAppVisibility
import java.io.File
import android.os.Bundle
import android.os.SystemClock
import java.util.concurrent.atomic.AtomicBoolean
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumble.appyx.core.integrationpoint.NodeActivity
import com.bumble.appyx.core.plugin.NodeReadyObserver
import io.element.android.compound.colors.SemanticColorsLightDark
import io.element.android.compound.theme.ElementTheme
import io.element.android.features.lockscreen.api.LockScreenEntryPoint
import io.element.android.features.lockscreen.api.LockScreenLockState
import io.element.android.features.lockscreen.api.LockScreenService
import io.element.android.features.lockscreen.api.handleSecureFlag
import io.element.android.libraries.architecture.appyx.DebugNavStateNodeHost
import io.element.android.libraries.architecture.bindings
import io.element.android.libraries.core.log.logger.LoggerTag
import io.element.android.libraries.designsystem.theme.ElementThemeApp
import io.element.android.libraries.designsystem.utils.snackbar.LocalSnackbarDispatcher
import io.element.android.services.analytics.compose.LocalAnalyticsService
import io.element.android.x.di.AppBindings
import io.element.android.x.intent.SafeUriHandler
import kotlinx.coroutines.launch
import timber.log.Timber

private val loggerTag = LoggerTag("MainActivity")

class MainActivity : NodeActivity() {
    private lateinit var mainNode: MainNode
    private lateinit var appBindings: AppBindings

    override fun onCreate(savedInstanceState: Bundle?) {
        StartupTrace.mark("activity_onCreate_start")
        Timber.tag(loggerTag.value).d("onCreate, with savedInstanceState: ${savedInstanceState != null}")
        installSplashScreen()
        StartupTrace.mark("splash_installed")
        super.onCreate(savedInstanceState)
        appBindings = bindings()
        setupLockManagement(appBindings.lockScreenService(), appBindings.lockScreenEntryPoint())
        enableEdgeToEdge()
        StartupTrace.mark("setContent_begin")
        setContent {
            MainContent(appBindings)
        }
        StartupTrace.mark("setContent_return")
        observeFirstDraw()
        if (intent?.action == "io.syncme.EXPORT_STARTUP_TRACE") exportStartupTrace()

        val activity = this
        appBindings.appStartupHooks().forEach {
            lifecycleScope.launch {
                it.onAppStartup(activity)
            }
        }
    }

    /**
     * The pre-Android 12 way into picture-in-picture for a native call.
     *
     * Everything else about it is installed where the call is drawn, which knows whose call it is.
     * This cannot be: an Activity override has no session in scope. From Android 12 the system is told
     * up front that this Activity would like to shrink, and the call handles it without ever coming
     * through here.
     */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        appBindings.nativeCallPip().onUserLeaveHint(this)
    }

    @Composable
    private fun MainContent(appBindings: AppBindings) {
        StartupTrace.mark("MainContent_composed")
        val migrationState = appBindings.migrationEntryPoint().present()
        val colors by remember {
            appBindings.enterpriseService().semanticColorsFlow(sessionId = null)
        }.collectAsState(SemanticColorsLightDark.default)
        ElementThemeApp(
            appPreferencesStore = appBindings.preferencesStore(),
            featureFlagService = appBindings.featureFlagService(),
            compoundLight = colors.light,
            compoundDark = colors.dark,
            buildMeta = appBindings.buildMeta()
        ) {
            CompositionLocalProvider(
                LocalSnackbarDispatcher provides appBindings.snackbarDispatcher(),
                LocalUriHandler provides SafeUriHandler(this),
                LocalAnalyticsService provides appBindings.analyticsService(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ElementTheme.colors.bgCanvasDefault),
                ) {
                    if (migrationState.migrationAction.isSuccess()) {
                        MainNodeHost()
                    } else {
                        appBindings.migrationEntryPoint().Render(
                            state = migrationState,
                            modifier = Modifier,
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun MainNodeHost() {
        // TODO this is a temporary helper to capture the nav state in a more readable format for crash reports
        // Revert to `NodeHost` once this is fixed
        DebugNavStateNodeHost(integrationPoint = appyxV1IntegrationPoint) {
            MainNode(
                it,
                plugins = listOf(
                    object : NodeReadyObserver<MainNode> {
                        override fun init(node: MainNode) {
                            Timber.tag(loggerTag.value).d("onMainNodeInit")
                            StartupTrace.mark("main_node_ready")
                            mainNode = node
                            mainNode.handleIntent(intent)
                        }
                    },
                ),
                context = applicationContext
            )
        }
    }

    private fun setupLockManagement(
        lockScreenService: LockScreenService,
        lockScreenEntryPoint: LockScreenEntryPoint
    ) {
        lockScreenService.handleSecureFlag(this)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                lockScreenService.lockState.collect { state ->
                    if (state == LockScreenLockState.Locked) {
                        startActivity(lockScreenEntryPoint.pinUnlockIntent(this@MainActivity))
                    }
                }
            }
        }
    }

    /**
     * Called when:
     * - the launcher icon is clicked (if the app is already running);
     * - a notification is clicked.
     * - a deep link have been clicked
     * - the app is going to background (<- this is strange)
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Timber.tag(loggerTag.value).d("onNewIntent")
        // If the mainNode is not init yet, keep the intent for later.
        // It can happen when the activity is killed by the system. The methods are called in this order :
        // onCreate(savedInstanceState=true) -> onNewIntent -> onResume -> onMainNodeInit
        if (intent.action == "io.syncme.EXPORT_STARTUP_TRACE") {
            exportStartupTrace()
            return
        }
        if (::mainNode.isInitialized) {
            mainNode.handleIntent(intent)
        } else {
            setIntent(intent)
        }
    }

    private val firstDrawSeen = AtomicBoolean(false)

    private fun observeFirstDraw() {
        val decor = window.decorView
        val observer = decor.viewTreeObserver
        val listener = object : android.view.ViewTreeObserver.OnDrawListener {
            override fun onDraw() {
                if (firstDrawSeen.compareAndSet(false, true)) {
                    StartupTrace.mark("first_window_draw")
                    decor.post {
                        if (decor.viewTreeObserver.isAlive) {
                            decor.viewTreeObserver.removeOnDrawListener(this)
                        }
                    }
                }
            }
        }
        observer.addOnDrawListener(listener)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        StartupTrace.mark(if (hasFocus) "window_focus_gained" else "window_focus_lost")
    }

    override fun onStart() {
        super.onStart()
        SyncMeAppVisibility.setForeground(true)
        StartupTrace.mark("presence_app_foreground")
    }

    override fun onStop() {
        SyncMeAppVisibility.setForeground(false)
        StartupTrace.mark("presence_app_background")
        StartupTrace.mark("activity_onStop")
        super.onStop()
    }

    private fun exportStartupTrace() {
        StartupTrace.mark("report_view_requested")
        lifecycleScope.launch {
            val report = withContext(Dispatchers.IO) {
                kotlinx.coroutines.delay(180)
                runCatching {
                    StartupTrace.readRecentReports().takeLast(500_000)
                        ?: "No startup diagnostics have been recorded yet."
                }.getOrElse { "Unable to read diagnostics: " + it.javaClass.simpleName }
            }
            // Native dialog is independent of Compose navigation and file sharing providers.
            val textView = android.widget.TextView(this@MainActivity).apply {
                text = report
                textSize = 12f
                setTextIsSelectable(true)
                setPadding(24, 16, 24, 16)
                typeface = android.graphics.Typeface.MONOSPACE
            }
            val scroll = android.widget.ScrollView(this@MainActivity).apply {
                addView(textView)
            }
            android.app.AlertDialog.Builder(this@MainActivity)
                .setTitle("SyncMe startup diagnostics")
                .setView(scroll)
                .setPositiveButton("Copy All") { _, _ ->
                    val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                        as android.content.ClipboardManager
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("SyncMe diagnostics", report))
                    Toast.makeText(this@MainActivity, "Diagnostics copied", Toast.LENGTH_SHORT).show()
                }
                .setNeutralButton("Clear Logs") { _, _ ->
                    android.app.AlertDialog.Builder(this@MainActivity)
                        .setTitle("Clear startup logs?")
                        .setMessage("Delete all saved startup diagnostics? Your chats, account and Matrix cache will not be affected.")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Clear") { _, _ ->
                            lifecycleScope.launch {
                                val cleared = withContext(Dispatchers.IO) {
                                    StartupTrace.clearReports()
                                }
                                Toast.makeText(
                                    this@MainActivity,
                                    if (cleared) "Startup logs cleared" else "Unable to clear logs",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        }
                        .show()
                }
                .setNegativeButton("Close", null)
                .show()
        }
    }

    override fun onPause() {
        super.onPause()
        StartupTrace.mark("activity_onPause")
        Timber.tag(loggerTag.value).d("onPause")
    }

    override fun onResume() {
        super.onResume()
        StartupTrace.mark("activity_onResume")
        Timber.tag(loggerTag.value).d("onResume")
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.tag(loggerTag.value).d("onDestroy")
    }
}
