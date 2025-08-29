/*
 * Copyright (C) 2023 The Android Open Source Project
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

import android.content.Context
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.tools.datatypes.Size
import android.tools.traces.deleteIfExists
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.annotation.VisibleForTesting
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.SurfaceAssetLoader
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import java.io.File

/** Runnable to record the screen contents and winscope metadata */
class ScreenRecordingRunnable
@JvmOverloads
@VisibleForTesting
constructor(
    private val outputFile: File,
    private val context: Context,
    displayManager: DisplayManager =
        context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager,
    windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager,
    private val assetLoaderFactory:
        (ScreenRecorderAssetLoader.Factory.Listener) -> ScreenRecorderAssetLoader.Factory =
        {
            makeAssetLoaderFactory(displayManager, windowManager, it)
        },
    private val encoderFactory: () -> DefaultEncoderFactory = { makeEncoderFactory(context) },
    private val muxerFactory:
        (ScreenRecorderMuxer.Factory.Listener) -> ScreenRecorderMuxer.Factory =
        {
            ScreenRecorderMuxer.Factory(it)
        },
) : Runnable, ScreenRecorderAssetLoader.Factory.Listener, ScreenRecorderMuxer.Factory.Listener {
    init {
        outputFile.deleteIfExists()
        require(!outputFile.exists())
        outputFile.createNewFile()
    }

    private var handler: Handler? = null
    private var muxer: ScreenRecorderMuxer? = null
    private var assetLoader: ScreenRecorderAssetLoader? = null

    val isFrameRecorded: Boolean
        get() = muxer?.isFrameRecorded ?: false

    var isCompleted: Boolean = false

    internal fun stop() {
        val assetLoader = assetLoader ?: error("AssetLoader was not created")
        assetLoader.stop()
    }

    override fun run() {
        Looper.prepare()
        val looper = Looper.myLooper() ?: error("Failed to prepare looper")
        handler = Handler(looper)

        val uri = SurfaceAssetLoader.MEDIA_ITEM_URI_SCHEME + ":flicker"
        val mediaItem: MediaItem = MediaItem.Builder().setUri(uri).build()

        handler?.post {
            val transformer =
                Transformer.Builder(context)
                    .setVideoMimeType(MIME_TYPE_VIDEO)
                    .setEncoderFactory(encoderFactory())
                    .setMuxerFactory(muxerFactory(this))
                    .setAssetLoaderFactory(assetLoaderFactory(this))
                    .setLooper(looper)
                    .addListener(
                        object : Transformer.Listener {
                            @Override
                            override fun onCompleted(
                                composition: Composition,
                                exportResult: ExportResult,
                            ) {
                                closeHandler()
                            }

                            @Override
                            override fun onError(
                                composition: Composition,
                                exportResult: ExportResult,
                                exportException: ExportException,
                            ) {
                                Log.e(LOG_TAG, "Transformer Error", exportException)
                                closeHandler()
                            }
                        }
                    )
                    .build()
            transformer.start(mediaItem, outputFile.getAbsolutePath())
        }
        Looper.loop()
    }

    private fun closeHandler() {
        val handler = handler ?: error("Handler shouldn't be null")
        handler.looper.quit()
        isCompleted = true
    }

    // ScreenRecorderAssetLoader.Factory.Listener
    override fun onAssetLoaderCreated(newAssetLoader: ScreenRecorderAssetLoader) {
        assetLoader = newAssetLoader
    }

    // ScreenRecorderMuxer.Factory.Listener
    override fun onMuxerCreated(newMuxer: ScreenRecorderMuxer) {
        muxer = newMuxer
    }

    companion object {
        private const val MIME_TYPE_VIDEO = MimeTypes.VIDEO_H264
        private const val BIT_RATE = 2000000 // 2Mbps
        private const val IFRAME_INTERVAL = 2f // 2 second between I-frames

        private fun makeEncoderFactory(context: Context): DefaultEncoderFactory {
            return DefaultEncoderFactory.Builder(context)
                .setRequestedVideoEncoderSettings(
                    VideoEncoderSettings.DEFAULT.buildUpon()
                        .setBitrate(BIT_RATE)
                        .setiFrameIntervalSeconds(IFRAME_INTERVAL)
                        .build()
                )
                .build()
        }

        private fun makeAssetLoaderFactory(
            displayManager: DisplayManager,
            windowManager: WindowManager,
            listener: ScreenRecorderAssetLoader.Factory.Listener,
        ): ScreenRecorderAssetLoader.Factory {
            val metrics: DisplayMetrics =
                DisplayMetrics().also { windowManager.defaultDisplay.getRealMetrics(it) }
            val recordingDisplaySize: Size = getRecordingDisplaySize(metrics)
            return ScreenRecorderAssetLoader.Factory(
                displayManager,
                Rect(0, 0, recordingDisplaySize.width, recordingDisplaySize.height),
                metrics.densityDpi,
                windowManager.defaultDisplay.mode.refreshRate,
                listener,
            )
        }

        private fun getRecordingDisplaySize(metrics: DisplayMetrics): Size {
            val widthPixels = if (metrics.widthPixels > 0) metrics.widthPixels else 720
            val heightPixels = if (metrics.heightPixels > 0) metrics.heightPixels else 1280
            val isPortrait = widthPixels < heightPixels
            val displayScaleValue =
                if (isPortrait && heightPixels > 1280) {
                    1280.0f / heightPixels
                } else if (!isPortrait && widthPixels > 1280) {
                    1280.0f / widthPixels
                } else {
                    1.0f
                }
            val width = (widthPixels * displayScaleValue).toInt()
            val height = (heightPixels * displayScaleValue).toInt()
            return Size.from(width, height)
        }
    }
}
