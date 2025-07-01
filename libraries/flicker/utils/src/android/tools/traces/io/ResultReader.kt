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

package android.tools.traces.io

import android.tools.Tag
import android.tools.Timestamp
import android.tools.io.Artifact
import android.tools.io.FLICKER_IO_TAG
import android.tools.io.Reader
import android.tools.io.ResultArtifactDescriptor
import android.tools.io.TraceType
import android.tools.parsers.events.EventLogParser
import android.tools.traces.events.CujTrace
import android.tools.traces.events.EventLog
import android.tools.traces.parsers.perfetto.LayersTraceParser
import android.tools.traces.parsers.perfetto.ProtoLogTraceParser
import android.tools.traces.parsers.perfetto.TraceProcessorSession
import android.tools.traces.parsers.perfetto.TransactionsTraceParser
import android.tools.traces.parsers.perfetto.TransitionsTraceParser
import android.tools.traces.parsers.perfetto.WindowManagerTraceParser
import android.tools.traces.parsers.wm.LegacyWindowManagerTraceParser
import android.tools.traces.parsers.wm.WindowManagerDumpParser
import android.tools.traces.protolog.ProtoLogTrace
import android.tools.traces.surfaceflinger.LayersTrace
import android.tools.traces.surfaceflinger.TransactionsTrace
import android.tools.traces.wm.TransitionsTrace
import android.tools.traces.wm.WindowManagerTrace
import android.tools.withTracing
import android.util.Log
import androidx.annotation.VisibleForTesting
import java.io.IOException

/**
 * Helper class to read results from a flicker artifact
 *
 * @param result to read from
 */
open class ResultReader(result: IResultData) : Reader {
    @VisibleForTesting
    var result = result
        internal set

    override val artifacts: Array<Artifact>
        get() = result.artifacts

    override val runStatus
        get() = result.runStatus

    internal val transitionTimeRange
        get() = result.transitionTimeRange

    override val isFailure
        get() = runStatus.isFailure

    override val executionError
        get() = result.executionError

    override fun readBytes(traceType: TraceType, tag: String): ByteArray? =
        readBytes(ResultArtifactDescriptor(traceType, tag))

    private fun readBytes(descriptor: ResultArtifactDescriptor): ByteArray? {
        for (artifact in result.artifacts) {
            if (artifact.hasTrace(descriptor)) {
                return artifact.readBytes(descriptor)
            }
        }

        return null
    }

    /**
     * {@inheritDoc}
     *
     * @throws IOException if the artifact file doesn't exist or can't be read
     */
    @Throws(IOException::class)
    override fun readWmState(tag: String): WindowManagerTrace? {
        return withTracing("readWmState#$tag") {
            val descriptor = ResultArtifactDescriptor(TraceType.WM_DUMP, tag)
            Log.d(FLICKER_IO_TAG, "Reading WM trace descriptor=$descriptor from $result")
            val traceData = readBytes(descriptor)
            traceData?.let {
                if (android.tracing.Flags.perfettoWmDump()) {
                    TraceProcessorSession.loadPerfettoTrace(it) { session ->
                        WindowManagerTraceParser().parse(session)
                    }
                } else {
                    WindowManagerDumpParser().parse(it, clearCache = true)
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws IOException if the artifact file doesn't exist or can't be read
     */
    @Throws(IOException::class)
    override fun readWmTrace(): WindowManagerTrace? {
        return withTracing("readWmTrace") {
            if (android.tracing.Flags.perfettoWmTracing()) {
                readPerfettoWindowManagerTrace()
            } else {
                readLegacyWindowManagerTrace()
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws IOException if the artifact file doesn't exist or can't be read
     */
    @Throws(IOException::class)
    override fun readLayersTrace(): LayersTrace? {
        return withTracing("readLayersTrace") {
            val descriptor = ResultArtifactDescriptor(TraceType.PERFETTO)
            readBytes(descriptor)?.let {
                TraceProcessorSession.loadPerfettoTrace(it) { session ->
                    LayersTraceParser()
                        .parse(
                            session,
                            transitionTimeRange.start,
                            transitionTimeRange.end,
                            addInitialEntry = true,
                            clearCache = true,
                        )
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws IOException if the artifact file doesn't exist or can't be read
     */
    @Throws(IOException::class)
    override fun readLayersDump(tag: String): LayersTrace? {
        return withTracing("readLayersDump#$tag") {
            val descriptor = ResultArtifactDescriptor(TraceType.PERFETTO, tag)
            readBytes(descriptor)?.let {
                TraceProcessorSession.loadPerfettoTrace(it) { session ->
                    LayersTraceParser().parse(session, clearCache = true)
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws IOException if the artifact file doesn't exist or can't be read
     */
    @Throws(IOException::class)
    override fun readTransactionsTrace(): TransactionsTrace? =
        withTracing("readTransactionsTrace") {
            doReadTransactionsTrace(from = transitionTimeRange.start, to = transitionTimeRange.end)
        }

    private fun doReadTransactionsTrace(from: Timestamp, to: Timestamp): TransactionsTrace? {
        val traceData = readBytes(ResultArtifactDescriptor(TraceType.PERFETTO))
        return traceData?.let {
            val trace =
                TraceProcessorSession.loadPerfettoTrace(traceData) { session ->
                    TransactionsTraceParser().parse(session, from, to, addInitialEntry = true)
                }
            require(trace.entries.isNotEmpty()) { "Transactions trace cannot be empty" }
            trace
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws IOException if the artifact file doesn't exist or can't be read
     */
    @Throws(IOException::class)
    override fun readTransitionsTrace(): TransitionsTrace? {
        return withTracing("readTransitionsTrace") {
            readBytes(ResultArtifactDescriptor(TraceType.PERFETTO))?.let {
                TraceProcessorSession.loadPerfettoTrace(it) { session ->
                    TransitionsTraceParser()
                        .parse(
                            session,
                            from = transitionTimeRange.start,
                            to = transitionTimeRange.end,
                        )
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws IOException if the artifact file doesn't exist or can't be read
     */
    @Throws(IOException::class)
    override fun readProtoLogTrace(): ProtoLogTrace? {
        return withTracing("readProtoLogTrace") {
            val traceData = readBytes(ResultArtifactDescriptor(TraceType.PERFETTO))

            traceData?.let {
                TraceProcessorSession.loadPerfettoTrace(traceData) { session ->
                    ProtoLogTraceParser()
                        .parse(
                            session,
                            from = transitionTimeRange.start,
                            to = transitionTimeRange.end,
                        )
                }
            }
        }
    }

    private fun readPerfettoWindowManagerTrace(): WindowManagerTrace? {
        val traceData = readBytes(ResultArtifactDescriptor(TraceType.PERFETTO))

        return traceData?.let {
            TraceProcessorSession.loadPerfettoTrace(traceData) { session ->
                WindowManagerTraceParser()
                    .parse(session, from = transitionTimeRange.start, to = transitionTimeRange.end)
            }
        }
    }

    private fun readLegacyWindowManagerTrace(): WindowManagerTrace? {
        val traceData = readBytes(ResultArtifactDescriptor(TraceType.WM))

        return traceData?.let {
            LegacyWindowManagerTraceParser()
                .parse(
                    it,
                    from = transitionTimeRange.start,
                    to = transitionTimeRange.end,
                    addInitialEntry = true,
                    clearCache = true,
                )
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws IOException if the artifact file doesn't exist or can't be read
     */
    @Throws(IOException::class)
    override fun readEventLogTrace(): EventLog? {
        return withTracing("readEventLogTrace") {
            val descriptor = ResultArtifactDescriptor(TraceType.EVENT_LOG)
            readBytes(descriptor)?.let {
                EventLogParser()
                    .parseSlice(it, from = transitionTimeRange.start, to = transitionTimeRange.end)
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws IOException if the artifact file doesn't exist or can't be read
     */
    @Throws(IOException::class)
    override fun readCujTrace(): CujTrace? = readEventLogTrace()?.cujTrace

    /** @return an [Reader] for the subsection of the trace we are reading in this reader */
    override fun slice(startTimestamp: Timestamp, endTimestamp: Timestamp): ResultReader {
        val slicedResult = result.slice(startTimestamp, endTimestamp)
        return ResultReader(slicedResult)
    }

    override fun toString(): String = "$result"

    /** @return the number of files in the artifact */
    @VisibleForTesting fun countFiles(): Int = result.artifacts.sumOf { it.traceCount() }

    /** @return if a file with type [traceType] linked to a [tag] exists in the artifact */
    fun hasTraceFile(traceType: TraceType, tag: String = Tag.ALL): Boolean {
        val descriptor = ResultArtifactDescriptor(traceType, tag)
        return result.artifacts.any { it.hasTrace(descriptor) }
    }
}
