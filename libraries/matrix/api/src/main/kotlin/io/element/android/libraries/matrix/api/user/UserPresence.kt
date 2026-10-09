package io.element.android.libraries.matrix.api.user

/** Presence is authoritative only when reported by the Matrix homeserver. */
sealed interface UserPresence {
    data object Unknown : UserPresence
    data object Online : UserPresence
    data object Unavailable : UserPresence
    data class Offline(val lastActiveAtMillis: Long? = null) : UserPresence
}
