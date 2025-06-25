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

package android.tools.io

import android.tools.Timestamps
import android.tools.testutils.CleanFlickerEnvironmentRule
import android.tools.testutils.TestTraces
import android.tools.testutils.assertThrows
import android.tools.testutils.newTestResultWriter
import android.tools.testutils.outputFileName
import android.tools.traces.deleteIfExists
import android.tools.traces.io.ResultData
import android.tools.traces.io.ResultReader
import com.google.common.truth.Truth
import java.io.FileNotFoundException
import kotlin.io.path.createTempDirectory
import org.junit.Before
import org.junit.ClassRule
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

/** Tests for [ResultReader] */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class ResultReaderTest {
    @Before
    fun setup() {
        outputFileName(RunStatus.RUN_EXECUTED).deleteIfExists()
        outputFileName(RunStatus.ASSERTION_SUCCESS).deleteIfExists()
    }

    @Test
    fun failFileNotFound() {
        val data = newTestResultWriter().write()
        data.artifacts.forEach { it.deleteIfExists() }
        val reader = ResultReader(data)
        assertThrows<FileNotFoundException> {
            reader.readTransitionsTrace() ?: error("Should have failed")
        }
    }

    @Test
    fun emptyResult() {
        val result =
            ResultData(
                emptyArray(),
                TransitionTimeRange(Timestamps.empty(), Timestamps.empty()),
                null,
            )
        val reader = ResultReader(result)
        Truth.assertThat(reader.countFiles()).isEqualTo(0)
        Truth.assertThat(reader.hasTraceFile(TraceType.WM)).isFalse()
        Truth.assertThat(reader.runStatus).isEqualTo(RunStatus.UNDEFINED)
    }

    @Test
    fun canReadFromMultipleArtifacts() {
        val writer1 =
            newTestResultWriter()
                .withOutputDir(createTempDirectory().toFile())
                .addTraceResult(TraceType.EVENT_LOG, TestTraces.EventLog.FILE)
        val result1 = writer1.write()

        val writer2 =
            newTestResultWriter()
                .withOutputDir(createTempDirectory().toFile())
                .addTraceResult(TraceType.PERFETTO, TestTraces.LayerTrace.FILE)
        val result2 = writer2.write()

        val combinedArtifacts = result1.artifacts + result2.artifacts
        val combinedResult = ResultData(combinedArtifacts, result1.transitionTimeRange, null)

        val reader = ResultReader(combinedResult)

        Truth.assertThat(reader.countFiles()).isEqualTo(2)
        Truth.assertThat(reader.hasTraceFile(TraceType.EVENT_LOG)).isTrue()
        Truth.assertThat(reader.hasTraceFile(TraceType.PERFETTO)).isTrue()
        Truth.assertThat(reader.readEventLogTrace()).isNotNull()
        Truth.assertThat(reader.readLayersTrace()).isNotNull()
    }

    @Test
    fun updateStatusOnAllArtifacts() {
        val writer1 =
            newTestResultWriter().withOutputDir(createTempDirectory().toFile()).setRunComplete()
        val result1 = writer1.write()

        val writer2 =
            newTestResultWriter().withOutputDir(createTempDirectory().toFile()).setRunComplete()
        val result2 = writer2.write()

        val combinedArtifacts = result1.artifacts + result2.artifacts
        val combinedResult = ResultData(combinedArtifacts, result1.transitionTimeRange, null)

        val reader = ResultReader(combinedResult)
        reader.result.updateStatus(RunStatus.ASSERTION_SUCCESS)

        Truth.assertThat(reader.runStatus).isEqualTo(RunStatus.ASSERTION_SUCCESS)
        Truth.assertThat(result1.runStatus).isEqualTo(RunStatus.ASSERTION_SUCCESS)
        Truth.assertThat(result2.runStatus).isEqualTo(RunStatus.ASSERTION_SUCCESS)
    }

    @Test
    fun slicedResultKeepsStatusInSync() {
        val data = newTestResultWriter().setRunComplete().write()
        val reader = ResultReader(data)
        val slicedReader = reader.slice(Timestamps.min(), Timestamps.max())
        reader.result.updateStatus(RunStatus.ASSERTION_SUCCESS)

        Truth.assertThat(reader.runStatus).isEqualTo(RunStatus.ASSERTION_SUCCESS)
        Truth.assertThat(reader.runStatus).isEqualTo(slicedReader.runStatus)

        for (artifact in reader.result.artifacts) {
            Truth.assertThat(artifact.runStatus).isEqualTo(reader.runStatus)
            Truth.assertThat(artifact.absolutePath).contains(RunStatus.ASSERTION_SUCCESS.prefix)
        }

        for (i in reader.result.artifacts.indices) {
            Truth.assertThat(reader.result.artifacts[i].absolutePath)
                .isEqualTo(slicedReader.result.artifacts[i].absolutePath)
        }
    }

    companion object {
        @ClassRule @JvmField val ENV_CLEANUP = CleanFlickerEnvironmentRule()
    }
}
