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
import com.google.common.truth.Truth
import org.junit.ClassRule
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

/** Tests for [ResultArtifactDescriptor] */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class ResultArtifactDescriptorTest {
    @Test
    fun generateDescriptorFromTrace() {
        createDescriptorAndValidateFileName(TraceType.PERFETTO)
        createDescriptorAndValidateFileName(TraceType.WM)
        createDescriptorAndValidateFileName(TraceType.PERFETTO)
        createDescriptorAndValidateFileName(TraceType.PERFETTO)
        createDescriptorAndValidateFileName(TraceType.SCREEN_RECORDING)
        createDescriptorAndValidateFileName(TraceType.WM_DUMP)
    }

    @Test
    fun generateDescriptorFromTraceWithTags() {
        createDescriptorAndValidateFileNameWithTag(TraceType.PERFETTO)
        createDescriptorAndValidateFileNameWithTag(TraceType.WM)
        createDescriptorAndValidateFileNameWithTag(TraceType.PERFETTO)
        createDescriptorAndValidateFileNameWithTag(TraceType.PERFETTO)
        createDescriptorAndValidateFileNameWithTag(TraceType.SCREEN_RECORDING)
        createDescriptorAndValidateFileNameWithTag(TraceType.WM_DUMP)
    }

    private fun createDescriptorAndValidateFileName(traceType: TraceType) {
        val descriptor = ResultArtifactDescriptor(traceType)
        Truth.assertWithMessage("Result file name")
            .that(descriptor.fileNameInArtifact)
            .isEqualTo(traceType.fileName)
    }

    private fun createDescriptorAndValidateFileNameWithTag(traceType: TraceType) {
        val tag = "testTag"
        val descriptor = ResultArtifactDescriptor(traceType, TEST_TAG)
        val subject =
            Truth.assertWithMessage("Result file name").that(descriptor.fileNameInArtifact)
        subject.startsWith(tag)
        subject.endsWith(traceType.fileName)
    }

    companion object {
        private const val TEST_TAG = "testTag"

        @ClassRule @JvmField val ENV_CLEANUP = CleanFlickerEnvironmentRule()
    }
}
