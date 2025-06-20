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

import android.tools.io.Artifact
import android.tools.io.RunStatus
import android.tools.io.TraceType
import android.tools.traces.deleteIfExists
import android.tools.withTracing
import java.io.File

abstract class FileArtifact
internal constructor(
    private val testIdentifier: String,
    artifactFile: File,
    private val counter: Int,
    override val type: TraceType,
) : Artifact {
    var file: File = artifactFile
        private set

    init {
        require(testIdentifier.isNotEmpty()) { "Test identifier shouldn't be empty" }
    }

    override val runStatus: RunStatus
        get() =
            RunStatus.fromFileName(file.name)
                ?: error("Failed to get RunStatus from file name ${file.name}")

    override val absolutePath: String
        get() = file.absolutePath

    override val fileName: String
        get() = file.name

    override val stableId: String = "$testIdentifier$counter"

    override fun updateStatus(newStatus: RunStatus) {
        val currFile = file
        val newFile = getNewFilePath(newStatus)
        if (currFile != newFile) {
            withTracing("${this::class.simpleName}#updateStatus") {
                IoUtils.moveFile(currFile, newFile)
                file = newFile
            }
        }
    }

    override fun readBytes(): ByteArray = file.readBytes()

    override fun deleteIfExists() {
        file.deleteIfExists()
    }

    override fun toString(): String = fileName

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Artifact) return false

        if (absolutePath != other.absolutePath) return false

        return true
    }

    override fun hashCode(): Int {
        return absolutePath.hashCode()
    }

    /** updates the artifact status to [newStatus] */
    private fun getNewFilePath(newStatus: RunStatus): File {
        return file.resolveSibling(newStatus.generateArchiveNameFor(testIdentifier, counter, type))
    }
}
