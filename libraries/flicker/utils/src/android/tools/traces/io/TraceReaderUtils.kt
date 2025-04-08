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

import android.content.Context
import android.tools.Scenario
import android.tools.Timestamps
import android.tools.io.Reader
import android.tools.io.TransitionTimeRange
import android.tools.traces.TRACE_CONFIG_REQUIRE_CHANGES
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.IOException

object TraceReaderUtils {
    fun getTraceReaderFromAsset(scenario: Scenario, path: String): Reader {
        val file = readAssetAsFile(path)

        val artifact = FileArtifact(scenario, file, 0)

        val result =
            ResultData(
                artifact,
                // TODO (b/408161530): We need to somehow store this in the trace or pass it in some
                //  way otherwise we will be analyzing too much data (the test rules, before and
                //  after blocks included)
                TransitionTimeRange(Timestamps.min(), Timestamps.max()),
                null,
            )

        return ResultReaderWithLru(result, TRACE_CONFIG_REQUIRE_CHANGES)
    }

    @Throws(IOException::class)
    fun readAssetAsFile(path: String): File {
        val context: Context = InstrumentationRegistry.getInstrumentation().context
        return File(context.cacheDir, path).also {
            if (!it.exists()) {
                it.parentFile.mkdirs()
                it.createNewFile()
                it.outputStream().use { cache ->
                    context.assets.open(path).use { inputStream -> inputStream.copyTo(cache) }
                }
            }
        }
    }
}
