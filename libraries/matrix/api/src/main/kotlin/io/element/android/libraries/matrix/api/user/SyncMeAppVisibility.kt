package io.element.android.libraries.matrix.api.user

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Process-local foreground hint for Matrix presence.
 * null means no Activity lifecycle transition has been observed yet.
 */
object SyncMeAppVisibility {
    private val mutableIsForeground = MutableStateFlow<Boolean?>(null)
    val isForeground: StateFlow<Boolean?> = mutableIsForeground

    fun setForeground(value: Boolean) {
        mutableIsForeground.value = value
    }
}
