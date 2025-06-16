/*
 * Copyright (C) 2024 The Android Open Source Project
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
import android.tools.Timestamps
import android.tools.io.Artifact
import android.tools.io.FLICKER_IO_TAG
import android.tools.io.ResultArtifactDescriptor
import android.tools.io.RunStatus
import android.tools.io.TraceType
import android.tools.io.TransitionTimeRange
import android.tools.withTracing
import android.util.Log
import java.io.File
import kotlin.collections.toTypedArray

/** Helper class to create run result artifact files */
open class ResultWriter {
    protected var testIdentifier: String = ""
    private var runStatus: RunStatus = RunStatus.UNDEFINED
    private val files = mutableMapOf<ResultArtifactDescriptor, File>()
    private var transitionStartTime = Timestamps.min()
    private var transitionEndTime = Timestamps.max()
    private var executionError: Throwable? = null
    private var outputDir: File? = null

    /** Sets the artifact name to [value] */
    fun withName(value: String) = apply { testIdentifier = value }

    /** Sets the artifact transition start time to [time] */
    fun setTransitionStartTime(time: Timestamp) = apply { transitionStartTime = time }

    /** Sets the artifact transition end time to [time] */
    fun setTransitionEndTime(time: Timestamp) = apply { transitionEndTime = time }

    /** Sets the artifact status as successfully executed transition ([RunStatus.RUN_EXECUTED]) */
    fun setRunComplete() = apply { runStatus = RunStatus.RUN_EXECUTED }

    /** Sets the dir where the artifact file will be stored to [dir] */
    fun withOutputDir(dir: File) = apply { outputDir = dir }

    /**
     * Sets the artifact status as failed executed transition ([RunStatus.RUN_FAILED])
     *
     * @param error that caused the transition to fail
     */
    fun setRunFailed(error: Throwable) = apply {
        runStatus = RunStatus.RUN_FAILED
        executionError = error
    }

    /**
     * Adds [artifact] to the result artifact
     *
     * @param traceType used when adding [artifact] to the result artifact
     * @param tag used when adding [artifact] to the result artifact
     */
    fun addTraceResult(traceType: TraceType, artifact: File, tag: String = Tag.ALL) = apply {
        Log.d(
            FLICKER_IO_TAG,
            "Add trace result file=$artifact type=$traceType tag=$tag testIdentifier=$testIdentifier",
        )
        val fileDescriptor = ResultArtifactDescriptor(traceType, tag)
        require(!files.containsKey(fileDescriptor)) { "File already added: $fileDescriptor" }
        files[fileDescriptor] = artifact
    }

    /** @return writes the result artifact to disk and returns it */
    open fun write(): IResultData {
        return withTracing("write") {
            val outputDir = outputDir
            requireNotNull(outputDir) { "Output dir not configured" }
            require(testIdentifier.isNotEmpty()) { "Test identifier shouldn't be empty" }

            if (runStatus == RunStatus.UNDEFINED) {
                Log.w(FLICKER_IO_TAG, "Writing result with $runStatus run status")
            }

            val screenRecordings = files.filter { it.key.traceType == TraceType.SCREEN_RECORDING }
            val otherTraces = files.filter { it.key.traceType != TraceType.SCREEN_RECORDING }

            val winscopeArtifact =
                ArtifactBuilder()
                    .withName(testIdentifier)
                    .withOutputDir(outputDir)
                    .withStatus(runStatus)
                    .withFiles(otherTraces)
                    .build()

            require(screenRecordings.size <= 1) { "Screen recording should be a single file" }
            val screenRecording =
                if (screenRecordings.isEmpty()) {
                    null
                } else {
                    ArtifactBuilder()
                        .withName(testIdentifier)
                        .withOutputDir(outputDir)
                        .withStatus(runStatus)
                        .withFiles(screenRecordings)
                        .build()
                }

            val artifacts =
                arrayOf(winscopeArtifact, screenRecording).filterNotNull().toTypedArray<Artifact>()

            ResultData(
                artifacts,
                TransitionTimeRange(transitionStartTime, transitionEndTime),
                executionError,
            )
        }
    }
}
