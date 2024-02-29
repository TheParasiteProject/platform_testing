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

package android.tools.flicker.config.ime

import android.tools.flicker.AssertionInvocationGroup
import android.tools.flicker.assertors.assertions.LayerBecomesInvisible
import android.tools.flicker.assertors.assertions.NonAppWindowBecomesInvisible
import android.tools.flicker.config.AssertionTemplates
import android.tools.flicker.config.FlickerConfigEntry
import android.tools.flicker.config.ScenarioId
import android.tools.flicker.config.ime.Components.IME
import android.tools.flicker.extractors.TaggedCujTransitionMatcher
import android.tools.flicker.extractors.TaggedScenarioExtractorBuilder
import android.tools.traces.events.CujType

val ImeInsetsHideAnimation =
    FlickerConfigEntry(
        enabled = true,
        scenarioId = ScenarioId("IME_INSETS_HIDE_ANIMATION"),
        assertions =
            AssertionTemplates.COMMON_ASSERTIONS +
                mapOf(
                    NonAppWindowBecomesInvisible(IME) to AssertionInvocationGroup.BLOCKING,
                    LayerBecomesInvisible(IME) to AssertionInvocationGroup.BLOCKING
                ),
        extractor =
            TaggedScenarioExtractorBuilder()
                .setTargetTag(CujType.CUJ_IME_INSETS_HIDE_ANIMATION)
                .setTransitionMatcher(
                    TaggedCujTransitionMatcher(associatedTransitionRequired = false)
                )
                .build()
    )