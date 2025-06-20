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

package android.tools.io

import android.tools.testutils.CleanFlickerEnvironmentRule
import android.tools.testutils.TEST_SCENARIO_KEY
import android.tools.testutils.createDefaultArtifactBuilder
import android.tools.traces.io.ArtifactBuilder
import com.google.common.truth.Truth
import java.io.File
import kotlin.io.path.createTempDirectory
import org.junit.ClassRule
import org.junit.Test

/** Tests for [ArtifactBuilder] */
class ArtifactBuilderTest {
    @Test
    fun buildArtifactAndFileExists() {
        for (status in RunStatus.entries) {
            val artifact = createDefaultArtifactBuilder(TEST_SCENARIO_KEY, status).build()
            Truth.assertWithMessage("Artifact exists").that(artifact.file.exists()).isTrue()
            artifact.deleteIfExists()
        }
    }

    @Test
    fun buildArtifactWithStatus() {
        for (status in RunStatus.entries) {
            val artifact = createDefaultArtifactBuilder(TEST_SCENARIO_KEY, status).build()
            val statusFromFile = RunStatus.fromFileName(artifact.file.name)
            Truth.assertWithMessage("Run status").that(statusFromFile).isEqualTo(status)
            artifact.deleteIfExists()
        }
    }

    @Test
    fun buildArtifactWithScenario() {
        val artifact = createDefaultArtifactBuilder(TEST_SCENARIO_KEY, RunStatus.RUN_FAILED).build()
        Truth.assertWithMessage("Scenario").that(artifact.file.name).contains(TEST_SCENARIO_KEY)
        artifact.deleteIfExists()
    }

    @Test
    fun buildArtifactWithOutputDir() {
        val expectedDir = createTempDirectory().toFile()
        val artifact =
            createDefaultArtifactBuilder(
                    TEST_SCENARIO_KEY,
                    RunStatus.RUN_FAILED,
                    outputDir = expectedDir,
                )
                .build()
        Truth.assertWithMessage("Output dir").that(artifact.file.parentFile).isEqualTo(expectedDir)
        artifact.deleteIfExists()
    }

    @Test
    fun buildArtifactWithFiles() {
        val expectedDescriptor = ResultArtifactDescriptor(TraceType.WM)
        val expectedFiles = mapOf(expectedDescriptor to File.createTempFile("test", ""))
        val artifact =
            createDefaultArtifactBuilder(
                    TEST_SCENARIO_KEY,
                    RunStatus.RUN_FAILED,
                    files = expectedFiles,
                )
                .build()
        Truth.assertWithMessage("Trace count").that(artifact.traceCount()).isEqualTo(1)
        Truth.assertWithMessage("Trace type").that(artifact.hasTrace(expectedDescriptor)).isTrue()
        artifact.deleteIfExists()
    }

    @Test
    fun buildArtifactAvoidDuplicate() {
        val builder = createDefaultArtifactBuilder(TEST_SCENARIO_KEY, RunStatus.RUN_FAILED)
        val artifact1 = builder.build()
        val artifact2 = builder.build()

        Truth.assertWithMessage("Artifact 1 exists").that(artifact1.file.exists()).isTrue()
        Truth.assertWithMessage("Artifact 2 exists").that(artifact2.file.exists()).isTrue()
        Truth.assertWithMessage("Different files")
            .that(artifact2.file.name)
            .isNotEqualTo(artifact1.file.name)
        Truth.assertWithMessage("Artifact 2 name")
            .that(artifact2.file.name)
            .endsWith("_1.winscope.zip")
    }

    @Test
    fun buildTwoArtifactsThatAreDifferentWithSameStatus() {
        val builder = createDefaultArtifactBuilder(TEST_SCENARIO_KEY, RunStatus.RUN_FAILED)
        val artifact1 = builder.build()
        val artifact2 = builder.build()

        Truth.assertWithMessage("Artifacts are equal").that(artifact1).isNotEqualTo(artifact2)
    }

    @Test
    fun buildTwoArtifactsThatAreDifferent() {
        val artifact1 =
            createDefaultArtifactBuilder(TEST_SCENARIO_KEY, RunStatus.RUN_FAILED).build()
        val artifact2 =
            createDefaultArtifactBuilder(TEST_SCENARIO_KEY, RunStatus.PARSING_FAILURE).build()
        Truth.assertWithMessage("Artifacts are equal").that(artifact1).isNotEqualTo(artifact2)
    }

    @Test(expected = IllegalStateException::class)
    fun failArtifactWithoutStatus() {
        ArtifactBuilder()
            .withName(TEST_SCENARIO_KEY)
            .withOutputDir(createTempDirectory().toFile())
            .withFiles(emptyMap())
            .build()
    }

    @Test(expected = IllegalArgumentException::class)
    fun failArtifactWithoutScenario() {
        ArtifactBuilder()
            .withOutputDir(createTempDirectory().toFile())
            .withStatus(RunStatus.RUN_FAILED)
            .withFiles(emptyMap())
            .build()
    }

    @Test(expected = IllegalStateException::class)
    fun failArtifactWithoutOutputDir() {
        ArtifactBuilder()
            .withName(TEST_SCENARIO_KEY)
            .withStatus(RunStatus.RUN_FAILED)
            .withFiles(emptyMap())
            .build()
    }

    companion object {
        @ClassRule @JvmField val ENV_CLEANUP = CleanFlickerEnvironmentRule()
    }
}
