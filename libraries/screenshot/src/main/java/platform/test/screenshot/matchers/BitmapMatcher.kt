/*
 * Copyright 2022 The Android Open Source Project
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

package platform.test.screenshot.matchers

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import kotlin.collections.List
import platform.test.screenshot.proto.ScreenshotResultProto
import platform.test.screenshot.proto.ScreenshotResultProto.DiffResult.ComparisonStatistics

/** The abstract class to implement to provide custom bitmap matchers. */
abstract class BitmapMatcher {
    /**
     * Compares the given bitmaps and returns result of the operation.
     *
     * The images need to have same size.
     *
     * @param expected The reference / golden image.
     * @param given The image taken during the test.
     * @param width Width of both of the images.
     * @param height Height of both of the images.
     * @param regions An optional array of interesting regions for screenshot diff.
     */
    abstract fun compareBitmaps(
        expected: IntArray,
        given: IntArray,
        width: Int,
        height: Int,
        regions: List<Rect> = emptyList()
    ): MatchResult

    /**
     * Compares the given bitmaps with different dimensions and returns result of the operation.
     *
     * The two different-sized images are aligned at (0, 0), and the underlying matcher is the same
     * as [PixelPerfectMatcher].
     *
     * @param expected The reference / golden image.
     * @param given The image taken during the test.
     * @param expectedWidth Width of the expected image.
     * @param expectedHeight Height of the expected image.
     * @param actualWidth Width of the actual image.
     * @param actualHeight Height of the actual image.
     */
    open fun compareBitmaps(
        expected: IntArray,
        given: IntArray,
        expectedWidth: Int,
        expectedHeight: Int,
        actualWidth: Int,
        actualHeight: Int
    ): MatchResult {
        val width = if (expectedWidth < actualWidth) expectedWidth else actualWidth
        val height = if (expectedHeight < actualHeight) expectedHeight else actualHeight
        var different = 0
        var same = 0

        val diffArray = lazy { IntArray(width * height) { Color.TRANSPARENT } }

        for (i in 0..<height) {
            for (j in 0..<width) {
                val actualIndex = i * actualWidth + j
                val expectedIndex = i * expectedWidth + j
                if (expected[expectedIndex] == given[actualIndex]) {
                    same++
                } else {
                    different++
                    diffArray.value[i * width + j] = Color.MAGENTA
                }
            }
        }

        val stats =
            ScreenshotResultProto.DiffResult.ComparisonStatistics.newBuilder()
                .setNumberPixelsCompared(width * height)
                .setNumberPixelsIdentical(same)
                .setNumberPixelsDifferent(different)
                .build()

        return if (different > 0) {
            val diff = Bitmap.createBitmap(diffArray.value, width, height, Bitmap.Config.ARGB_8888)
            MatchResult(matches = false, diff = diff, comparisonStatistics = stats)
        } else {
            MatchResult(matches = true, diff = null, comparisonStatistics = stats)
        }
    }

    @SuppressLint("CheckResult")
    protected fun getFilter(width: Int, height: Int, regions: List<Rect>): BooleanArray {
        return if (regions.isEmpty()) {
            BooleanArray(width * height) { true }
        } else {
            val regionsSanitised = regions.map { Rect(it).apply { intersect(0, 0, width, height) } }
            BooleanArray(width * height).also { filterArr ->
                regionsSanitised.forEach { region ->
                    val startX = region.left.coerceIn(0, width - 1)
                    val endX = region.right.coerceIn(0, width - 1)
                    val startY = region.top.coerceIn(0, height - 1)
                    val endY = region.bottom.coerceIn(0, height - 1)
                    for (x in startX..endX) {
                        for (y in startY..endY) {
                            filterArr[y * width + x] = true
                        }
                    }
                }
            }
        }
    }
}

/**
 * Result of the matching performed by [BitmapMatcher].
 *
 * @param matches True if bitmaps match.
 * @param comparisonStatistics Matching statistics provided by this matcher that performed the
 *   comparison.
 * @param diff Diff bitmap that highlights the differences between the images. Can be null if match
 *   was found.
 */
class MatchResult(
    val matches: Boolean,
    val comparisonStatistics: ComparisonStatistics,
    val diff: Bitmap?
)
