/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.root

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.preferences.impl.R
import io.element.android.features.preferences.impl.account.PreferencesAccountView
import io.element.android.features.preferences.impl.user.UserPreferences
import io.element.android.features.preferences.impl.userstatus.UserStatusState
import io.element.android.features.preferences.impl.userstatus.UserStatusView
import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.architecture.coverage.ExcludeFromCoverage
import io.element.android.libraries.designsystem.components.async.AsyncActionIndicator
import io.element.android.libraries.designsystem.components.async.AsyncIndicator
import io.element.android.libraries.designsystem.components.avatar.AvatarRow
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.designsystem.components.avatar.AvatarType
import io.element.android.libraries.designsystem.components.list.ListItemContent
import io.element.android.libraries.designsystem.components.preferences.PreferenceCategory
import io.element.android.libraries.designsystem.components.preferences.PreferenceDropdown
import io.element.android.libraries.designsystem.components.preferences.PreferencePage
import io.element.android.libraries.designsystem.preview.ElementPreviewDark
import io.element.android.libraries.designsystem.preview.ElementPreviewLight
import io.element.android.libraries.designsystem.preview.PreviewWithLargeHeight
import io.element.android.libraries.designsystem.theme.components.HorizontalDivider
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.IconSource
import io.element.android.libraries.designsystem.theme.components.ListItem
import io.element.android.libraries.designsystem.theme.components.ListSectionHeader
import io.element.android.libraries.designsystem.theme.components.ListItemStyle
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.utils.CommonDrawables
import io.element.android.libraries.designsystem.utils.snackbar.SnackbarHost
import io.element.android.libraries.designsystem.utils.snackbar.rememberSnackbarHostState
import io.element.android.libraries.emoji.api.picker.EmojiPickerRenderer
import io.element.android.libraries.emoji.api.picker.NoOpEmojiPickerRenderer
import io.element.android.libraries.matrix.api.user.MatrixUser
import io.element.android.libraries.matrix.ui.components.MatrixUserRow
import io.element.android.libraries.matrix.ui.model.getAvatarData
import io.element.android.libraries.testtags.TestTags
import io.element.android.libraries.testtags.testTag
import io.element.android.libraries.ui.strings.CommonStrings
import kotlinx.collections.immutable.toImmutableList

@Composable
fun PreferencesRootView(
    state: PreferencesRootState,
    emojiPickerRenderer: EmojiPickerRenderer,
    onBackClick: () -> Unit,
    onAddAccountClick: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onOpenLockScreenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenDeveloperSettings: () -> Unit,
    onOpenMediaSettings: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onOpenLabs: () -> Unit,
    onEditProfileClick: (MatrixUser) -> Unit,
    onSecureBackupClick: () -> Unit,
    onManageAccountClick: (url: String) -> Unit,
    onLinkNewDeviceClick: () -> Unit,
    onOpenRageShake: () -> Unit,
    onModerationAndSafetyClick: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onSignOutClick: () -> Unit,
    onDeactivateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = rememberSnackbarHostState(snackbarMessage = state.snackbarMessage)

    Box(modifier = modifier) {
        // Include pref from other modules
        PreferencePage(
            onBackClick = onBackClick,
            title = stringResource(id = CommonStrings.common_settings),
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) {
            UserPreferences(
                modifier = Modifier
                    .testTag(TestTags.settingsUserProfile)
                    .clickable {
                        onEditProfileClick(state.myUser)
                    },
                matrixUser = state.myUser,
            )
            if (state.userStatusState != null) {
                UserStatusSection(
                    userStatusState = state.userStatusState,
                    emojiPickerRenderer = emojiPickerRenderer,
                )
            }
            if (state.isMultiAccountEnabled) {
                MultiAccountSection(
                    state = state,
                    onAddAccountClick = onAddAccountClick,
                )
            }
            HorizontalDivider()
            // Account settings section
            PreferencesAccountView(
                state = state.preferencesAccountState,
                onSecureBackupClick = onSecureBackupClick,
                onManageAccountClick = onManageAccountClick,
                onLinkNewDeviceClick = onLinkNewDeviceClick,
                onOpenRageShake = onOpenRageShake,
                onModerationAndSafetyClick = onModerationAndSafetyClick,
                onOpenNotificationSettings = onOpenNotificationSettings,
                onSignOutClick = onSignOutClick,
                onDeactivateClick = onDeactivateClick,
            )
            // 'App settings' section
            AppSettingsSection(
                state = state,
                onOpenLockScreenSettings = onOpenLockScreenSettings,
                onOpenMediaSettings = onOpenMediaSettings,
                onOpenLocationSettings = onOpenLocationSettings,
            )
            DestructiveAccountSection(
                canDeactivateAccount = state.preferencesAccountState.canDeactivateAccount,
                onSignOutClick = onSignOutClick,
                onDeactivateClick = onDeactivateClick,
            )
            // General section
            GeneralSection(
                state = state,
                onOpenAbout = onOpenAbout,
                onOpenAnalytics = onOpenAnalytics,
                onOpenDeveloperSettings = onOpenDeveloperSettings,
                onOpenLabs = onOpenLabs,
            )
            // Version
            Footer(
                version = state.version,
                onClick = if (!state.showDeveloperSettings) {
                    { state.eventSink(PreferencesRootEvent.OnVersionInfoClick) }
                } else {
                    null
                }
            )
        }
        state.userStatusState?.let {
            UserStatusUpdateIndicator(it.updateStatusAction)
        }
    }
}

@Composable
private fun BoxScope.UserStatusUpdateIndicator(updateStatusAction: AsyncAction<Unit>) {
    AsyncActionIndicator(
        asyncAction = updateStatusAction,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .statusBarsPadding(),
        loading = { AsyncIndicator.Loading(text = stringResource(CommonStrings.common_saving)) },
        failure = { _ -> AsyncIndicator.Failure(text = stringResource(CommonStrings.common_failed)) },
    )
}

@Composable
private fun ColumnScope.UserStatusSection(
    userStatusState: UserStatusState,
    emojiPickerRenderer: EmojiPickerRenderer,
) {
    HorizontalDivider(
        thickness = 1.dp,
        color = ElementTheme.colors.bgSubtleSecondary,
    )
    UserStatusView(
        state = userStatusState,
        emojiPickerRenderer = emojiPickerRenderer,
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * Ref: https://www.figma.com/design/G1xy0HDZKJf5TCRFmKb5d5/Compound-Android-Components?node-id=5414-4759
 */
@Composable
private fun ColumnScope.MultiAccountSection(
    state: PreferencesRootState,
    onAddAccountClick: () -> Unit,
) {
    if (state.otherSessions.isEmpty()) {
        HorizontalDivider()
        if (state.otherSessions.size + 1 < 3) AddAccountItem(onAddAccountClick)
    } else {
        val expandedStateDescription = if (state.isOtherAccountsSectionExpanded) {
            stringResource(CommonStrings.a11y_state_expanded)
        } else {
            stringResource(CommonStrings.a11y_state_collapsed)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { stateDescription = expandedStateDescription }
                .clickable { state.eventSink(PreferencesRootEvent.ToggleOtherAccountsExpanded) },
        ) {
            ListSectionHeader(
                title = stringResource(CommonStrings.common_switch_account),
                hasDivider = true,
            )
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    // Center on header text; header reserves space above its top divider.
                    .padding(end = 16.dp, top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AnimatedVisibility(
                    visible = !state.isOtherAccountsSectionExpanded,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    AvatarRow(
                        avatarDataList = state.otherSessions
                            .take(3)
                            .map { it.getAvatarData(AvatarSize.OtherAccountItem) }
                            .toImmutableList(),
                        avatarType = AvatarType.User,
                        lastOnTop = true,
                    )
                }
                val rotation: Float by animateFloatAsState(
                    targetValue = if (state.isOtherAccountsSectionExpanded) -180f else 0f,
                    animationSpec = tween(delayMillis = 0, durationMillis = 300),
                    label = "chevron",
                )
                Icon(
                    modifier = Modifier.rotate(rotation),
                    imageVector = CompoundIcons.ChevronDown(),
                    contentDescription = null,
                )
            }
        }
        AnimatedVisibility(
            visible = state.isOtherAccountsSectionExpanded,
        ) {
            Column {
                state.otherSessions.forEach { matrixUser ->
                    MatrixUserRow(
                        modifier = Modifier
                            .clickable {
                                state.eventSink(PreferencesRootEvent.SwitchToSession(matrixUser.userId))
                            }
                            .padding(top = 2.dp, bottom = 2.dp, end = 8.dp),
                        matrixUser = matrixUser,
                        avatarSize = AvatarSize.AccountItem,
                        verticalSpaceWidth = 16.dp,
                    )
                }
                if (state.otherSessions.size + 1 < 3) AddAccountItem(onAddAccountClick)
            }
        }
    }
}

@Composable
private fun AddAccountItem(onAddAccountClick: () -> Unit) {
    ListItem(
        leadingContent = ListItemContent.Icon(IconSource.Vector(CompoundIcons.Plus())),
        content = {
            Text(stringResource(CommonStrings.common_add_another_account))
        },
        onClick = onAddAccountClick,
    )
}

@Composable
private fun AppSettingsSection(
    state: PreferencesRootState,
    onOpenLockScreenSettings: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onOpenMediaSettings: () -> Unit,
) {
    var showPresenceDialog by remember { mutableStateOf(false) }
    if (showPresenceDialog) {
        AlertDialog(
            onDismissRequest = { showPresenceDialog = false },
            title = { Text("Last Seen & Online") },
            text = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            state.eventSink(PreferencesRootEvent.SetAppearOffline(false))
                            showPresenceDialog = false
                        }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = !state.appearOffline, onClick = {
                            state.eventSink(PreferencesRootEvent.SetAppearOffline(false))
                            showPresenceDialog = false
                        })
                        Text("Show My Activity", modifier = Modifier.padding(start = 8.dp))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            state.eventSink(PreferencesRootEvent.SetAppearOffline(true))
                            showPresenceDialog = false
                        }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = state.appearOffline, onClick = {
                            state.eventSink(PreferencesRootEvent.SetAppearOffline(true))
                            showPresenceDialog = false
                        })
                        Text("Appear Offline", modifier = Modifier.padding(start = 8.dp))
                    }
                    Text(
                        "Appear Offline is a client-side presence setting, not guaranteed last-seen privacy across Matrix devices.",
                        style = ElementTheme.typography.fontBodySmRegular,
                        color = ElementTheme.colors.textSecondary,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPresenceDialog = false }) { Text("Done") }
            },
        )
    }
    PreferenceCategory(
        title = stringResource(CommonStrings.common_app_settings),
    ) {
        PreferenceDropdown(
            icon = CompoundIcons.DarkMode(),
            dropDownIcon = CompoundIcons.ChevronUpDown(),
            title = stringResource(id = CommonStrings.common_appearance),
            selectedOption = state.theme,
            options = state.availableThemeOptions,
            onSelectOption = { themeOption ->
                state.eventSink(PreferencesRootEvent.SetTheme(themeOption))
            }
        )
ListItem(
            content = { Text("Last Seen & Online") },
            supportingContent = { Text(if (state.appearOffline) "Appear Offline" else "Show My Activity") },
            leadingContent = ListItemContent.Icon(IconSource.Vector(CompoundIcons.Lock())),
            onClick = { showPresenceDialog = true },
        )
        ListItem(
            content = { Text(stringResource(id = CommonStrings.common_screen_lock)) },
            leadingContent = ListItemContent.Icon(IconSource.Vector(CompoundIcons.Lock())),
            onClick = onOpenLockScreenSettings,
        )
        ListItem(
            content = { Text(stringResource(id = CommonStrings.common_location_sharing)) },
            leadingContent = ListItemContent.Icon(IconSource.Vector(CompoundIcons.LocationPin())),
            onClick = onOpenLocationSettings,
        )
    }
}

@Composable
private fun ColumnScope.DestructiveAccountSection(
    canDeactivateAccount: Boolean,
    onSignOutClick: () -> Unit,
    onDeactivateClick: () -> Unit,
) {
    HorizontalDivider(
        thickness = 1.dp,
        color = ElementTheme.colors.bgSubtleSecondary,
    )
    ListItem(
        content = { Text(stringResource(id = CommonStrings.action_signout)) },
        leadingContent = ListItemContent.Icon(IconSource.Vector(CompoundIcons.Close())),
        style = ListItemStyle.Destructive,
        onClick = onSignOutClick,
    )
    if (canDeactivateAccount) {
        ListItem(
            content = { Text(stringResource(id = CommonStrings.action_delete_account)) },
            leadingContent = ListItemContent.Icon(IconSource.Vector(CompoundIcons.Delete())),
            style = ListItemStyle.Destructive,
            onClick = onDeactivateClick,
        )
    }
}

@Composable
private fun ColumnScope.GeneralSection(
    state: PreferencesRootState,
    onOpenAbout: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onOpenLabs: () -> Unit,
    onOpenDeveloperSettings: () -> Unit,
) {
    HorizontalDivider(
        thickness = 1.dp,
        color = ElementTheme.colors.bgSubtleSecondary,
    )
    // SyncMe intentionally hides Analytics and Labs from the user-facing settings menu.
    ListItem(
        content = { Text(stringResource(id = CommonStrings.common_about)) },
        leadingContent = ListItemContent.Icon(IconSource.Vector(CompoundIcons.Info())),
        onClick = onOpenAbout,
    )
    // SyncMe developer options appear only after five taps on the version.
    AnimatedVisibility(
        visible = state.showDeveloperSettings,
    ) {
        DeveloperPreferencesView(onOpenDeveloperSettings)
    }
}

@Composable
private fun ColumnScope.Footer(
    version: String,
    onClick: (() -> Unit)?,
) {
    Text(
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .clickable(enabled = onClick != null, onClick = onClick ?: {})
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        textAlign = TextAlign.Center,
        text = version,
        style = ElementTheme.typography.fontBodySmRegular,
        color = ElementTheme.colors.textSecondary,
    )
}

@Composable
private fun DeveloperPreferencesView(onOpenDeveloperSettings: () -> Unit) {
    ListItem(
        content = { Text(stringResource(id = CommonStrings.common_developer_options)) },
        leadingContent = ListItemContent.Icon(IconSource.Vector(CompoundIcons.Code())),
        onClick = onOpenDeveloperSettings
    )
}

@PreviewWithLargeHeight
@Composable
internal fun PreferencesRootViewLightPreview(@PreviewParameter(PreferencesRootStatePreviewParam::class) state: PreferencesRootState) =
    ElementPreviewLight(
        drawableFallbackForImages = CommonDrawables.sample_avatar,
    ) { ContentToPreview(state) }

@PreviewWithLargeHeight
@Composable
internal fun PreferencesRootViewDarkPreview(@PreviewParameter(PreferencesRootStatePreviewParam::class) state: PreferencesRootState) =
    ElementPreviewDark(
        drawableFallbackForImages = CommonDrawables.sample_avatar,
    ) { ContentToPreview(state) }

@ExcludeFromCoverage
@Composable
private fun ContentToPreview(state: PreferencesRootState) {
    PreferencesRootView(
        state = state,
        emojiPickerRenderer = NoOpEmojiPickerRenderer,
        onBackClick = {},
        onAddAccountClick = {},
        onOpenAnalytics = {},
        onOpenDeveloperSettings = {},
        onOpenLocationSettings = {},
        onOpenMediaSettings = {},
        onOpenLabs = {},
        onOpenAbout = {},
        onOpenLockScreenSettings = {},
        onEditProfileClick = {},
        onSecureBackupClick = {},
        onManageAccountClick = {},
        onLinkNewDeviceClick = {},
        onOpenRageShake = {},
        onModerationAndSafetyClick = {},
        onOpenNotificationSettings = {},
        onSignOutClick = {},
        onDeactivateClick = {},
    )
}
