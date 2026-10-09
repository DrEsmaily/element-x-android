/*
 * Copyright (c) 2026 SyncMe contributors.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.api.user

/**
 * Presence is different from connection health: a background sync connection must not
 * by itself mark a person online.
 *
 * Unknown is deliberately not treated as Offline, and does not expose a fabricated
 * last-seen timestamp. These values represent *server-reported* presence only.
 */
sealed interface UserPresence {
    data object Unknown : UserPresence
    data object Online : UserPresence
    data object Unavailable : UserPresence
    data class Offline(val lastActiveAtMillis: Long? = null) : UserPresence
}

/** Client-side preference only. It is not a server-enforced last-seen privacy rule. */
enum class OwnPresenceMode {
    SHOW_ACTIVITY,
    APPEAR_OFFLINE,
}

/**
 * Only display an online count if every joined member has an authoritative presence.
 * Room-member summaries / lazy-loaded member lists are not sufficient to calculate it.
 */
fun completeOnlineCount(memberCount: Int, reportedStates: Map<String, UserPresence>): Int? {
    if (memberCount < 0 || reportedStates.size != memberCount) return null
    if (reportedStates.values.any { it is UserPresence.Unknown }) return null
    return reportedStates.values.count { it is UserPresence.Online }
}

/** Any missing or invalid timestamp must be rendered as unavailable, never guessed. */
fun UserPresence.lastSeenTimestampOrNull(nowMillis: Long): Long? =
    (this as? UserPresence.Offline)?.lastActiveAtMillis
        ?.takeIf { it in 1..nowMillis }

/**
 * Neutral, English-only display text. Callers should display nothing if the
 * server has not supplied an authoritative presence state.
 */
fun UserPresence.displayText(nowMillis: Long): String? = when (this) {
    UserPresence.Unknown -> null
    UserPresence.Online -> "Online"
    UserPresence.Unavailable -> "Away"
    is UserPresence.Offline -> {
        val timestamp = lastSeenTimestampOrNull(nowMillis)
        if (timestamp == null) null else {
            val minutes = (nowMillis - timestamp) / 60_000
            when {
                minutes < 1 -> "Last seen just now"
                minutes == 1L -> "Last seen 1 minute ago"
                minutes < 60 -> "Last seen $minutes minutes ago"
                minutes < 120 -> "Last seen 1 hour ago"
                minutes < 24 * 60 -> "Last seen ${minutes / 60} hours ago"
                else -> null // Older timestamps require locale/timezone-aware date formatting in the UI.
            }
        }
    }
}
