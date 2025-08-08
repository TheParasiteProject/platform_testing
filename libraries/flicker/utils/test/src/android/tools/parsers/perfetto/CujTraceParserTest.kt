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

package android.tools.parsers.perfetto

import android.tools.Cache
import android.tools.testutils.CleanFlickerEnvironmentRule
import android.tools.testutils.readAsset
import android.tools.traces.events.CujType
import android.tools.traces.events.ICujType
import android.tools.traces.parsers.perfetto.CujTraceParser
import android.tools.traces.parsers.perfetto.TraceProcessorSession
import com.google.common.truth.Truth
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test

class CujTraceParserTest {
    @Before
    fun before() {
        Cache.clear()
    }

    @Test
    fun canParseFromTrace() {
        val trace =
            TraceProcessorSession.loadPerfettoTrace(readAsset("cujs.pftrace")) { session ->
                CujTraceParser().parse(session)
            }
        Truth.assertWithMessage("Trace").that(trace.entries).isNotEmpty()
        Truth.assertWithMessage("Trace contains corrects number of entries")
            .that(trace.entries.size)
            .isEqualTo(3)
        Truth.assertWithMessage("Trace contains correct CUJs")
            .that(trace.entries.map { it.cuj })
            .isEqualTo(
                listOf<ICujType>(
                    CujType.CUJ_LAUNCHER_APP_LAUNCH_FROM_ICON,
                    CujType.CUJ_LAUNCHER_APP_CLOSE_TO_HOME,
                    CujType.CUJ_RECENTS_SCROLLING,
                )
            )
    }

    companion object {
        @ClassRule @JvmField val ENV_CLEANUP = CleanFlickerEnvironmentRule()
    }
}
