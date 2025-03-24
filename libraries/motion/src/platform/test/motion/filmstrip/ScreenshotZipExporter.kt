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

package platform.test.motion.filmstrip

import android.graphics.Bitmap
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Produces a zip file of the pngs generated from [screenshots]. */
class ScreenshotZipExporter(private val screenshots: List<MotionScreenshot>) : ScreenshotExporter {

    companion object {
        private const val FILE_EXTENSION = "screenshots.zip"
    }

    init {
        require(screenshots.isNotEmpty())
    }

    override val fileExtension: String = FILE_EXTENSION

    override fun exportToFile(file: File) {
        exportZipFromBitmaps(file)
    }

    private fun exportZipFromBitmaps(outputZipFile: File) {
        try {
            FileOutputStream(outputZipFile).use { fos ->
                BufferedOutputStream(fos).use { bos ->
                    ZipOutputStream(bos).use { zos ->
                        screenshots.forEachIndexed { index, motionScreenshot ->
                            val bitmap = motionScreenshot.bitmap

                            // Generate filename (padding ensures chronological order when sorted by
                            // name)
                            val paddedSuffix = index.toString().padStart(4, '0')
                            val entryName = "image_$paddedSuffix.png"

                            try {
                                addBitmapEntryToZip(zos, bitmap, entryName)
                            } catch (e: Exception) {
                                throw IOException(
                                    "Error adding bitmap entry '$entryName' at index $index: ${e.message}"
                                )
                            }
                        }
                        zos.finish()
                    }
                }
            }
        } catch (e: IOException) {
            // Clean up potentially partial file
            outputZipFile.delete()
            throw IOException("Error during ZIP creation: ${e.message}", e)
        } catch (e: Exception) {
            outputZipFile.delete()
            throw IOException("Unexpected error during ZIP creation: ${e.message}", e)
        }
    }

    private fun addBitmapEntryToZip(
        zos: ZipOutputStream,
        bitmap: Bitmap,
        entryName: String,
        quality: Int = 100,
    ) {
        val zipEntry = ZipEntry(entryName)
        zos.putNextEntry(zipEntry)
        val success = bitmap.compress(Bitmap.CompressFormat.PNG, quality, zos)
        if (!success) {
            throw IOException("Failed to compress bitmap for entry $entryName")
        }
        zos.closeEntry()
    }
}
