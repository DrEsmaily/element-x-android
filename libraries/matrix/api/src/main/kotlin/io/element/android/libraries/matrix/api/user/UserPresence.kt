package io.element.android.libraries.matrix.api.user

/** Only an explicit server "online" response can show a green indicator. */
sealed interface UserPresence {
    data object Unknown : UserPresence
    data object Online : UserPresence
    data object Away : UserPresence
    data class Offline(val lastSeenAt: Long?) : UserPresence
}

fun UserPresence.presenceLabel(nowMillis: Long): String? = when (this) {
    UserPresence.Unknown -> null
    UserPresence.Online -> "Online"
    UserPresence.Away -> "Away"
    is UserPresence.Offline -> {
        val seen = lastSeenAt
        when {
            seen == null || seen > nowMillis || seen <= 0L -> "Offline"
            nowMillis - seen < 60_000L -> "Last seen just now"
            nowMillis - seen < 3_600_000L -> "Last seen ${(nowMillis - seen) / 60_000L} min ago"
            nowMillis - seen < 86_400_000L -> "Last seen ${(nowMillis - seen) / 3_600_000L} hours ago"
            else -> "Last seen ${(nowMillis - seen) / 86_400_000L} days ago"
        }
    }
}
