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
import android.tools.traces.io.WinscopeZipArtifact
import com.google.common.truth.Truth
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createTempDirectory
import org.junit.After
import org.junit.Before
import org.junit.Test

class WinscopeZipArtifactTest {
    private lateinit var outputDir: File

    @Before
    fun setup() {
        outputDir = createTempDirectory().toFile()
    }

    @After
    fun teardown() {
        outputDir.deleteRecursively()
    }

    private fun createTestZipFile(
        zipFileName: String,
        fileNamesAndContent: Map<String, String>,
    ): File {
        val zipFile = File(outputDir, zipFileName)
        zipFile.parentFile.mkdirs()
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            for ((fileName, content) in fileNamesAndContent) {
                val entry = ZipEntry(fileName)
                zos.putNextEntry(entry)
                zos.write(content.toByteArray())
                zos.closeEntry()
            }
        }
        return zipFile
    }

    @Test
    fun hasTraceFileExistsInZip() {
        val filesInZip =
            mapOf(
                TraceType.PERFETTO.fileName to "data1",
                TraceType.SCREEN_RECORDING.fileName to "data2",
            )
        val zipFile = createTestZipFile("archive.zip", filesInZip)
        val artifact = WinscopeZipArtifact(TEST_SCENARIO, zipFile, 0)
        Truth.assertThat(artifact.hasTrace(ResultArtifactDescriptor(TraceType.PERFETTO))).isTrue()
        Truth.assertThat(artifact.hasTrace(ResultArtifactDescriptor(TraceType.SCREEN_RECORDING)))
            .isTrue()
    }

    @Test
    fun hasTraceFileDoesNotExistInZip() {
        val filesInZip = mapOf("trace1.pb" to "data1")
        val zipFile = createTestZipFile("archive.zip", filesInZip)
        val artifact = WinscopeZipArtifact(TEST_SCENARIO, zipFile, 0)
        val descriptor = ResultArtifactDescriptor(TraceType.WM)
        Truth.assertThat(artifact.hasTrace(descriptor)).isFalse()
    }

    @Test
    fun traceCountCountsFilesInZip() {
        val filesInZip =
            mapOf("trace1.pb" to "data1", "trace2.wintrace" to "data2", "log.txt" to "log")
        val zipFile = createTestZipFile("archive.zip", filesInZip)
        val artifact = WinscopeZipArtifact(TEST_SCENARIO, zipFile, 0)
        Truth.assertThat(artifact.traceCount()).isEqualTo(filesInZip.size)
    }

    @Test
    fun traceCountEmptyZip() {
        val zipFile = createTestZipFile("empty.zip", emptyMap())
        val artifact = WinscopeZipArtifact(TEST_SCENARIO, zipFile, 0)
        Truth.assertThat(artifact.traceCount()).isEqualTo(0)
    }

    @Test
    fun readBytesDescriptorFileExistsInZip() {
        val content = "This is specific trace data"
        val filesInZip =
            mapOf(
                TraceType.PERFETTO.fileName to content,
                TraceType.SCREEN_RECORDING.fileName to "other data",
            )
        val zipFile = createTestZipFile("archive.zip", filesInZip)
        val artifact = WinscopeZipArtifact(TEST_SCENARIO, zipFile, 0)
        val descriptor = ResultArtifactDescriptor(TraceType.PERFETTO)
        val bytes = artifact.readBytes(descriptor)
        Truth.assertThat(bytes).isNotNull()
        Truth.assertThat(String(bytes!!)).isEqualTo(content)
    }

    @Test
    fun readBytesDescriptorFileDoesNotExistInZip() {
        val filesInZip = mapOf("actual_trace.pb" to "some data")
        val zipFile = createTestZipFile("archive.zip", filesInZip)
        val artifact = WinscopeZipArtifact(TEST_SCENARIO, zipFile, 0)
        val descriptor = ResultArtifactDescriptor(TraceType.WM)
        Truth.assertThat(artifact.readBytes(descriptor)).isNull()
    }

    @Test(expected = FileNotFoundException::class)
    fun readBytesDescriptorZipFileNotFoundThrowsException() {
        val nonExistentZip = File(outputDir, "non_existent.zip")
        val artifact = WinscopeZipArtifact(TEST_SCENARIO, nonExistentZip, 0)
        val descriptor = ResultArtifactDescriptor(TraceType.PERFETTO)
        artifact.readBytes(descriptor) // Should throw
    }

    @Test
    fun readBytesZipDeletedAfterCreationThrowsException() {
        val filesInZip = mapOf(TraceType.PERFETTO.fileName to "data")
        val zipFile = createTestZipFile("deletable.zip", filesInZip)
        val artifact = WinscopeZipArtifact(TEST_SCENARIO, zipFile, 0)
        val descriptor = ResultArtifactDescriptor(TraceType.PERFETTO)

        // Read once, should work
        Truth.assertThat(String(artifact.readBytes(descriptor)!!)).isEqualTo("data")

        // Delete the zip file
        zipFile.delete()

        var exceptionThrown = false
        try {
            artifact.readBytes(descriptor)
        } catch (e: FileNotFoundException) {
            exceptionThrown = true
        }
        Truth.assertThat(exceptionThrown).isTrue()
    }

    @Test
    fun updateStatusRenamesZipFile() {
        val initialFileName =
            RunStatus.RUN_EXECUTED.generateArchiveNameFor(TEST_SCENARIO, 0, TraceType.WINSCOPE_ZIP)
        val zipFile = createTestZipFile(initialFileName, mapOf("entry.txt" to "data"))
        val artifact = WinscopeZipArtifact(TEST_SCENARIO, zipFile, 0)

        Truth.assertThat(artifact.file.name).isEqualTo(initialFileName)
        Truth.assertThat(artifact.runStatus).isEqualTo(RunStatus.RUN_EXECUTED)

        artifact.updateStatus(RunStatus.ASSERTION_SUCCESS)
        val newFileName =
            RunStatus.ASSERTION_SUCCESS.generateArchiveNameFor(
                TEST_SCENARIO,
                0,
                TraceType.WINSCOPE_ZIP,
            )

        Truth.assertThat(artifact.runStatus).isEqualTo(RunStatus.ASSERTION_SUCCESS)
        Truth.assertThat(artifact.file.name).isEqualTo(newFileName)
        Truth.assertThat(File(outputDir, initialFileName).exists()).isFalse()
        Truth.assertThat(File(outputDir, newFileName).exists()).isTrue()
    }

    @Test
    fun deleteIfExistsDeletesZipFile() {
        val zipFile = createTestZipFile("toBeDeleted.zip", mapOf("entry.txt" to "data"))
        val artifact = WinscopeZipArtifact(TEST_SCENARIO, zipFile, 0)
        Truth.assertThat(artifact.file.exists()).isTrue()
        artifact.deleteIfExists()
        Truth.assertThat(artifact.file.exists()).isFalse()
    }

    @Test
    fun equalityTestSameUnderlyingZipFile() {
        val zipFile = createTestZipFile("file.zip", mapOf("a" to "b"))
        val artifact1 = WinscopeZipArtifact(TEST_SCENARIO, zipFile, 0)
        val artifact2 = WinscopeZipArtifact(TEST_SCENARIO, zipFile, 0)
        Truth.assertThat(artifact1).isEqualTo(artifact2)
    }

    @Test
    fun equalityTestFifferentUnderlyingZipFilesSameNameInDifferentDir() {
        val zipFile1 = createTestZipFile("file.zip", mapOf("a" to "b"))

        val outputDir2 = createTempDirectory("outputDir2").toFile()
        val zipFile2 = File(outputDir2, "file.zip")
        ZipOutputStream(FileOutputStream(zipFile2)).use { zos ->
            zos.putNextEntry(ZipEntry("c"))
            zos.write("d".toByteArray())
            zos.closeEntry()
        }

        val artifact1 = WinscopeZipArtifact(TEST_SCENARIO, zipFile1, 0)
        val artifact2 = WinscopeZipArtifact(TEST_SCENARIO, zipFile2, 0)
        Truth.assertThat(artifact1).isNotEqualTo(artifact2)
        outputDir2.deleteRecursively()
    }

    @Test
    fun equalityTestDifferentUnderlyingZipFilesDifferentName() {
        val zipFile1 = createTestZipFile("file1.zip", mapOf("a" to "b"))
        val zipFile2 = createTestZipFile("file2.zip", mapOf("a" to "b"))
        val artifact1 = WinscopeZipArtifact(TEST_SCENARIO, zipFile1, 0)
        val artifact2 = WinscopeZipArtifact(TEST_SCENARIO, zipFile2, 0)
        Truth.assertThat(artifact1).isNotEqualTo(artifact2)
    }
}
