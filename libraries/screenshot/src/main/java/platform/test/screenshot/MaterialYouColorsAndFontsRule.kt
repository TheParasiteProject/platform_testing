/*
 * Copyright 2024 The Android Open Source Project
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

package platform.test.screenshot

import android.content.Context
import android.util.SparseIntArray
import android.widget.RemoteViews
import androidx.annotation.VisibleForTesting
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A rule to overload the target context system colors by [colors], and to apply desired fonts.
 *
 * This is especially useful to apply the colors before you start an activity using an
 * [ActivityScenarioRule] or any other rule, given that the colors must be [applied]
 * [MaterialYouColors.apply] *before* doing any resource resolution.
 */
class MaterialYouColorsAndFontsRule(private val colors: MaterialYouColors = MaterialYouColors.GreenBlue) :
    TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                createAndApplyResourcesProvider(
                    InstrumentationRegistry.getInstrumentation().targetContext, colors.colors)
                base.evaluate()
            }
        }
    }
}

