/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.onboarding

import androidx.compose.foundation.Image
import android.widget.ImageView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.login.impl.R
import io.element.android.features.login.impl.login.LoginModeEvent
import io.element.android.features.login.impl.login.LoginModeView
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.designsystem.atomic.atoms.ElementLogoAtom
import io.element.android.libraries.designsystem.atomic.atoms.ElementLogoAtomSize
import io.element.android.libraries.designsystem.atomic.molecules.ButtonColumnMolecule
import io.element.android.libraries.designsystem.atomic.pages.FlowStepPage
import io.element.android.libraries.designsystem.atomic.pages.OnBoardingPage
import io.element.android.libraries.designsystem.components.BigIcon
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Button
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.IconButton
import io.element.android.libraries.designsystem.theme.components.IconSource
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextButton
import io.element.android.libraries.matrix.api.auth.OAuthDetails
import io.element.android.libraries.permissions.api.localnetwork.LocalNetworkPermissionDialogView
import io.element.android.libraries.testtags.TestTags
import io.element.android.libraries.testtags.testTag
import io.element.android.libraries.ui.strings.CommonStrings

/**
 * Ref: https://www.figma.com/design/pDlJZGBsri47FNTXMnEdXB/Compound-Android-Templates?node-id=41-6503
 */
@Composable
fun OnBoardingView(
    state: OnBoardingState,
    onBackClick: () -> Unit,
    onDeveloperSettingsClick: () -> Unit,
    onSignInWithQrCode: () -> Unit,
    onSignIn: (mustChooseAccountProvider: Boolean) -> Unit,
    onCreateAccount: () -> Unit,
    onOAuthDetails: (OAuthDetails) -> Unit,
    onNeedLoginPassword: () -> Unit,
    onLearnMoreClick: () -> Unit,
    onReportProblem: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val loginView = @Composable {
        LoginModeView(
            loginMode = state.loginModeState.loginMode,
            onClearError = {
                state.eventSink(OnBoardingEvent.ClearError)
            },
            onLearnMoreClick = onLearnMoreClick,
            onOAuthDetails = onOAuthDetails,
            onNeedLoginPassword = onNeedLoginPassword,
        )
        LocalNetworkPermissionDialogView(
            dialog = state.loginModeState.localNetworkPermissionDialog,
            onSubmit = {
                state.loginModeState.eventSink(LoginModeEvent.RequestLocalNetworkPermission)
            },
            onDismiss = {
                state.loginModeState.eventSink(LoginModeEvent.DismissLocalNetworkPermission)
            }
        )
    }
    val buttons = @Composable {
        OnBoardingButtons(
            state = state,
            onSignInWithQrCode = onSignInWithQrCode,
            onSignIn = onSignIn,
            onCreateAccount = onCreateAccount,
            onReportProblem = onReportProblem,
        )
    }

    if (state.isAddingAccount) {
        AddOtherAccountScaffold(
            modifier = modifier,
            loginView = loginView,
            buttons = buttons,
            onBackClick = onBackClick,
        )
    } else {
        AddFirstAccountScaffold(
            modifier = modifier,
            state = state,
            loginView = loginView,
            buttons = buttons,
            onBackClick = onBackClick,
            onDeveloperSettingsClick = onDeveloperSettingsClick,
        )
    }
}

@Composable
private fun AddFirstAccountScaffold(
    state: OnBoardingState,
    loginView: @Composable () -> Unit,
    buttons: @Composable () -> Unit,
    onBackClick: () -> Unit,
    onDeveloperSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OnBoardingPage(
        modifier = modifier,
        renderBackground = true,
        content = {
            Box(
                modifier = Modifier.fillMaxSize(),
            ) {
                OnBoardingContent(state = state)
                if (state.showDeveloperSettings) {
                    IconButton(
                        onClick = onDeveloperSettingsClick,
                        modifier = Modifier
                            .align(Alignment.TopStart),
                    ) {
                        Icon(
                            imageVector = CompoundIcons.SettingsSolid(),
                            contentDescription = stringResource(CommonStrings.common_developer_options),
                        )
                    }
                }
                if (state.showBackButton) {
                    // Add icon button to "navigate back"
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .align(Alignment.TopEnd),
                    ) {
                        Icon(
                            imageVector = CompoundIcons.Close(),
                            contentDescription = stringResource(CommonStrings.action_cancel),
                        )
                    }
                }
            }
            loginView()
        },
        footer = {
            buttons()
        }
    )
}

@Composable
private fun AddOtherAccountScaffold(
    loginView: @Composable () -> Unit,
    buttons: @Composable () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowStepPage(
        modifier = modifier,
        title = stringResource(CommonStrings.common_add_account),
        iconStyle = BigIcon.Style.Default(CompoundIcons.HomeSolid()),
        buttons = { buttons() },
        content = loginView,
        onBackClick = onBackClick,
    )
}

@Composable
private fun OnBoardingContent(state: OnBoardingState) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = CenterHorizontally,
        ) {
            // Use the full-resolution SyncMe artwork bundled as drawable-nodpi.
            // The launcher icon supplied by PackageManager may be a density-scaled
            // adaptive-icon raster and visibly blurry when enlarged.
            AndroidView(
                modifier = Modifier.height(132.dp).fillMaxWidth(),
                factory = { context ->
                    ImageView(context).apply {
                        val fullResolution = context.resources.getIdentifier(
                            "syncme_launcher", "drawable", context.packageName
                        )
                        if (fullResolution != 0) {
                            // The supplied artwork has a dark square baked into the image.
                            // Extract the luminous SyncMe symbol on a transparent bitmap
                            // instead of drawing the complete square over the welcome gradient.
                            val original = android.graphics.BitmapFactory.decodeResource(context.resources, fullResolution)
                            if (original != null) {
                                val transparent = original.copy(android.graphics.Bitmap.Config.ARGB_8888, true)
                                val pixels = IntArray(transparent.width * transparent.height)
                                transparent.getPixels(pixels, 0, transparent.width, 0, 0, transparent.width, transparent.height)
                                for (i in pixels.indices) {
                                    val pixel = pixels[i]
                                    val red = android.graphics.Color.red(pixel)
                                    val green = android.graphics.Color.green(pixel)
                                    val blue = android.graphics.Color.blue(pixel)
                                    val brightness = maxOf(red, green, blue)
                                    // Keep bright cyan/blue strokes; fade the dark baked-in background.
                                    val opacity = ((brightness - 65) * 255 / 95).coerceIn(0, 255)
                                    pixels[i] = android.graphics.Color.argb(
                                        opacity * android.graphics.Color.alpha(pixel) / 255,
                                        red, green, blue
                                    )
                                }
                                transparent.setPixels(pixels, 0, transparent.width, 0, 0, transparent.width, transparent.height)
                                setImageBitmap(transparent)
                            } else {
                                setImageResource(fullResolution)
                            }
                        } else {
                            setImageDrawable(context.packageManager.getApplicationIcon(context.packageName))
                        }
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        adjustViewBounds = true
                        contentDescription = "SyncMe"
                    }
                },
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "SyncMe",
                color = ElementTheme.colors.textPrimary,
                style = ElementTheme.typography.fontHeadingLgBold,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Stay connected, Stay private",
                color = ElementTheme.colors.textPrimary,
                style = ElementTheme.typography.fontBodyMdRegular,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Messaging made simple",
                color = ElementTheme.colors.textSecondary,
                style = ElementTheme.typography.fontBodyMdRegular,
                textAlign = TextAlign.Center,
            )
        }
        Text(
            text = "by 0x07c4",
            color = ElementTheme.colors.textSecondary,
            style = ElementTheme.typography.fontBodyMdRegular,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
        )
    }
}

@Composable
private fun OnBoardingLogo(
    onBoardingLogoResId: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = onBoardingLogoResId),
            contentDescription = null
        )
    }
}

@Composable
private fun OnBoardingButtons(
    state: OnBoardingState,
    onSignInWithQrCode: () -> Unit,
    onSignIn: (mustChooseAccountProvider: Boolean) -> Unit,
    onCreateAccount: () -> Unit,
    onReportProblem: () -> Unit,
) {
    val isLoading by remember(state.loginModeState.loginMode) {
        derivedStateOf {
            state.loginModeState.loginMode is AsyncData.Loading
        }
    }

    ButtonColumnMolecule {
        val defaultAccountProvider = state.defaultAccountProvider
        Button(
            text = "Login",
            showProgress = isLoading,
            onClick = {
                if (defaultAccountProvider != null) {
                    state.eventSink(OnBoardingEvent.OnSignIn(defaultAccountProvider))
                } else {
                    onSignIn(state.mustChooseAccountProvider)
                }
            },
            enabled = defaultAccountProvider == null || state.submitEnabled || isLoading,
            modifier = Modifier.fillMaxWidth().testTag(TestTags.onBoardingSignIn),
        )
    }
}

@PreviewsDayNight
@Composable
internal fun OnBoardingViewPreview(
    @PreviewParameter(OnBoardingStatePreviewParam::class) state: OnBoardingState
) = ElementPreview {
    OnBoardingView(
        state = state,
        onBackClick = {},
        onDeveloperSettingsClick = {},
        onSignInWithQrCode = {},
        onSignIn = {},
        onCreateAccount = {},
        onReportProblem = {},
        onOAuthDetails = {},
        onNeedLoginPassword = {},
        onLearnMoreClick = {},
    )
}
