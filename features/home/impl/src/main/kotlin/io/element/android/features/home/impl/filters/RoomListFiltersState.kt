/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.filters

import io.element.android.features.home.impl.filters.selection.FilterSelectionState
import io.element.android.libraries.preferences.api.store.CustomRoomTag
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

data class RoomListFiltersState(
    val filterSelectionStates: ImmutableList<FilterSelectionState>,
    val customTags: ImmutableList<CustomRoomTag>,
    val activeCustomTagId: String?,
    val eventSink: (RoomListFiltersEvent) -> Unit,
) {
    val hasAnyFilterSelected = filterSelectionStates.any { it.isSelected } || activeCustomTagId != null

    fun selectedCustomTag(): CustomRoomTag? {
        return customTags.firstOrNull { it.id == activeCustomTagId }
    }

    fun selectedFilters(): ImmutableList<RoomListFilter> {
        return filterSelectionStates
            .filter { it.isSelected }
            .map { it.filter }
            .toImmutableList()
    }
}
