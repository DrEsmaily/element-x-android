/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.filters

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.IconButton
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.Surface
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.home.impl.R
import io.element.android.libraries.preferences.api.store.CustomRoomTag
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.testtags.TestTags
import io.element.android.libraries.testtags.testTag

/**
 * Ref: https://www.figma.com/design/G1xy0HDZKJf5TCRFmKb5d5/Compound-Android-Components?node-id=2191-606
 */
@Composable
fun RoomListFiltersView(
    state: RoomListFiltersState,
    modifier: Modifier = Modifier
) {
    fun onClearFiltersClick() {
        state.eventSink(RoomListFiltersEvent.ClearSelectedFilters)
    }

    fun onToggleFilter(filter: RoomListFilter) {
        state.eventSink(RoomListFiltersEvent.ToggleFilter(filter))
    }

    var scrollToStart by remember { mutableIntStateOf(0) }
    val lazyListState = rememberLazyListState()
    var showTagManager by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(scrollToStart) {
        if (scrollToStart == 0) return@LaunchedEffect
        // Scroll until the first item start to be displayed
        // Since all items have different size, there is no way to compute the amount of
        // pixel to scroll to go directly to the start of the row.
        // But IRL it should only happen for one item.
        while (lazyListState.firstVisibleItemIndex > 0) {
            lazyListState.animateScrollBy(
                value = -(lazyListState.firstVisibleItemScrollOffset + 1f),
                animationSpec = spring(
                    stiffness = Spring.StiffnessMediumLow,
                )
            )
        }
        // Then scroll to the start of the list, a bit faster, to fully reveal the first
        // item, which can be the close button to reset filter, or the first item
        // if the user has scroll a bit before clicking on the close button.
        lazyListState.animateScrollBy(
            value = -lazyListState.firstVisibleItemScrollOffset.toFloat(),
            animationSpec = spring(
                stiffness = Spring.StiffnessMedium,
            )
        )
    }
    val previousFilters = remember { mutableStateOf(listOf<RoomListFilter>()) }
    // Put the selected custom tag first among custom chips, just like built-in
    // filters place the selected chip at the leading edge of their group.
    val orderedCustomTags = state.customTags.sortedBy { if (it.id == state.activeCustomTagId) 0 else 1 }
    LazyRow(
        contentPadding = PaddingValues(start = 8.dp, end = 16.dp),
        modifier = modifier.fillMaxWidth(),
        state = lazyListState,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item("clear_filters") {
            if (state.hasAnyFilterSelected) {
                RoomListClearFiltersButton(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .testTag(TestTags.homeScreenClearFilters),
                    onClick = {
                        previousFilters.value = state.selectedFilters()
                        onClearFiltersClick()
                        // When clearing filter, we want to ensure that the list
                        // of filters is scrolled to the start.
                        scrollToStart++
                    }
                )
            }
        }
        // Selected tag is a genuinely leading chip, not merely a scrolled trailing chip.
        orderedCustomTags.firstOrNull { it.id == state.activeCustomTagId }?.let { tag ->
            item("custom_tag_${tag.id}") {
                RoomListCustomTagView(
                    tag = tag,
                    selected = true,
                    modifier = Modifier.animateItem(),
                    onClick = { state.eventSink(RoomListFiltersEvent.SelectCustomTag(tag.id)) },
                )
            }
        }
        state.filterSelectionStates.forEachIndexed { i, filterWithSelection ->
            item(filterWithSelection.filter) {
                val zIndex = (if (previousFilters.value.contains(filterWithSelection.filter)) state.filterSelectionStates.size else 0) - i.toFloat()
                RoomListFilterView(
                    modifier = Modifier
                        .animateItem()
                        .zIndex(zIndex),
                    roomListFilter = filterWithSelection.filter,
                    selected = filterWithSelection.isSelected,
                    onClick = {
                        previousFilters.value = state.selectedFilters()
                        onToggleFilter(it)
                        if (filterWithSelection.isSelected.not()) {
                            scrollToStart++
                        }
                    },
                )
            }
        }
        orderedCustomTags.filterNot { it.id == state.activeCustomTagId }.forEach { tag ->
            item("custom_tag_${tag.id}") {
                RoomListCustomTagView(
                    tag = tag,
                    selected = state.activeCustomTagId == tag.id,
                    modifier = Modifier.animateItem(),
                    onClick = {
                        state.eventSink(RoomListFiltersEvent.SelectCustomTag(tag.id))
                        // Scroll this actual chip into the leading position, instead of
                        // forcing a repeated animated scroll to absolute index zero.
                    },
                )
            }
        }
        item("manage_custom_tags") {
            FilterChip(
                selected = false,
                onClick = { showTagManager = true },
                modifier = Modifier.height(32.dp),
                shape = CircleShape,
                label = {
                    Text(
                        text = "+",
                        style = ElementTheme.typography.fontBodyMdRegular,
                    )
                },
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = false,
                    borderColor = ElementTheme.colors.borderInteractiveSecondary,
                ),
            )
        }
    }

    if (showTagManager) {
        SyncMeTagManager(
            tags = state.customTags,
            onDismiss = { showTagManager = false },
            onCreate = { state.eventSink(RoomListFiltersEvent.CreateCustomTag(it)) },
            onRename = { id, name -> state.eventSink(RoomListFiltersEvent.RenameCustomTag(id, name)) },
            onClearChats = { state.eventSink(RoomListFiltersEvent.ClearCustomTagChats(it)) },
            onDelete = { state.eventSink(RoomListFiltersEvent.DeleteCustomTag(it)) },
            onOpen = { id ->
                state.eventSink(RoomListFiltersEvent.SelectCustomTag(id))
                showTagManager = false
            },
        )
    }
}

@Composable
private fun SyncMeTagManager(
    tags: List<CustomRoomTag>,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onClearChats: (String) -> Unit,
    onDelete: (String) -> Unit,
    onOpen: (String) -> Unit,
) {
    var page by rememberSaveable { mutableStateOf("manage") }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var draft by rememberSaveable { mutableStateOf("") }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val selected = tags.firstOrNull { it.id == selectedId }
    val remaining = 3 - tags.size
    val trimmed = draft.trim()
    val valid = trimmed.isNotBlank() && trimmed.length <= 20 &&
        tags.none { it.id != (if (page == "rename") selectedId else null) && it.name.equals(trimmed, ignoreCase = true) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(color = ElementTheme.colors.bgCanvasDefault, modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 26.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = {
                        if (page == "manage") onDismiss() else {
                            page = if (page == "create" || page == "details") "manage" else "details"
                            confirmDelete = false
                        }
                    }) { androidx.compose.material3.Text("‹ Back") }
                    androidx.compose.material3.Text(
                        text = when (page) {
                            "create" -> "Create tag"
                            "details" -> "Tag details"
                            "rename" -> "Rename tag"
                            else -> "Manage tags"
                        },
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                when (page) {
                    "manage" -> {
                        androidx.compose.material3.Text("Organize your chats with up to 3 custom tags.",
                            color = ElementTheme.colors.textSecondary)
                        androidx.compose.material3.Text("Favorite", style = MaterialTheme.typography.titleMedium)
                        TagManagerRow("★", "Favorite", "Built-in", { onDismiss() })
                        HorizontalDivider()
                        androidx.compose.material3.Text("Custom tags", style = MaterialTheme.typography.titleMedium)
                        tags.forEach { tag ->
                            TagManagerRow("◈", tag.name, "${tag.roomIds.size} chats", {
                                selectedId = tag.id
                                page = "details"
                            })
                        }
                        if (tags.isEmpty()) androidx.compose.material3.Text("No custom tags yet.")
                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = { draft = ""; page = "create" },
                            enabled = remaining > 0,
                            modifier = Modifier.fillMaxWidth(),
                        ) { androidx.compose.material3.Text("+ Create new tag") }
                        androidx.compose.material3.Text(
                            if (remaining == 0) "You've reached the 3-tag limit. Delete a tag to create another."
                            else "${tags.size} of 3 tags used",
                            color = ElementTheme.colors.textSecondary,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                    }
                    "create", "rename" -> {
                        androidx.compose.material3.Text("Tag name")
                        OutlinedTextField(
                            value = draft,
                            onValueChange = { draft = it.take(20) },
                            label = { androidx.compose.material3.Text("Add tag name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        androidx.compose.material3.Text("${draft.length}/20", color = ElementTheme.colors.textSecondary)
                        androidx.compose.material3.Text("Choose an icon (optional)")
                        androidx.compose.material3.Text("◈     ♡     ★     ▣     ✈",
                            style = MaterialTheme.typography.headlineMedium, color = ElementTheme.colors.textSecondary)
                        androidx.compose.material3.Text("Preview")
                        TagManagerRow("◈", draft.ifBlank { "Tag name" }, "0 chats", {})
                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = {
                                if (page == "rename") {
                                    selectedId?.let { onRename(it, trimmed) }
                                    page = "details"
                                } else {
                                    onCreate(trimmed)
                                    page = "manage"
                                }
                            },
                            enabled = valid && (page == "rename" || remaining > 0),
                            modifier = Modifier.fillMaxWidth(),
                        ) { androidx.compose.material3.Text(if (page == "rename") "Save" else "Create tag") }
                        androidx.compose.material3.Text("You can create up to 3 custom tags. New tags are not selected automatically.",
                            color = ElementTheme.colors.textSecondary)
                    }
                    "details" -> {
                        if (selected == null) {
                            androidx.compose.material3.Text("Tag no longer exists.")
                            TextButton(onClick = { page = "manage" }) { androidx.compose.material3.Text("Back") }
                        } else {
                            Spacer(Modifier.height(10.dp))
                            androidx.compose.material3.Text("◈", modifier = Modifier.align(Alignment.CenterHorizontally),
                                style = MaterialTheme.typography.headlineLarge)
                            androidx.compose.material3.Text(selected.name, modifier = Modifier.align(Alignment.CenterHorizontally),
                                style = MaterialTheme.typography.titleLarge)
                            androidx.compose.material3.Text("${selected.roomIds.size} chats",
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                                color = ElementTheme.colors.textSecondary)
                            TagManagerRow("✎", "Rename", "", {
                                draft = selected.name
                                page = "rename"
                            })
                            TagManagerRow("✓", "View tagged chats", "", { onOpen(selected.id) })
                            TagManagerRow("−", "Remove from chats", "Keep all conversations", {
                                onClearChats(selected.id)
                            })
                            TagManagerRow("✕", "Delete tag", "", { confirmDelete = true })
                            if (confirmDelete) {
                                Spacer(Modifier.weight(1f))
                                androidx.compose.material3.Text("Delete this tag?", style = MaterialTheme.typography.titleLarge)
                                androidx.compose.material3.Text("Chats will remain, only the tag will be removed.")
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedButton(onClick = { confirmDelete = false }, modifier = Modifier.weight(1f)) {
                                        androidx.compose.material3.Text("Cancel")
                                    }
                                    Button(onClick = {
                                        onDelete(selected.id)
                                        selectedId = null
                                        confirmDelete = false
                                        page = "manage"
                                    }, modifier = Modifier.weight(1f)) { androidx.compose.material3.Text("Delete tag") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TagManagerRow(symbol: String, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        color = ElementTheme.colors.bgSubtleSecondary,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            androidx.compose.material3.Text(symbol, style = MaterialTheme.typography.titleLarge)
            Column(modifier = Modifier.weight(1f)) {
                androidx.compose.material3.Text(title)
                if (subtitle.isNotEmpty()) androidx.compose.material3.Text(subtitle, color = ElementTheme.colors.textSecondary)
            }
            androidx.compose.material3.Text("›")
        }
    }
}

@Composable
private fun RoomListClearFiltersButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(ElementTheme.colors.bgActionPrimaryRest)
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Icon(
            modifier = Modifier
                .align(Alignment.Center)
                .size(16.dp),
            imageVector = CompoundIcons.Close(),
            tint = ElementTheme.colors.iconOnSolidPrimary,
            contentDescription = stringResource(id = R.string.screen_roomlist_clear_filters),
        )
    }
}

@Composable
private fun RoomListFilterView(
    roomListFilter: RoomListFilter,
    selected: Boolean,
    onClick: (RoomListFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val background = animateColorAsState(
        targetValue = if (selected) ElementTheme.colors.bgActionPrimaryRest else ElementTheme.colors.bgCanvasDefault,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "chip background colour",
    )
    val textColour = animateColorAsState(
        targetValue = if (selected) ElementTheme.colors.textOnSolidPrimary else ElementTheme.colors.textPrimary,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "chip text colour",
    )
    val borderColour = animateColorAsState(
        targetValue = if (selected) Color.Transparent else ElementTheme.colors.borderInteractiveSecondary,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "chip border colour",
    )

    FilterChip(
        selected = selected,
        onClick = { onClick(roomListFilter) },
        modifier = modifier.height(32.dp),
        shape = CircleShape,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = background.value,
            selectedContainerColor = background.value,
            labelColor = textColour.value,
            selectedLabelColor = textColour.value,
        ),
        label = {
            Text(
                text = stringResource(id = roomListFilter.stringResource),
                style = ElementTheme.typography.fontBodyMdRegular,
            )
        },
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = borderColour.value,
        ),
    )
}

@Composable
private fun RoomListCustomTagView(
    tag: CustomRoomTag,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = animateColorAsState(
        targetValue = if (selected) ElementTheme.colors.bgActionPrimaryRest else ElementTheme.colors.bgCanvasDefault,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "custom tag chip background",
    )
    val textColour = animateColorAsState(
        targetValue = if (selected) ElementTheme.colors.textOnSolidPrimary else ElementTheme.colors.textPrimary,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "custom tag chip text",
    )
    val borderColour = animateColorAsState(
        targetValue = if (selected) Color.Transparent else ElementTheme.colors.borderInteractiveSecondary,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "custom tag chip border",
    )

    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier.height(32.dp),
        shape = CircleShape,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = background.value,
            selectedContainerColor = background.value,
            labelColor = textColour.value,
            selectedLabelColor = textColour.value,
        ),
        label = {
            Text(
                text = tag.name,
                style = ElementTheme.typography.fontBodyMdRegular,
            )
        },
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = borderColour.value,
        ),
    )
}

@PreviewsDayNight
@Composable
internal fun RoomListFiltersViewPreview(@PreviewParameter(RoomListFiltersStatePreviewParam::class) state: RoomListFiltersState) = ElementPreview {
    RoomListFiltersView(
        modifier = Modifier.padding(vertical = 4.dp),
        state = state,
    )
}
