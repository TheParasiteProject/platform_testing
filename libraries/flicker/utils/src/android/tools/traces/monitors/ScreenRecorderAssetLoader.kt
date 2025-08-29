/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.tools.traces.monitors

import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.os.Handler
import android.os.Looper
import android.view.Surface
import androidx.annotation.Nullable
import androidx.annotation.VisibleForTesting
import androidx.media3.common.C
import androidx.media3.common.ColorInfo
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.transformer.AssetLoader
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.SampleConsumer
import androidx.media3.transformer.SurfaceAssetLoader

class ScreenRecorderAssetLoader
@VisibleForTesting
constructor(private val listener: Listener, private val surfaceAssetLoader: SurfaceAssetLoader) :
    AssetLoader {

    @Override
    override fun start() {
        surfaceAssetLoader.start()
    }

    @Override
    override fun getProgress(progressHolder: ProgressHolder) =
        surfaceAssetLoader.getProgress(progressHolder)

    @Override override fun getDecoderNames() = surfaceAssetLoader.getDecoderNames()

    @Override
    override fun release() {
        surfaceAssetLoader.release()
    }

    fun stop() {
        listener.onStop()
        surfaceAssetLoader.signalEndOfInput()
    }

    class Factory(
        private val displayManager: DisplayManager,
        private val bounds: Rect,
        private val densityDpi: Int,
        private val frameRate: Float,
        private val factoryListener: Listener,
    ) : AssetLoader.Factory {
        @Override
        override fun createAssetLoader(
            editedMediaItem: EditedMediaItem,
            looper: Looper,
            listener: AssetLoader.Listener,
            compositionSettings: AssetLoader.CompositionSettings,
        ): ScreenRecorderAssetLoader {
            val screenCaptureFormat =
                Format.Builder()
                    .setSampleMimeType(MimeTypes.VIDEO_RAW)
                    .setWidth(bounds.width())
                    .setHeight(bounds.height())
                    .setColorInfo(ColorInfo.SRGB_BT709_FULL)
                    .setFrameRate(frameRate)
                    .build()

            val screenRecorderAssetLoaderListener =
                Listener(Handler(looper), displayManager, screenCaptureFormat, densityDpi)
            val surfaceAssetLoader =
                SurfaceAssetLoader.Factory(screenRecorderAssetLoaderListener)
                    .createAssetLoader(
                        editedMediaItem,
                        looper,
                        AssetLoaderListener(listener),
                        compositionSettings,
                    )
                    .apply { setContentFormat(screenCaptureFormat) }
            val screenRecorderAssetLoader =
                ScreenRecorderAssetLoader(screenRecorderAssetLoaderListener, surfaceAssetLoader)
            factoryListener.onAssetLoaderCreated(screenRecorderAssetLoader)
            return screenRecorderAssetLoader
        }

        interface Listener {
            fun onAssetLoaderCreated(newAssetLoader: ScreenRecorderAssetLoader)
        }
    }

    class Listener(
        private val handler: Handler,
        private val displayManager: DisplayManager,
        private val screenCaptureFormat: Format,
        private val densityDpi: Int,
    ) : SurfaceAssetLoader.Callback {
        private var virtualDisplay: VirtualDisplay? = null

        @Override
        override fun onSurfaceAssetLoaderCreated(surfaceAssetLoader: SurfaceAssetLoader) {
            // do nothing
        }

        @Override
        override fun onSurfaceReady(surface: Surface, editedMediaItem: EditedMediaItem) {
            handler.post {
                virtualDisplay =
                    displayManager.createVirtualDisplay(
                        VIRTUAL_DISPLAY_NAME,
                        screenCaptureFormat.width,
                        screenCaptureFormat.height,
                        densityDpi,
                        surface,
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                        null,
                        null,
                    )
            }
        }

        fun onStop() {
            virtualDisplay?.let {
                it.setSurface(null)
                it.release()
            }
        }
    }

    private class AssetLoaderListener(private val listener: AssetLoader.Listener) :
        AssetLoader.Listener {
        private var videoFormat: Format? = null

        @Override
        override fun onDurationUs(durationUs: Long) {
            // do nothing
        }

        @Override
        override fun onTrackCount(trackCount: Int) {
            // do nothing
        }

        @Override
        override fun onTrackAdded(inputFormat: Format, supportedOutputTypes: Int): Boolean {
            if (MimeTypes.isVideo(inputFormat.sampleMimeType)) {
                videoFormat = inputFormat
                listener.onDurationUs(C.TIME_UNSET)
                listener.onTrackCount(1)
                listener.onTrackAdded(inputFormat, AssetLoader.SUPPORTED_OUTPUT_TYPE_DECODED)
            }
            return true
        }

        @Nullable
        @Override
        @kotlin.Throws(ExportException::class)
        override fun onOutputFormat(format: Format): SampleConsumer? {
            return if (videoFormat == null) {
                null
            } else {
                listener.onOutputFormat(format)
            }
        }

        @Override
        override fun onError(exportException: ExportException) {
            listener.onError(exportException)
        }
    }

    companion object {
        private const val VIRTUAL_DISPLAY_NAME = "Recording Display"
    }
}
