/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.preferences.impl.store

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import io.element.android.libraries.core.meta.BuildMeta
import io.element.android.libraries.core.meta.BuildType
import io.element.android.libraries.matrix.api.media.MediaPreviewValue
import io.element.android.libraries.matrix.api.tracing.LogLevel
import io.element.android.libraries.matrix.api.tracing.TraceLogPack
import io.element.android.libraries.preferences.api.store.AppPreferencesStore
import io.element.android.libraries.preferences.api.store.CustomRoomTag
import io.element.android.libraries.preferences.api.store.NotificationSound
import io.element.android.libraries.preferences.api.store.NotificationSound.Companion.toStored
import io.element.android.libraries.preferences.api.store.NotificationSoundChannelConfig
import io.element.android.libraries.preferences.api.store.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val developerModeKey = booleanPreferencesKey("developerMode")
private val customElementCallBaseUrlKey = stringPreferencesKey("elementCallBaseUrl")
private val themeKey = stringPreferencesKey("theme")
private val hideInviteAvatarsKey = booleanPreferencesKey("hideInviteAvatars")
private val otherAccountsExpandedKey = booleanPreferencesKey("otherAccountsExpanded")
private val timelineMediaPreviewValueKey = stringPreferencesKey("timelineMediaPreviewValue")
private val liveLocationMinimumDistanceUpdateKey = intPreferencesKey("liveLocationMinimumDistanceUpdate")
private val logLevelKey = stringPreferencesKey("logLevel")
private val traceLogPacksKey = stringPreferencesKey("traceLogPacks")
private val homeserverHistoryKey = stringPreferencesKey("homeserverHistory")
private val messageSoundUriKey = stringPreferencesKey("notificationMessageSoundUri")
private val messageSoundChannelVersionKey = intPreferencesKey("notificationMessageSoundChannelVersion")
private val messageSoundDisplayNameKey = stringPreferencesKey("notificationMessageSoundDisplayName")
private val callRingtoneUriKey = stringPreferencesKey("notificationCallRingtoneUri")
private val callRingtoneChannelVersionKey = intPreferencesKey("notificationCallRingtoneChannelVersion")
private val callRingtoneDisplayNameKey = stringPreferencesKey("notificationCallRingtoneDisplayName")
private val customRoomTagsKey = stringPreferencesKey("syncmeCustomRoomTags")
private val activeCustomRoomTagIdKey = stringPreferencesKey("syncmeActiveCustomRoomTagId")

// URLs never contain a newline, so it is a safe delimiter to persist an ordered list in a single String.
private const val HOMESERVER_HISTORY_DELIMITER = "\n"
private const val MAX_HOMESERVER_HISTORY_SIZE = 20
private const val CUSTOM_TAG_RECORD_SEPARATOR = "\n"
private const val CUSTOM_TAG_FIELD_SEPARATOR = "\t"
private const val CUSTOM_TAG_ROOM_SEPARATOR = "\u001f"
private const val MAX_CUSTOM_ROOM_TAGS = 30
private const val MAX_CUSTOM_ROOM_TAG_NAME_LENGTH = 32

@ContributesBinding(AppScope::class)
class DefaultAppPreferencesStore(
    private val buildMeta: BuildMeta,
    preferenceDataStoreFactory: PreferenceDataStoreFactory,
) : AppPreferencesStore {
    private val store = preferenceDataStoreFactory.create("elementx_preferences")

    override suspend fun setDeveloperModeEnabled(enabled: Boolean) {
        store.edit { prefs ->
            prefs[developerModeKey] = enabled
        }
    }

    override fun isDeveloperModeEnabledFlow(): Flow<Boolean> {
        return store.data.map { prefs ->
            // disabled by default on release and nightly, enabled by default on debug
            prefs[developerModeKey] ?: (buildMeta.buildType == BuildType.DEBUG)
        }
    }

    override suspend fun setCustomElementCallBaseUrl(string: String?) {
        store.edit { prefs ->
            if (string != null) {
                prefs[customElementCallBaseUrlKey] = string
            } else {
                prefs.remove(customElementCallBaseUrlKey)
            }
        }
    }

    override fun getCustomElementCallBaseUrlFlow(): Flow<String?> {
        return store.data.map { prefs ->
            prefs[customElementCallBaseUrlKey]
        }
    }

    override suspend fun setTheme(theme: String) {
        store.edit { prefs ->
            prefs[themeKey] = theme
        }
    }

    override fun getThemeFlow(): Flow<String?> {
        return store.data.map { prefs ->
            // SyncMe defaults to the true-black OLED theme until the user chooses another one.
            prefs[themeKey] ?: "Black"
        }
    }

    override suspend fun setOtherAccountsExpanded(expanded: Boolean) {
        store.edit { prefs ->
            prefs[otherAccountsExpandedKey] = expanded
        }
    }

    override fun isOtherAccountsExpandedFlow(): Flow<Boolean> {
        return store.data.map { prefs ->
            prefs[otherAccountsExpandedKey] ?: true
        }
    }

    override suspend fun setLiveLocationMinimumDistanceInMetersUpdate(value: Int) {
        store.edit { prefs ->
            prefs[liveLocationMinimumDistanceUpdateKey] = value
        }
    }

    override fun getLiveLocationMinimumDistanceInMetersUpdateFlow(): Flow<Int> {
        return store.data.map { prefs ->
            prefs[liveLocationMinimumDistanceUpdateKey] ?: 10
        }
    }

    @Deprecated("Use MediaPreviewService instead. Kept only for migration.")
    override fun getHideInviteAvatarsFlow(): Flow<Boolean?> {
        return store.data.map { prefs ->
            prefs[hideInviteAvatarsKey]
        }
    }

    @Deprecated("Use MediaPreviewService instead. Kept only for migration.")
    override suspend fun setHideInviteAvatars(hide: Boolean?) {
        store.edit { prefs ->
            if (hide != null) {
                prefs[hideInviteAvatarsKey] = hide
            } else {
                prefs.remove(hideInviteAvatarsKey)
            }
        }
    }

    @Deprecated("Use MediaPreviewService instead. Kept only for migration.")
    override suspend fun setTimelineMediaPreviewValue(mediaPreviewValue: MediaPreviewValue?) {
        store.edit { prefs ->
            if (mediaPreviewValue != null) {
                prefs[timelineMediaPreviewValueKey] = mediaPreviewValue.name
            } else {
                prefs.remove(timelineMediaPreviewValueKey)
            }
        }
    }

    @Deprecated("Use MediaPreviewService instead. Kept only for migration.")
    override fun getTimelineMediaPreviewValueFlow(): Flow<MediaPreviewValue?> {
        return store.data.map { prefs ->
            prefs[timelineMediaPreviewValueKey]?.let { MediaPreviewValue.valueOf(it) }
        }
    }

    override suspend fun setTracingLogLevel(logLevel: LogLevel) {
        store.edit { prefs ->
            prefs[logLevelKey] = logLevel.name
        }
    }

    override fun getTracingLogLevelFlow(): Flow<LogLevel> {
        return store.data.map { prefs ->
            prefs[logLevelKey]?.let { LogLevel.valueOf(it) } ?: buildMeta.defaultLogLevel()
        }
    }

    override suspend fun setTracingLogPacks(targets: Set<TraceLogPack>) {
        val value = targets.joinToString(",") { it.key }
        store.edit { prefs ->
            prefs[traceLogPacksKey] = value
        }
    }

    override fun getTracingLogPacksFlow(): Flow<Set<TraceLogPack>> {
        return store.data.map { prefs ->
            prefs[traceLogPacksKey]
                ?.split(",")
                ?.mapNotNull { value -> TraceLogPack.entries.find { it.key == value } }
                ?.toSet()
                ?: emptySet()
        }
    }

    override fun getHomeserverHistoryFlow(): Flow<List<String>> {
        return store.data.map { prefs -> prefs.readHomeserverHistory() }
    }

    override suspend fun addHomeserverToHistory(url: String) {
        val normalized = url.trim().lowercase()
        if (normalized.isEmpty()) return
        store.edit { prefs ->
            val updated = (listOf(normalized) + prefs.readHomeserverHistory().filter { it != normalized })
                .take(MAX_HOMESERVER_HISTORY_SIZE)
            prefs[homeserverHistoryKey] = updated.joinToString(HOMESERVER_HISTORY_DELIMITER)
        }
    }

    override fun getMessageSoundFlow(): Flow<NotificationSound> {
        return store.data.map { prefs -> NotificationSound.fromStored(prefs[messageSoundUriKey]) }
    }

    override suspend fun setMessageSoundAndIncrementVersion(sound: NotificationSound, title: String?): Int {
        var newVersion = 0
        store.edit { prefs ->
            val stored = sound.toStored()
            if (stored != null) {
                prefs[messageSoundUriKey] = stored
            } else {
                prefs.remove(messageSoundUriKey)
            }
            // Clear title on non-Custom so the picker doesn't show a stale label after a revert.
            if (sound is NotificationSound.Custom && !title.isNullOrBlank()) {
                prefs[messageSoundDisplayNameKey] = title
            } else {
                prefs.remove(messageSoundDisplayNameKey)
            }
            newVersion = (prefs[messageSoundChannelVersionKey] ?: 0) + 1
            prefs[messageSoundChannelVersionKey] = newVersion
        }
        return newVersion
    }

    override fun getMessageSoundDisplayNameFlow(): Flow<String?> {
        return store.data.map { prefs -> prefs[messageSoundDisplayNameKey] }
    }

    override fun getCallRingtoneFlow(): Flow<NotificationSound> {
        return store.data.map { prefs -> NotificationSound.fromStored(prefs[callRingtoneUriKey]) }
    }

    override suspend fun setCallRingtoneAndIncrementVersion(sound: NotificationSound, title: String?): Int {
        var newVersion = 0
        store.edit { prefs ->
            val stored = sound.toStored()
            if (stored != null) {
                prefs[callRingtoneUriKey] = stored
            } else {
                prefs.remove(callRingtoneUriKey)
            }
            if (sound is NotificationSound.Custom && !title.isNullOrBlank()) {
                prefs[callRingtoneDisplayNameKey] = title
            } else {
                prefs.remove(callRingtoneDisplayNameKey)
            }
            newVersion = (prefs[callRingtoneChannelVersionKey] ?: 0) + 1
            prefs[callRingtoneChannelVersionKey] = newVersion
        }
        return newVersion
    }

    override fun getCallRingtoneDisplayNameFlow(): Flow<String?> {
        return store.data.map { prefs -> prefs[callRingtoneDisplayNameKey] }
    }

    override suspend fun getNotificationSoundChannelConfig(): NotificationSoundChannelConfig {
        val prefs = store.data.first()
        return NotificationSoundChannelConfig(
            messageSound = NotificationSound.fromStored(prefs[messageSoundUriKey]),
            messageSoundVersion = prefs[messageSoundChannelVersionKey] ?: 0,
            messageSoundDisplayName = prefs[messageSoundDisplayNameKey],
            callRingtone = NotificationSound.fromStored(prefs[callRingtoneUriKey]),
            callRingtoneVersion = prefs[callRingtoneChannelVersionKey] ?: 0,
            callRingtoneDisplayName = prefs[callRingtoneDisplayNameKey],
        )
    }

    override fun getCustomRoomTagsFlow(): Flow<List<CustomRoomTag>> {
        return store.data.map { prefs -> prefs.readCustomRoomTags() }
    }

    override fun getActiveCustomRoomTagIdFlow(): Flow<String?> {
        return store.data.map { prefs ->
            val active = prefs[activeCustomRoomTagIdKey]
            active?.takeIf { id -> prefs.readCustomRoomTags().any { it.id == id } }
        }
    }

    override suspend fun createCustomRoomTag(name: String): String? {
        val normalizedName = name.trim().take(MAX_CUSTOM_ROOM_TAG_NAME_LENGTH)
        if (normalizedName.isBlank()) return null
        var createdId: String? = null
        store.edit { prefs ->
            val existing = prefs.readCustomRoomTags()
            if (existing.size >= MAX_CUSTOM_ROOM_TAGS) return@edit
            if (existing.any { it.name.equals(normalizedName, ignoreCase = true) }) return@edit
            val id = java.util.UUID.randomUUID().toString()
            prefs.writeCustomRoomTags(existing + CustomRoomTag(id, normalizedName, emptySet()))
            createdId = id
        }
        return createdId
    }

    override suspend fun deleteCustomRoomTag(tagId: String) {
        store.edit { prefs ->
            val updated = prefs.readCustomRoomTags().filterNot { it.id == tagId }
            prefs.writeCustomRoomTags(updated)
            if (prefs[activeCustomRoomTagIdKey] == tagId) {
                prefs.remove(activeCustomRoomTagIdKey)
            }
        }
    }

    override suspend fun setActiveCustomRoomTagId(tagId: String?) {
        store.edit { prefs ->
            if (tagId != null && prefs.readCustomRoomTags().any { it.id == tagId }) {
                prefs[activeCustomRoomTagIdKey] = tagId
            } else {
                prefs.remove(activeCustomRoomTagIdKey)
            }
        }
    }

    override suspend fun toggleRoomInCustomTag(tagId: String, roomId: String) {
        store.edit { prefs ->
            val updated = prefs.readCustomRoomTags().map { tag ->
                if (tag.id != tagId) {
                    tag
                } else {
                    val rooms = tag.roomIds.toMutableSet()
                    if (!rooms.add(roomId)) rooms.remove(roomId)
                    tag.copy(roomIds = rooms)
                }
            }
            prefs.writeCustomRoomTags(updated)
        }
    }

    override suspend fun reset() {
        store.edit { it.clear() }
    }
}

private fun Preferences.readCustomRoomTags(): List<CustomRoomTag> {
    val raw = this[customRoomTagsKey].orEmpty()
    if (raw.isEmpty()) return emptyList()
    return raw.split(CUSTOM_TAG_RECORD_SEPARATOR)
        .mapNotNull { record ->
            val parts = record.split(CUSTOM_TAG_FIELD_SEPARATOR, limit = 3)
            if (parts.size < 2) return@mapNotNull null
            val id = parts[0]
            val name = parts[1]
            if (id.isBlank() || name.isBlank()) return@mapNotNull null
            val roomIds = parts.getOrNull(2)
                .orEmpty()
                .split(CUSTOM_TAG_ROOM_SEPARATOR)
                .filter { it.isNotEmpty() }
                .toSet()
            CustomRoomTag(id = id, name = name, roomIds = roomIds)
        }
        .take(MAX_CUSTOM_ROOM_TAGS)
}

private fun androidx.datastore.preferences.core.MutablePreferences.writeCustomRoomTags(tags: List<CustomRoomTag>) {
    this[customRoomTagsKey] = tags.take(MAX_CUSTOM_ROOM_TAGS).joinToString(CUSTOM_TAG_RECORD_SEPARATOR) { tag ->
        val safeName = tag.name
            .replace(CUSTOM_TAG_FIELD_SEPARATOR, " ")
            .replace(CUSTOM_TAG_RECORD_SEPARATOR, " ")
            .take(MAX_CUSTOM_ROOM_TAG_NAME_LENGTH)
        val safeRooms = tag.roomIds
            .map { it.replace(CUSTOM_TAG_ROOM_SEPARATOR, "") }
            .joinToString(CUSTOM_TAG_ROOM_SEPARATOR)
        listOf(tag.id, safeName, safeRooms).joinToString(CUSTOM_TAG_FIELD_SEPARATOR)
    }
}

private fun Preferences.readHomeserverHistory(): List<String> {
    return this[homeserverHistoryKey]
        ?.split(HOMESERVER_HISTORY_DELIMITER)
        ?.filter { it.isNotEmpty() }
        ?: emptyList()
}

private fun BuildMeta.defaultLogLevel(): LogLevel {
    return when (buildType) {
        BuildType.DEBUG -> LogLevel.TRACE
        BuildType.NIGHTLY -> LogLevel.DEBUG
        BuildType.RELEASE -> LogLevel.INFO
    }
}
