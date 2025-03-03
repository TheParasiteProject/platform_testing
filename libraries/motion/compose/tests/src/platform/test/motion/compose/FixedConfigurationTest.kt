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

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.base.Function
import com.google.common.truth.Correspondence.transforming
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import platform.test.motion.golden.DataPoint
import platform.test.motion.golden.FeatureCapture
import platform.test.motion.golden.ValueDataPoint
import platform.test.motion.golden.asDataPoint
import platform.test.motion.testing.createGoldenPathManager

@RunWith(AndroidJUnit4::class)
class FixedConfigurationTest {
    private val pathManager =
        createGoldenPathManager("platform_testing/libraries/motion/compose/tests/goldens")

    @get:Rule val motionRule = createFixedConfigurationComposeMotionTestRule(pathManager)

    private val composeRule: ComposeContentTestRule
        get() = motionRule.toolkit.composeContentTestRule

    @Test
    fun defaultFixedDensity_usesExpectedRatio() {
        var actualPixels = Float.NaN

        composeRule.setContent {
            FixedConfigurationProvider(FixedConfiguration()) {
                with(LocalDensity.current) { actualPixels = 10.dp.toPx() }
            }
        }

        assertThat(actualPixels).isEqualTo(25)
    }

    @Test
    fun fixedDensity_canSetAnyRatio() {
        var actualPixels = Float.NaN

        composeRule.setContent {
            FixedConfigurationProvider(FixedConfiguration(density = Density(10f))) {
                with(LocalDensity.current) { actualPixels = 10.dp.toPx() }
            }
        }

        assertThat(actualPixels).isEqualTo(100f)
    }

    @Test
    fun fixedDensity_reflectedInLocalConfiguration() {
        var densityDpi = -1

        composeRule.setContent {
            FixedConfigurationProvider(FixedConfiguration(density = Density(2f))) {
                densityDpi = LocalConfiguration.current.densityDpi
            }
        }

        // Density of 1 is 160dpi, thus a density of 2 is 320dpi
        assertThat(densityDpi).isEqualTo(320)
    }

    @Test
    fun recordMotion_usedFixedDensity() =
        motionRule.runTest {
            val motion =
                recordMotion(
                    content = {
                        Box(modifier = Modifier.testTag("foo").size(width = 10.dp, height = 20.dp))
                    },
                    ComposeRecordingSpec(
                        MotionControl { awaitFrames(1) },
                        recordBefore = false,
                        recordAfter = false,
                    ) {
                        feature(hasTestTag("foo"), ComposeFeatureCaptures.dpSize, name = "dp_size")
                        feature(hasTestTag("foo"), ComposeFeatureCaptures.size, name = "px_size")
                    },
                )

            assertThat(motion.timeSeries.features["dp_size"]?.dataPoints)
                .containsExactly(DpSize(10.dp, 20.dp).asDataPoint())

            assertThat(motion.timeSeries.features["px_size"]?.dataPoints)
                .containsExactly(IntSize(25, 50).asDataPoint())
        }

    @Test
    fun fixedTouchSlop_reflectedInViewConfiguration_usesSuppliedDensity() {
        var touchSlopPx = -1f

        composeRule.setContent {
            FixedConfigurationProvider(
                FixedConfiguration(touchSlop = 10.dp, density = Density(4f))
            ) {
                touchSlopPx = LocalViewConfiguration.current.touchSlop
            }
        }

        // Density of 1 is 160dpi, thus a density of 2 is 320dpi
        assertThat(touchSlopPx).isEqualTo(40)
    }

    @Test
    fun recordMotion_usesFixedTouchSlop() =
        motionRule.runTest {
            var yDragDistance by mutableStateOf(0f)

            val dragStartThreshold = 30f // Defaults: 12.dp touchslop at a 2.5 density

            val motion =
                recordMotion(
                    content = {
                        Box(
                            modifier =
                                Modifier.pointerInput(Unit) {
                                        detectDragGestures(
                                            onDragStart = { yDragDistance = it.y }
                                        ) { _, dragAmount ->
                                            yDragDistance += dragAmount.y
                                        }
                                    }
                                    .testTag("foo")
                                    .size(width = 10.dp, height = 10.dp)
                        )
                    },
                    ComposeRecordingSpec(
                        MotionControl {
                            performTouchInputAsync(onNode(hasTestTag("foo"))) {
                                swipeDown(startY = 0f, endY = 40f, durationMillis = 128)
                            }
                        },
                        recordBefore = false,
                        recordAfter = false,
                    ) {
                        feature(FeatureCapture("yDragDistance") { yDragDistance.asDataPoint() })
                    },
                )

            val dataPoints = checkNotNull(motion.timeSeries.features["yDragDistance"]?.dataPoints)

            // recorded swipe is 8 frames long (128ms/16ms frames)
            assertThat(dataPoints.size).isEqualTo(8)

            // drag distance threshold is at 3/4 of the drag distance, swipe is 8 frames long, thus
            // 6 elements must be 0
            val firstNonZeroIndex = dataPoints.indexOfFirst { it != 0f.asDataPoint() }
            assertThat(firstNonZeroIndex).isEqualTo(6)

            // Of the remaining elements, they all must be at least dragStartThreshold.
            assertThat(dataPoints.drop(firstNonZeroIndex))
                .comparingElementsUsing(
                    transforming(
                        Function<DataPoint<Float>, Boolean> {
                            ((it as? ValueDataPoint<Float>)?.value ?: 0f) >= dragStartThreshold
                        },
                        "is above touch slop threshold",
                    )
                )
                .doesNotContain(false)
        }
}
