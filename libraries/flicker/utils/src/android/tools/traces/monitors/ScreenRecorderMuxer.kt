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

import android.media.MediaCodec
import android.os.SystemClock
import androidx.annotation.VisibleForTesting
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.Metadata
import androidx.media3.common.MimeTypes
import androidx.media3.muxer.BufferInfo
import androidx.media3.muxer.Muxer
import androidx.media3.muxer.MuxerException
import androidx.media3.transformer.InAppMp4Muxer
import com.google.common.collect.ImmutableList
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit

class ScreenRecorderMuxer @VisibleForTesting constructor(private val muxer: Muxer) : Muxer {
    @Volatile var isFrameRecorded: Boolean = false
    private val metadataTrackToken: Int =
        muxer.addTrack(Format.Builder().setSampleMimeType(MIME_TYPE_METADATA).build())
    private var videoTrackToken: Int = -1

    class Factory(private val listener: Listener) : Muxer.Factory {
        val factory = InAppMp4Muxer.Factory()

        @Override
        @kotlin.Throws(MuxerException::class)
        override fun create(path: String): Muxer {
            val muxer = ScreenRecorderMuxer(factory.create(path))
            listener.onMuxerCreated(muxer)
            return muxer
        }

        @Override
        override fun getSupportedSampleMimeTypes(trackType: Int): ImmutableList<String> {
            return if (trackType == C.TRACK_TYPE_METADATA) {
                ImmutableList.Builder<String>().add(MIME_TYPE_METADATA).build()
            } else factory.getSupportedSampleMimeTypes(trackType)
        }

        interface Listener {
            fun onMuxerCreated(newMuxer: ScreenRecorderMuxer)
        }
    }

    @Override
    @kotlin.Throws(MuxerException::class)
    override fun addTrack(format: Format): Int {
        val trackToken: Int = muxer.addTrack(format)
        if (MimeTypes.isVideo(format.sampleMimeType)) {
            videoTrackToken = trackToken
        }
        return trackToken
    }

    @Override
    @kotlin.Throws(MuxerException::class)
    override fun writeSampleData(trackToken: Int, byteBuffer: ByteBuffer, bufferInfo: BufferInfo) {
        muxer.writeSampleData(trackToken, byteBuffer, bufferInfo)
        if (trackToken == videoTrackToken) {
            writeMetadata(bufferInfo.presentationTimeUs)
            isFrameRecorded = true
        }
    }

    @Override
    override fun addMetadataEntry(metadataEntry: Metadata.Entry) {
        muxer.addMetadataEntry(metadataEntry)
    }

    @Override
    @kotlin.Throws(MuxerException::class)
    override fun close() {
        muxer.close()
    }

    /**
     * Saves metadata needed by Winscope to synchronize the screen recording playback with other
     * traces.
     *
     * The metadata (version 3) is written sample-by-sample. Each sample contains:
     * - Realtime-to-elapsed time offset in nanoseconds (8B little endian)
     * - Elapsed time accounting for elapsed-to-monotonic offset in nanoseconds (8B little endian)
     *
     * The first sample also contains the following data preceding the above:
     * - Winscope magic string (#VV1NSC0PET1ME2#, 16B)
     * - Metadata version number (4B little endian)
     */
    private fun writeMetadata(presentationTimeUs: Long) {
        // rte offset + elapsed presentation time
        var bufferSize = Long.SIZE_BYTES + Long.SIZE_BYTES

        if (!isFrameRecorded) {
            // magic string + metadata version
            bufferSize += WINSCOPE_MAGIC_STRING.toByteArray().size + Int.SIZE_BYTES
        }

        val buffer = ByteBuffer.allocate(bufferSize).order(ByteOrder.LITTLE_ENDIAN)

        if (!isFrameRecorded) {
            buffer.put(WINSCOPE_MAGIC_STRING.toByteArray()).putInt(WINSCOPE_METADATA_VERSION)
        }

        val monotonicTimeNs = TimeUnit.MILLISECONDS.toNanos(SystemClock.uptimeMillis())
        val elapsedTimeNs = SystemClock.elapsedRealtimeNanos()
        val realTimeNs = TimeUnit.MILLISECONDS.toNanos(System.currentTimeMillis())

        val realToElapsedTimeOffsetNs = realTimeNs - elapsedTimeNs
        val elapsedToMonotonicTimeOffsetNs = elapsedTimeNs - monotonicTimeNs

        buffer
            .putLong(realToElapsedTimeOffsetNs)
            .putLong(
                elapsedToMonotonicTimeOffsetNs + TimeUnit.MICROSECONDS.toNanos(presentationTimeUs)
            )

        val bufferInfo =
            BufferInfo(presentationTimeUs, bufferSize, MediaCodec.BUFFER_FLAG_KEY_FRAME)

        buffer.flip()
        writeSampleData(metadataTrackToken, buffer, bufferInfo)
    }

    companion object {
        private const val MIME_TYPE_METADATA = MimeTypes.APPLICATION_ID3
        private const val WINSCOPE_MAGIC_STRING = "#VV1NSC0PET1ME2#"
        private const val WINSCOPE_METADATA_VERSION = 3
    }
}
