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

package android.tools.traces.io

import android.tools.io.FLICKER_IO_TAG
import android.tools.io.ResultArtifactDescriptor
import android.tools.io.TraceType
import android.util.Log
import java.io.File
import java.io.IOException

class SingleTraceFileArtifact(
    testIdentifier: String,
    artifactFile: File,
    counter: Int,
    type: TraceType,
) : FileArtifact(testIdentifier, artifactFile, counter, type) {

    override fun hasTrace(descriptor: ResultArtifactDescriptor) = type == descriptor.traceType

    override fun traceCount() = 1

    @Throws(IOException::class)
    override fun readBytes(descriptor: ResultArtifactDescriptor): ByteArray? {
        Log.d(FLICKER_IO_TAG, "Reading descriptor=$descriptor from $this")
        return if (type == descriptor.traceType) readBytes() else null
    }
}
