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
import android.hardware.display.DisplayTopology.TreeNode
import android.hardware.display.DisplayTopologyGraph
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
        val node1 =
            DisplayTopologyGraph.DisplayNode(
                1,
                DEFAULT_DENSITY,
                arrayOf(
                    DisplayTopologyGraph.AdjacentDisplay(
                        2,
                        TreeNode.POSITION_TOP,
                        DEFAULT_DISPLAY_OFFSET_DP,
                    )
                ),
            )
        val node2 =
            DisplayTopologyGraph.DisplayNode(
                2,
                DEFAULT_DENSITY,
                arrayOf(
                    DisplayTopologyGraph.AdjacentDisplay(
                        1,
                        TreeNode.POSITION_BOTTOM,
                        DEFAULT_DISPLAY_OFFSET_DP,
                    ),
                    DisplayTopologyGraph.AdjacentDisplay(
                        3,
                        TreeNode.POSITION_RIGHT,
                        DEFAULT_DISPLAY_OFFSET_DP,
                    ),
                    DisplayTopologyGraph.AdjacentDisplay(
                        4,
                        TreeNode.POSITION_LEFT,
                        DEFAULT_DISPLAY_OFFSET_DP,
                    ),
                ),
            )
        val node3 =
            DisplayTopologyGraph.DisplayNode(
                3,
                DEFAULT_DENSITY,
                arrayOf(
                    DisplayTopologyGraph.AdjacentDisplay(
                        2,
                        TreeNode.POSITION_LEFT,
                        DEFAULT_DISPLAY_OFFSET_DP,
                    )
                ),
            )
        val node4 =
            DisplayTopologyGraph.DisplayNode(
                4,
                DEFAULT_DENSITY,
                arrayOf(
                    DisplayTopologyGraph.AdjacentDisplay(
                        2,
                        TreeNode.POSITION_RIGHT,
                        DEFAULT_DISPLAY_OFFSET_DP,
                    )
                ),
            )
        val graph = DisplayTopologyGraph(1, arrayOf(node1, node2, node3, node4))

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
        val DEFAULT_DENSITY = 160
        val DEFAULT_DISPLAY_OFFSET_DP = 0f
    }
}
