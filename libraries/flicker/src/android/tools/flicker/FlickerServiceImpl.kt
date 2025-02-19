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

package android.tools.flicker

import android.tools.flicker.config.FlickerConfig
import android.tools.io.Reader
import android.tools.withTracing

/** Contains the logic for Flicker as a Service. */
class FlickerServiceImpl(private val flickerConfig: FlickerConfig) : FlickerService {
    override fun detectScenarios(reader: Reader): Collection<ScenarioInstance> {
        validateTrace(reader)

        return withTracing("FlickerService#detectScenarios") {
            flickerConfig.getEntries().flatMap { configEntry ->
                configEntry.extractor.extract(reader).map { traceSlice ->
                    ScenarioInstanceImpl.fromSlice(traceSlice, reader, configEntry)
                }
            }
        }
    }

    private fun validateTrace(reader: Reader) {
        val layersTrace = reader.readLayersTrace()
                ?: error("Missing layers trace: Cannot run Flicker Service without this trace!")
        assert(layersTrace.entries.isNotEmpty()) {
            "Layers trace is empty! Cannot run Flicker Service on this trace! " +
                    "This is likely a bug! It shouldn't ever happen."
        }
        assert(layersTrace.entries.size > 1) {
            "Layers trace must have at least only one entry. This is likely due to nothing " +
                    "happening on the device while the trace is being collected. Please double " +
                    "check that something is happening while the trace is being collected."
        }
    }
}
