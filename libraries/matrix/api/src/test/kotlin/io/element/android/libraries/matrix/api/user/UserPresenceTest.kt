package io.element.android.libraries.matrix.api.user

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UserPresenceTest {
    @Test fun onlineIsOnlyExplicitOnline() {
        assertEquals("Online", UserPresence.Online.presenceLabel(120_000))
        assertEquals("Away", UserPresence.Away.presenceLabel(120_000))
        assertNull(UserPresence.Unknown.presenceLabel(120_000))
        assertEquals("Offline", UserPresence.Offline(null).presenceLabel(120_000))
    }

    @Test fun lastSeenFromValidServerTimestamp() {
        assertEquals("Last seen just now", UserPresence.Offline(90_000).presenceLabel(120_000))
        assertEquals("Last seen 2 min ago", UserPresence.Offline(120_000).presenceLabel(240_000))
    }

    @Test fun futureServerTimestampIsNotAccepted() {
        assertEquals("Offline", UserPresence.Offline(200_000).presenceLabel(100_000))
    }
}
