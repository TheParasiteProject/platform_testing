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

package android.tools.io

import android.tools.testutils.TEST_SCENARIO
import android.tools.traces.io.SingleTraceFileArtifact
import com.google.common.truth.Truth
import java.io.File
import java.io.FileNotFoundException
import kotlin.io.path.createTempDirectory
import org.junit.After
import org.junit.Before
import org.junit.Test

class SingleTraceFileArtifactTest {
    private lateinit var outputDir: File

    @Before
    fun setup() {
        outputDir = createTempDirectory().toFile()
    }

    @After
    fun teardown() {
        outputDir.deleteRecursively()
    }

    private fun createTestFile(name: String, content: String = "test data"): File {
        val file = File(outputDir, name)
        file.parentFile.mkdirs()
        file.writeText(content)
        return file
    }

    @Test
    fun hasTraceMatchesType() {
        val traceFile = createTestFile("trace.pb")
        val artifact = SingleTraceFileArtifact(TEST_SCENARIO, traceFile, 0, TraceType.PERFETTO)
        val descriptor = ResultArtifactDescriptor(TraceType.PERFETTO)
        Truth.assertThat(artifact.hasTrace(descriptor)).isTrue()
    }

    @Test
    fun hasTraceMismatchesType() {
        val traceFile = createTestFile("trace.pb")
        val artifact = SingleTraceFileArtifact(TEST_SCENARIO, traceFile, 0, TraceType.PERFETTO)
        val descriptor = ResultArtifactDescriptor(TraceType.WM)
        Truth.assertThat(artifact.hasTrace(descriptor)).isFalse()
    }

    @Test
    fun traceCountIsAlwaysOne() {
        val traceFile = createTestFile("trace.pb")
        val artifact = SingleTraceFileArtifact(TEST_SCENARIO, traceFile, 0, TraceType.PERFETTO)
        Truth.assertThat(artifact.traceCount()).isEqualTo(1)
    }

    @Test
    fun readBytesDescriptorMatchesType() {
        val content = "This is transaction data"
        val traceFile = createTestFile("transaction.pb", content)
        val artifact = SingleTraceFileArtifact(TEST_SCENARIO, traceFile, 0, TraceType.PERFETTO)
        val descriptor = ResultArtifactDescriptor(TraceType.PERFETTO)
        val bytes = artifact.readBytes(descriptor)
        Truth.assertThat(bytes).isNotNull()
        Truth.assertThat(String(bytes!!)).isEqualTo(content)
    }

    @Test
    fun readBytesDescriptorMismatchesType() {
        val traceFile = createTestFile("some_other_trace.pb")
        val artifact = SingleTraceFileArtifact(TEST_SCENARIO, traceFile, 0, TraceType.PERFETTO)
        val descriptor = ResultArtifactDescriptor(TraceType.WM)
        Truth.assertThat(artifact.readBytes(descriptor)).isNull()
    }

    @Test(expected = FileNotFoundException::class)
    fun readBytesDescriptorMatchesTypeFileNotFoundThrowsException() {
        val nonExistentFile = File(outputDir, "non_existent.pb")
        // Ensure the file does not exist for this test case
        if (nonExistentFile.exists()) {
            nonExistentFile.delete()
        }
        val artifact =
            SingleTraceFileArtifact(TEST_SCENARIO, nonExistentFile, 0, TraceType.PERFETTO)
        val descriptor = ResultArtifactDescriptor(TraceType.PERFETTO)
        // This should attempt to read the non-existent file because types match,
        // leading to parent FileArtifact.readBytes() throwing FileNotFoundException
        artifact.readBytes(descriptor)
    }

    @Test
    fun readBytesUnderlyingFileDeletedAfterCreationThrowsException() {
        val content = "Initial content"
        val traceFile = createTestFile("deletable_trace.pb", content)
        val artifact = SingleTraceFileArtifact(TEST_SCENARIO, traceFile, 0, TraceType.PERFETTO)
        val descriptor = ResultArtifactDescriptor(TraceType.PERFETTO)

        // Read once, should work
        Truth.assertThat(String(artifact.readBytes(descriptor)!!)).isEqualTo(content)

        // Delete the file
        traceFile.delete()

        var exceptionThrown = false
        try {
            artifact.readBytes(descriptor)
        } catch (e: FileNotFoundException) {
            exceptionThrown = true
        }
        Truth.assertThat(exceptionThrown).isTrue()
    }

    @Test
    fun updateStatusRenamesFile() {
        val initialFileName =
            RunStatus.RUN_EXECUTED.generateArchiveNameFor(TEST_SCENARIO, 0, TraceType.PERFETTO)
        val traceFile = createTestFile(initialFileName)
        val artifact = SingleTraceFileArtifact(TEST_SCENARIO, traceFile, 0, TraceType.PERFETTO)

        Truth.assertThat(artifact.file.name).isEqualTo(initialFileName)
        Truth.assertThat(artifact.runStatus).isEqualTo(RunStatus.RUN_EXECUTED)

        artifact.updateStatus(RunStatus.ASSERTION_SUCCESS)
        val newFileName =
            RunStatus.ASSERTION_SUCCESS.generateArchiveNameFor(TEST_SCENARIO, 0, TraceType.PERFETTO)

        Truth.assertThat(artifact.runStatus).isEqualTo(RunStatus.ASSERTION_SUCCESS)
        Truth.assertThat(artifact.file.name).isEqualTo(newFileName)
        Truth.assertThat(File(outputDir, initialFileName).exists()).isFalse()
        Truth.assertThat(File(outputDir, newFileName).exists()).isTrue()
    }

    @Test
    fun deleteIfExistsDeletesFile() {
        val traceFile = createTestFile("toBeDeleted.pb")
        val artifact = SingleTraceFileArtifact(TEST_SCENARIO, traceFile, 0, TraceType.PERFETTO)
        Truth.assertThat(artifact.file.exists()).isTrue()
        artifact.deleteIfExists()
        Truth.assertThat(artifact.file.exists()).isFalse()
    }

    @Test
    fun equalityTestSameUnderlyingFile() {
        val traceFile = createTestFile("file.pb")
        val artifact1 = SingleTraceFileArtifact(TEST_SCENARIO, traceFile, 0, TraceType.PERFETTO)
        val artifact2 = SingleTraceFileArtifact(TEST_SCENARIO, traceFile, 0, TraceType.PERFETTO)
        Truth.assertThat(artifact1).isEqualTo(artifact2)
    }

    @Test
    fun equalityTestDifferentUnderlyingFilesSameNameInDifferentDir() {
        val traceFile1 = createTestFile("file.pb")

        val outputDir2 = createTempDirectory("outputDir2").toFile()
        val traceFile2 = File(outputDir2, "file.pb")
        traceFile2.parentFile.mkdirs()
        traceFile2.writeText("data")

        val artifact1 = SingleTraceFileArtifact(TEST_SCENARIO, traceFile1, 0, TraceType.PERFETTO)
        val artifact2 = SingleTraceFileArtifact(TEST_SCENARIO, traceFile2, 0, TraceType.PERFETTO)
        Truth.assertThat(artifact1).isNotEqualTo(artifact2)
        outputDir2.deleteRecursively()
    }

    @Test
    fun equalityTestDifferentUnderlyingFilesDifferentName() {
        val traceFile1 = createTestFile("file1.pb")
        val traceFile2 = createTestFile("file2.pb")
        val artifact1 = SingleTraceFileArtifact(TEST_SCENARIO, traceFile1, 0, TraceType.PERFETTO)
        val artifact2 = SingleTraceFileArtifact(TEST_SCENARIO, traceFile2, 0, TraceType.PERFETTO)
        Truth.assertThat(artifact1).isNotEqualTo(artifact2)
    }
}
