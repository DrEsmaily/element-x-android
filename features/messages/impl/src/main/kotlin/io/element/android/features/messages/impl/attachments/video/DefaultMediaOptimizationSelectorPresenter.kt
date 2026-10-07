/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.attachments.video

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesBinding
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.core.mimetype.MimeTypes.isMimeTypeVideo
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.mediaupload.api.MaxUploadSizeProvider
import io.element.android.libraries.mediaupload.api.MediaOptimizationConfigProvider
import io.element.android.libraries.mediaviewer.api.local.LocalMedia
import io.element.android.libraries.preferences.api.store.VideoCompressionPreset
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import timber.log.Timber

@AssistedInject
class DefaultMediaOptimizationSelectorPresenter(
    @Assisted private val index: Int,
    @Assisted private val localMedia: LocalMedia,
    @Assisted private val sendAsFile: Boolean,
    private val maxUploadSizeProvider: MaxUploadSizeProvider,
    private val mediaOptimizationConfigProvider: MediaOptimizationConfigProvider,
    @Suppress("UNUSED_PARAMETER") videoCompressionPresetSelector: VideoCompressionPresetSelector,
    @Suppress("UNUSED_PARAMETER") mediaExtractorFactory: VideoMetadataExtractor.Factory,
) : MediaOptimizationSelectorPresenter {
    @ContributesBinding(SessionScope::class)
    @AssistedFactory
    interface Factory : MediaOptimizationSelectorPresenter.Factory {
        override fun create(
            index: Int,
            localMedia: LocalMedia,
            sendAsFile: Boolean,
        ): DefaultMediaOptimizationSelectorPresenter
    }

    @Composable
    override fun present(): MediaOptimizationSelectorState {
        val displayMediaSelectorViews by produceState<Boolean?>(null) {
            // SyncMe exposes no video quality choice: video is always sent as Original.
            value = !sendAsFile && !localMedia.info.mimeType.isMimeTypeVideo()
        }

        var displayVideoPresetSelectorDialog by remember { mutableStateOf(false) }

        val maxUploadSize by produceState(AsyncData.Loading()) {
            maxUploadSizeProvider.getMaxUploadSize().fold(
                onSuccess = { value = AsyncData.Success(it) },
                onFailure = {
                    Timber.e(it, "Failed to retrieve max upload size for video optimization selector")
                    value = AsyncData.Success((100 * 1024 * 1024).toLong()) // Default to 100 MB if we can't retrieve the max upload size
                }
            )
        }

        val mediaMimeType = localMedia.info.mimeType

        val videoSizeEstimations by produceState<AsyncData<ImmutableList<VideoUploadEstimation>>>(
            initialValue = AsyncData.Loading(),
            key1 = maxUploadSize,
        ) {
            if (maxUploadSize !is AsyncData.Success) {
                return@produceState
            }

            if (!mediaMimeType.isMimeTypeVideo()) {
                value = AsyncData.Uninitialized
                return@produceState
            }

            val originalSize = localMedia.info.fileSize ?: 0L
            val sizeEstimations = listOf(
                VideoUploadEstimation(
                    preset = VideoCompressionPreset.HIGH,
                    sizeInBytes = originalSize,
                    canUpload = originalSize <= (maxUploadSize as AsyncData.Success).data,
                )
            ).toImmutableList()

            value = AsyncData.Success(sizeEstimations)
        }

        var selectedImageOptimization by remember { mutableStateOf<AsyncData<Boolean>>(AsyncData.Loading()) }
        var selectedVideoOptimizationPreset by remember { mutableStateOf<AsyncData<VideoCompressionPreset>>(AsyncData.Loading()) }

        LaunchedEffect(videoSizeEstimations.dataOrNull()) {
            val mediaOptimizationConfig = mediaOptimizationConfigProvider.get()
            selectedImageOptimization = AsyncData.Success(
                if (sendAsFile) false else mediaOptimizationConfig.compressImages
            )
            selectedVideoOptimizationPreset = AsyncData.Success(VideoCompressionPreset.HIGH)
        }

        fun handleEvent(event: MediaOptimizationSelectorEvent) {
            when (event) {
                is MediaOptimizationSelectorEvent.SelectImageOptimization -> {
                    selectedImageOptimization = AsyncData.Success(event.enabled)
                }
                is MediaOptimizationSelectorEvent.SelectVideoPreset -> {
                    selectedVideoOptimizationPreset = AsyncData.Success(VideoCompressionPreset.HIGH)
                    displayVideoPresetSelectorDialog = false
                }
                is MediaOptimizationSelectorEvent.OpenVideoPresetSelectorDialog -> {
                    displayVideoPresetSelectorDialog = true
                }
                is MediaOptimizationSelectorEvent.DismissVideoPresetSelectorDialog -> {
                    displayVideoPresetSelectorDialog = false
                }
            }
        }

        return MediaOptimizationSelectorState(
            index = index,
            maxUploadSize = maxUploadSize,
            videoSizeEstimations = videoSizeEstimations,
            isImageOptimizationEnabled = selectedImageOptimization.dataOrNull(),
            selectedVideoPreset = selectedVideoOptimizationPreset.dataOrNull(),
            displayMediaSelectorViews = displayMediaSelectorViews,
            displayVideoPresetSelectorDialog = displayVideoPresetSelectorDialog,
            eventSink = ::handleEvent,
        )
    }
}
