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

package android.tools.flicker.assertors.assertions

import android.graphics.Region
import android.tools.flicker.ScenarioInstance
import android.tools.flicker.assertions.FlickerTest
import android.tools.flicker.assertors.AssertionTemplate
import android.tools.flicker.assertors.ComponentTemplate
import android.tools.flicker.subject.layers.LayersTraceSubject
import android.tools.traces.component.ComponentNameMatcher
import android.tools.traces.component.IComponentMatcher
import android.tools.traces.surfaceflinger.Layer

/**
 * Checks that:
 * - The background layer becomes visible during the scenario and becomes invisible again by its end
 * - The background layer has a color and covers the same region the closing layers are covering at
 *   the start of the trace
 */
class BackgroundShowsInTransition(val startingChanges: ComponentTemplate) : AssertionTemplate() {
    val animationBackgroundMatcher = ComponentNameMatcher("", "animation-background")

    /** {@inheritDoc} */
    override fun doEvaluate(scenarioInstance: ScenarioInstance, flicker: FlickerTest) {
        val firstTraceEntry = scenarioInstance.reader.readLayersTrace()?.entries?.first()
        val visibleLayers =
            startingChanges
                .get(scenarioInstance)
                .filterLayers(
                    firstTraceEntry?.visibleLayers
                        ?: error("No visible layers have been found in $startingChanges")
                )

        flicker.assertLayers {
            isInvisible(animationBackgroundMatcher)
                .then()
                .isVisible(animationBackgroundMatcher)
                .coversAtLeast(animationBackgroundMatcher, transitionRegion(visibleLayers))
                .hasColor(animationBackgroundMatcher)
                .then()
                .isInvisible(animationBackgroundMatcher)
        }
    }

    private fun LayersTraceSubject.coversAtLeast(
        component: IComponentMatcher,
        expectedArea: Region,
    ): LayersTraceSubject =
        invoke("$component coversAtLeast $expectedArea") {
            it.visibleRegion(component).coversAtLeast(expectedArea)
        }

    private fun transitionRegion(visibleLayers: Collection<Layer>): Region {
        return visibleLayers.fold(Region()) { region, layer ->
            region.apply { union(layer.visibleRegion.bounds) }
        }
    }
}
