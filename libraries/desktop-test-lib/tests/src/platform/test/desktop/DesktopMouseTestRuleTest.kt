/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package platform.test.desktop

import android.graphics.PointF
import android.graphics.RectF
import android.hardware.display.DisplayTopology
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import platform.test.desktop.DesktopMouseTestRule.AdjacentDisplay
import platform.test.desktop.DesktopMouseTestRule.AdjacentDisplay.Position

class DesktopMouseTestRuleTest {

    @Test
    fun testCalculateCrossingDetailsDpRight() {
        val b1 = RectF(100f, 100f, 200f, 200f)
        val b2 = RectF(200f, 150f, 300f, 250f)
        val position = Position.RIGHT

        val result = DesktopMouseTestRule.calculateCrossingDetailsDp(b1, b2, position)

        assertThat(result.targetPointDp).isEqualTo(PointF(200f, 175f))
        assertThat(result.toCrossDxDp).isEqualTo(DesktopMouseTestRule.MOUSE_CROSS_DISPLAY_OFFSET_DP)
        assertThat(result.toCrossDyDp).isEqualTo(0f)
    }

    @Test
    fun testCalculateCrossingDetailsDpLeft() {
        val b1 = RectF(100f, 100f, 200f, 200f)
        val b2 = RectF(0f, 120f, 100f, 180f)
        val position = Position.LEFT

        val result = DesktopMouseTestRule.calculateCrossingDetailsDp(b1, b2, position)

        assertThat(result.targetPointDp).isEqualTo(PointF(100f, 150f))
        assertThat(result.toCrossDxDp)
            .isEqualTo(-DesktopMouseTestRule.MOUSE_CROSS_DISPLAY_OFFSET_DP)
        assertThat(result.toCrossDyDp).isEqualTo(0f)
    }

    @Test
    fun testCalculateCrossingDetailsDpBottom() {
        val b1 = RectF(100f, 100f, 200f, 200f)
        val b2 = RectF(150f, 200f, 250f, 300f)
        val position = Position.BOTTOM

        val result = DesktopMouseTestRule.calculateCrossingDetailsDp(b1, b2, position)

        assertThat(result.targetPointDp).isEqualTo(PointF(175f, 200f))
        assertThat(result.toCrossDxDp).isEqualTo(0f)
        assertThat(result.toCrossDyDp).isEqualTo(DesktopMouseTestRule.MOUSE_CROSS_DISPLAY_OFFSET_DP)
    }

    @Test
    fun testCalculateCrossingDetailsDpTop() {
        val b1 = RectF(100f, 100f, 200f, 200f)
        val b2 = RectF(120f, 0f, 180f, 100f)
        val position = Position.TOP

        val result = DesktopMouseTestRule.calculateCrossingDetailsDp(b1, b2, position)

        assertThat(result.targetPointDp).isEqualTo(PointF(150f, 100f))
        assertThat(result.toCrossDxDp).isEqualTo(0f)
        assertThat(result.toCrossDyDp)
            .isEqualTo(-DesktopMouseTestRule.MOUSE_CROSS_DISPLAY_OFFSET_DP)
    }

    @Test
    fun testFindPath() {
        // DisplayTopology representation:
        // [4] - [2] - [3]
        //        |
        //       [1]
        val graph =
            DisplayTopology()
                .apply {
                    addDisplay(1, DEFAULT_SIZE_DP, DEFAULT_SIZE_DP, DEFAULT_DENSITY)
                    addDisplay(2, DEFAULT_SIZE_DP, DEFAULT_SIZE_DP, DEFAULT_DENSITY)
                    addDisplay(3, DEFAULT_SIZE_DP, DEFAULT_SIZE_DP, DEFAULT_DENSITY)
                    addDisplay(4, DEFAULT_SIZE_DP, DEFAULT_SIZE_DP, DEFAULT_DENSITY)
                    rearrange(
                        mapOf(
                            1 to PointF(0f, 0f),
                            2 to PointF(0f, -DEFAULT_SIZE_DP.toFloat()),
                            3 to
                                PointF(
                                    DEFAULT_SIZE_DP.toFloat(),
                                    -DEFAULT_SIZE_DP.toFloat() - OFFSET_TO_NOT_SHARE_CORNER_DP,
                                ),
                            4 to
                                PointF(
                                    -DEFAULT_SIZE_DP.toFloat(),
                                    -DEFAULT_SIZE_DP.toFloat() - OFFSET_TO_NOT_SHARE_CORNER_DP,
                                ),
                        )
                    )
                }
                .graph

        // Path from node 1
        assertThat(DesktopMouseTestRule.findPath(1, 1, graph)).isEmpty()
        assertThat(DesktopMouseTestRule.findPath(1, 2, graph))
            .containsExactly(AdjacentDisplay(2, Position.TOP))
        assertThat(DesktopMouseTestRule.findPath(1, 3, graph))
            .containsExactly(AdjacentDisplay(2, Position.TOP), AdjacentDisplay(3, Position.RIGHT))
            .inOrder()
        assertThat(DesktopMouseTestRule.findPath(1, 4, graph))
            .containsExactly(AdjacentDisplay(2, Position.TOP), AdjacentDisplay(4, Position.LEFT))
            .inOrder()

        // Path from node 2
        assertThat(DesktopMouseTestRule.findPath(2, 1, graph))
            .containsExactly(AdjacentDisplay(1, Position.BOTTOM))

        assertThat(DesktopMouseTestRule.findPath(2, 2, graph)).isEmpty()

        assertThat(DesktopMouseTestRule.findPath(2, 3, graph))
            .containsExactly(AdjacentDisplay(3, Position.RIGHT))

        assertThat(DesktopMouseTestRule.findPath(2, 4, graph))
            .containsExactly(AdjacentDisplay(4, Position.LEFT))

        // Path from node 3
        assertThat(DesktopMouseTestRule.findPath(3, 1, graph))
            .containsExactly(AdjacentDisplay(2, Position.LEFT), AdjacentDisplay(1, Position.BOTTOM))
            .inOrder()
        assertThat(DesktopMouseTestRule.findPath(3, 2, graph))
            .containsExactly(AdjacentDisplay(2, Position.LEFT))
        assertThat(DesktopMouseTestRule.findPath(3, 3, graph)).isEmpty()
        assertThat(DesktopMouseTestRule.findPath(3, 4, graph))
            .containsExactly(AdjacentDisplay(2, Position.LEFT), AdjacentDisplay(4, Position.LEFT))
            .inOrder()

        // Path from node 4
        assertThat(DesktopMouseTestRule.findPath(4, 1, graph))
            .containsExactly(
                AdjacentDisplay(2, Position.RIGHT),
                AdjacentDisplay(1, Position.BOTTOM),
            )
            .inOrder()
        assertThat(DesktopMouseTestRule.findPath(4, 2, graph))
            .containsExactly(AdjacentDisplay(2, Position.RIGHT))
        assertThat(DesktopMouseTestRule.findPath(4, 3, graph))
            .containsExactly(AdjacentDisplay(2, Position.RIGHT), AdjacentDisplay(3, Position.RIGHT))
            .inOrder()
        assertThat(DesktopMouseTestRule.findPath(4, 4, graph)).isEmpty()

        assertThrows(DesktopMouseTestRule.NoPathFoundException::class.java) {
            DesktopMouseTestRule.findPath(1, 5, graph)
        }
    }

    private companion object {
        val DEFAULT_SIZE_DP = 100
        val DEFAULT_DENSITY = 160
        val OFFSET_TO_NOT_SHARE_CORNER_DP = 10f
    }
}
