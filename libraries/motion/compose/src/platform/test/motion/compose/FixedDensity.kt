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

package platform.test.motion.compose

import android.content.res.Configuration
import android.util.DisplayMetrics
import android.view.ContextThemeWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import kotlin.math.floor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestScope
import org.junit.rules.RuleChain
import platform.test.motion.MotionTestRule
import platform.test.screenshot.GoldenPathManager

/**
 * Default density for tests.
 *
 * Using a uneven number to reveal issues with rounding.
 */
val DefaultFixedDensity = Density(2.5f)

/**
 * Convenience to create a [MotionTestRule], including the required setup.
 *
 * NOTE: The [density] applies to the complete content, EXCEPT the root node returned by the
 * `isRoot()` [SemanticMatcher]. This can produce unexpected results when dispatching gestures on
 * the root node. To work around this, dispatch the gestures on a node owned by the composable under
 * test.
 *
 * This is an experimental replacement for the `createComposeMotionTestRule`, and avoids setting the
 * device density to achieve the same results.
 *
 * This rule sets the [LocalDensity] to [density], allowing for a deterministic calculation of `dp
 * -> px -> dp`.
 *
 * In addition to the [MotionTestRule], this function also creates a [ComposeContentTestRule], which
 * is run as part of the [MotionTestRule].
 */
@OptIn(ExperimentalTestApi::class)
fun createFixedDensityComposeMotionTestRule(
    goldenPathManager: GoldenPathManager,
    testScope: TestScope = TestScope(),
    density: Density = DefaultFixedDensity,
): MotionTestRule<ComposeToolkit> {
    val composeRule = createComposeRule(testScope.coroutineContext + Dispatchers.Main)

    return MotionTestRule(
        ComposeToolkit(composeRule, testScope, density),
        goldenPathManager,
        extraRules = RuleChain.outerRule(composeRule),
    )
}

@Composable
internal fun FixedDensity(density: Density, content: @Composable () -> Unit) {
    val previousConfiguration = LocalConfiguration.current
    val configuration =
        remember(previousConfiguration, density) {
            Configuration().apply {
                updateFrom(previousConfiguration)
                densityDpi = floor(density.density * DisplayMetrics.DENSITY_DEFAULT).toInt()
                fontScale = density.fontScale
            }
        }
    val previousContext = LocalContext.current
    val context =
        remember(previousContext, configuration) {
            ContextThemeWrapper(previousContext, 0).apply {
                applyOverrideConfiguration(configuration)
            }
        }
    CompositionLocalProvider(
        LocalContext provides context,
        LocalDensity provides density,
        LocalConfiguration provides configuration,
        content = content,
    )
}
