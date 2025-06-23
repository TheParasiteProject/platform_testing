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

import android.tools.Timestamp
import android.tools.io.Artifact
import android.tools.io.ResultArtifactDescriptor
import android.tools.io.RunStatus
import android.tools.io.TraceType
import android.tools.io.TransitionTimeRange

/**
 * Contents of a flicker run (e.g. files, status, event log)
 *
 * @param _artifact Path to the artifact file
 * @param _transitionTimeRange Transition start and end time
 * @param _executionError Transition execution error (if any)
 */
open class ResultData(
    _artifacts: Array<Artifact>,
    _transitionTimeRange: TransitionTimeRange,
    _executionError: Throwable?,
) : IResultData {
    final override val artifacts: Array<Artifact> = _artifacts
    final override val transitionTimeRange: TransitionTimeRange = _transitionTimeRange
    final override val executionError: Throwable? = _executionError
    final override val runStatus: RunStatus
        get() {
            val status = artifacts.firstOrNull()?.runStatus ?: RunStatus.UNDEFINED
            require(artifacts.all { it.runStatus == status }) {
                "Sanity check failure: Not all artifacts have the same status"
            }
            return status
        }

    init {
        validateArtifacts()
    }

    /** {@inheritDoc} */
    override fun slice(startTimestamp: Timestamp, endTimestamp: Timestamp) = apply {
        require(startTimestamp.hasAllTimestamps) {
            "startTimestamp ($startTimestamp) has missing timestamps"
        }
        require(endTimestamp.hasAllTimestamps) {
            "endTimestamp ($endTimestamp) has missing timestamps"
        }
        return ResultData(
            artifacts,
            TransitionTimeRange(startTimestamp, endTimestamp),
            executionError,
        )
    }

    override fun toString(): String = buildString {
        append(artifacts)
        append(" (status=")
        append(runStatus)
        executionError?.let {
            append(", error=")
            append(it.message)
        }
        append(") ")
    }

    /** {@inheritDoc} */
    override fun updateStatus(newStatus: RunStatus) = apply {
        artifacts.forEach { it.updateStatus(newStatus) }
    }

    private fun validateArtifacts() {
        val typeToArtifact = mutableMapOf<TraceType, Artifact>()

        for (artifact in artifacts) {
            for (traceType in TraceType.entries) {
                if (artifact.hasTrace(ResultArtifactDescriptor(traceType))) {
                    require(!typeToArtifact.containsKey(traceType)) {
                        "Sanity check failure: multiple artifacts for trace type $traceType. " +
                            "Included in $artifact and ${typeToArtifact[traceType]}"
                    }

                    typeToArtifact.put(traceType, artifact)
                }
            }
        }
    }
}
