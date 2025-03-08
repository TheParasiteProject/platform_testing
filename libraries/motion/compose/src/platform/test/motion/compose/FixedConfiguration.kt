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
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.floor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestScope
import org.junit.rules.RuleChain
import platform.test.motion.MotionTestRule
import platform.test.screenshot.GoldenPathManager

/**
 * @param density Sets the [LocalDensity], allowing for a deterministic calculation of `dp -> px ->
 *   dp`.
 * @param touchSlop Sets the [ViewConfiguration.touchSlop], allowing for a deterministic gesture
 *   start.
 */
data class FixedConfiguration(
    val density: Density = DefaultDensity,
    val touchSlop: Dp = DefaultTouchSlop,
) {

    companion object {
        /**
         * Default density for tests.
         *
         * Using a uneven number to reveal issues with rounding.
         */
        val DefaultDensity = Density(2.5f)

        /**
         * Touch slop for gestures.
         *
         * Using the cuttlefish default value.
         */
        val DefaultTouchSlop = 12.dp
    }
}

/**
 * Convenience to create a [MotionTestRule], including the required setup.
 *
 * NOTE: The [configuration] applies to the complete content, EXCEPT the root node returned by the
 * `isRoot()` [SemanticMatcher]. This can produce unexpected results when dispatching gestures on
 * the root node. To work around this, dispatch the gestures on a node owned by the composable under
 * test.
 *
 * In addition to the [MotionTestRule], this function also creates a [ComposeContentTestRule], which
 * is run as part of the [MotionTestRule].
 */
@OptIn(ExperimentalTestApi::class)
fun createFixedConfigurationComposeMotionTestRule(
    goldenPathManager: GoldenPathManager,
    testScope: TestScope = TestScope(),
    configuration: FixedConfiguration = FixedConfiguration(),
): MotionTestRule<ComposeToolkit> {
    val composeRule = createComposeRule(testScope.coroutineContext + Dispatchers.Main)

    return MotionTestRule(
        ComposeToolkit(composeRule, testScope, configuration),
        goldenPathManager,
        extraRules = RuleChain.outerRule(composeRule),
    )
}

@Composable
internal fun FixedConfigurationProvider(
    fixedConfiguration: FixedConfiguration,
    content: @Composable () -> Unit,
) {
    val baseLocalConfiguration = LocalConfiguration.current
    val configuration =
        remember(baseLocalConfiguration, fixedConfiguration) {
            Configuration().apply {
                updateFrom(baseLocalConfiguration)
                densityDpi =
                    floor(fixedConfiguration.density.density * DisplayMetrics.DENSITY_DEFAULT)
                        .toInt()
                fontScale = fixedConfiguration.density.fontScale
            }
        }
    val previousContext = LocalContext.current
    val context =
        remember(previousContext, configuration) {
            ContextThemeWrapper(previousContext, 0).apply {
                applyOverrideConfiguration(configuration)
            }
        }

    val baseViewConfiguration = LocalViewConfiguration.current
    val customViewConfiguration =
        remember(baseViewConfiguration, fixedConfiguration) {
            object : ViewConfiguration by baseViewConfiguration {
                override val touchSlop: Float =
                    with(fixedConfiguration.density) { fixedConfiguration.touchSlop.toPx() }
            }
        }

    CompositionLocalProvider(
        LocalContext provides context,
        LocalDensity provides fixedConfiguration.density,
        LocalConfiguration provides configuration,
        LocalViewConfiguration provides customViewConfiguration,
        content = content,
    )
}
