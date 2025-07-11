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

import android.tools.flicker.ScenarioInstance
import android.tools.flicker.assertions.FlickerChecker
import android.tools.flicker.assertors.ComponentTemplate

/**
 * Checks if the app layer expands in its visible region.
 *
 * Note: it is technically possible that no size change occurs between two traces, so we make a more
 * generic assertion that the layer area does not get smaller.
 */
class LayerExpands(private val componentTemplate: ComponentTemplate) :
    AssertionTemplateWithComponent(componentTemplate) {
    /** {@inheritDoc} */
    override fun doEvaluate(scenarioInstance: ScenarioInstance, flicker: FlickerChecker) {
        val matcher = componentTemplate.get(scenarioInstance)
        flicker.assertLayers {
            val layerList = this.layers { matcher.layerMatchesAnyOf(it) && it.isVisible }
            layerList.zipWithNext { previous, current ->
                current.visibleRegion.notSmallerThan(previous.visibleRegion.region)
            }
        }
    }
}
