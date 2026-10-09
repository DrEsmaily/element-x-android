/*
 * Copyright (c) 2026 SyncMe contributors.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.api.user

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UserPresenceTest {
    @Test fun unknownPresenceNeverCreatesAnOnlineCount() {
        assertNull(completeOnlineCount(2, mapOf("a" to UserPresence.Online, "b" to UserPresence.Unknown)))
        assertNull(completeOnlineCount(2, mapOf("a" to UserPresence.Online)))
    }

    @Test fun countsOnlyConfirmedOnlineMembers() {
        assertEquals(1, completeOnlineCount(2, mapOf("a" to UserPresence.Online, "b" to UserPresence.Offline())))
    }

    @Test fun invalidLastSeenTimesAreNotShown() {
        assertNull(UserPresence.Offline(-1).lastSeenTimestampOrNull(100))
        assertNull(UserPresence.Offline(200).lastSeenTimestampOrNull(100))
        assertNull(UserPresence.Online.lastSeenTimestampOrNull(100))
        assertEquals(80L, UserPresence.Offline(80).lastSeenTimestampOrNull(100))
    }
}
