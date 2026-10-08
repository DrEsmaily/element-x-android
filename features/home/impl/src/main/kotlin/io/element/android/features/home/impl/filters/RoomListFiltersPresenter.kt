/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.filters

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import dev.zacsweers.metro.Inject
import io.element.android.features.home.impl.filters.selection.FilterSelectionStrategy
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.preferences.api.store.AppPreferencesStore
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch

@Inject
class RoomListFiltersPresenter(
    private val filterSelectionStrategy: FilterSelectionStrategy,
    private val appPreferencesStore: AppPreferencesStore,
) : Presenter<RoomListFiltersState> {
    @Composable
    override fun present(): RoomListFiltersState {
        val coroutineScope = rememberCoroutineScope()
        val customTags by appPreferencesStore.getCustomRoomTagsFlow().collectAsState(emptyList())
        val activeCustomTagId by appPreferencesStore.getActiveCustomRoomTagIdFlow().collectAsState(null)

        fun handleEvent(event: RoomListFiltersEvent) {
            when (event) {
                RoomListFiltersEvent.ClearSelectedFilters -> {
                    filterSelectionStrategy.clear()
                    coroutineScope.launch { appPreferencesStore.setActiveCustomRoomTagId(null) }
                }
                is RoomListFiltersEvent.ToggleFilter -> {
                    filterSelectionStrategy.toggle(event.filter)
                }
                is RoomListFiltersEvent.SelectCustomTag -> {
                    coroutineScope.launch {
                        val next = event.tagId.takeUnless { it == activeCustomTagId }
                        appPreferencesStore.setActiveCustomRoomTagId(next)
                    }
                }
                is RoomListFiltersEvent.CreateCustomTag -> {
                    coroutineScope.launch {
                        // Creating a tag must not select or activate it.
                        appPreferencesStore.createCustomRoomTag(event.name)
                    }
                }
                is RoomListFiltersEvent.DeleteCustomTag -> {
                    coroutineScope.launch {
                        appPreferencesStore.deleteCustomRoomTag(event.tagId)
                    }
                }
            }
        }

        val filters by filterSelectionStrategy.filterSelectionStates.collectAsState()
        return RoomListFiltersState(
            filterSelectionStates = filters.toImmutableList(),
            customTags = customTags.toImmutableList(),
            activeCustomTagId = activeCustomTagId,
            eventSink = ::handleEvent,
        )
    }
}
