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

import android.tools.io.BUFFER_SIZE
import android.tools.io.FLICKER_IO_TAG
import android.tools.io.ResultArtifactDescriptor
import android.tools.io.TraceType
import android.util.Log
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class WinscopeZipArtifact(testIdentifier: String, artifactFile: File, counter: Int) :
    FileArtifact(testIdentifier, artifactFile, counter, TraceType.WINSCOPE_ZIP) {

    override fun hasTrace(descriptor: ResultArtifactDescriptor): Boolean {
        var found = false
        forEachFileInZip { found = found || (it.name == descriptor.fileNameInArtifact) }
        return found
    }

    override fun traceCount(): Int {
        var count = 0
        forEachFileInZip { count++ }
        return count
    }

    @Throws(IOException::class)
    override fun readBytes(descriptor: ResultArtifactDescriptor): ByteArray? {
        Log.d(FLICKER_IO_TAG, "Reading descriptor=$descriptor from $this")

        var foundFile = false
        val outByteArray = ByteArrayOutputStream()
        val tmpBuffer = ByteArray(BUFFER_SIZE)
        withZipFile {
            var zipEntry: ZipEntry? = it.nextEntry
            while (zipEntry != null) {
                if (zipEntry.name == descriptor.fileNameInArtifact) {
                    val outputStream = BufferedOutputStream(outByteArray, BUFFER_SIZE)
                    try {
                        var size = it.read(tmpBuffer, 0, BUFFER_SIZE)
                        while (size > 0) {
                            outputStream.write(tmpBuffer, 0, size)
                            size = it.read(tmpBuffer, 0, BUFFER_SIZE)
                        }
                        it.closeEntry()
                    } finally {
                        outputStream.flush()
                        outputStream.close()
                    }
                    foundFile = true
                    break
                }
                zipEntry = it.nextEntry
            }
        }

        return if (foundFile) outByteArray.toByteArray() else null
    }

    private fun withZipFile(predicate: (ZipInputStream) -> Unit) {
        if (!file.exists()) {
            val directory = file.parentFile
            val files =
                try {
                    directory?.listFiles()?.filterNot { it.isDirectory }?.map { it.absolutePath }
                } catch (e: Throwable) {
                    null
                }
            throw FileNotFoundException(
                buildString {
                    append(file)
                    appendLine(" could not be found!")
                    append("Found ")
                    append(files?.joinToString()?.ifEmpty { "no files" })
                    append(" in ")
                    append(directory?.absolutePath)
                }
            )
        }

        val zipInputStream = ZipInputStream(BufferedInputStream(FileInputStream(file), BUFFER_SIZE))
        try {
            predicate(zipInputStream)
        } finally {
            zipInputStream.closeEntry()
            zipInputStream.close()
        }
    }

    private fun forEachFileInZip(predicate: (ZipEntry) -> Unit) {
        withZipFile {
            var zipEntry: ZipEntry? = it.nextEntry
            while (zipEntry != null) {
                predicate(zipEntry)
                zipEntry = it.nextEntry
            }
        }
    }
}
