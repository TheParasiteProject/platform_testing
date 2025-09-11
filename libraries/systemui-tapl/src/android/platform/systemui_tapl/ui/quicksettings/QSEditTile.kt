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

package android.platform.systemui_tapl.ui.quicksettings

import android.graphics.Point
import android.platform.systemui_tapl.utils.DeviceUtils.sysuiResSelector
import android.platform.uiautomatorhelpers.BetterSwipe
import android.platform.uiautomatorhelpers.DeviceHelpers.assertInvisible
import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisible
import android.platform.uiautomatorhelpers.DeviceHelpers.doubleTapAt
import android.platform.uiautomatorhelpers.DeviceHelpers.uiDevice
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForObj
import android.view.animation.DecelerateInterpolator
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import kotlinx.coroutines.android.awaitFrame

class QSEditTile(val tile: QSEditTileSpec, val format: Format, val selected: Boolean = false) {

    init {
        check(!(format == Format.AVAILABLE && selected)) {
            "Selection not supported for available tiles"
        }
    }

    /** Represents the different formats for an edit tile. */
    enum class Format {
        SMALL,
        LARGE,
        AVAILABLE,
    }

    /** The tile's [BySelector], which depends on its [Format]. */
    val selector: BySelector
        get() =
            when (format) {
                Format.SMALL -> asSmallTile()
                Format.LARGE -> asLargeTile()
                Format.AVAILABLE -> asAvailableTile()
            }.let { if (selected) asSelectedTile { it } else it }

    /** Asserts that the tile is currently visible. */
    fun assertVisible() {
        selector.assertVisible()
    }

    /** Asserts that the tile is currently invisible. */
    fun assertInvisible() {
        selector.assertInvisible()
    }

    /**
     * Clicks on the tile.
     *
     * This clicks off center to avoid tapping on the badge accidentally.
     */
    fun click() {
        waitForObj(selector).apply { click(getClickTarget()) }
    }

    /**
     * Double clicks on the tile.
     *
     * This clicks off center to avoid tapping on the badge accidentally. This isn't supported for
     * [Format.AVAILABLE] tiles.
     */
    fun doubleClick() {
        check(format != Format.AVAILABLE) { "Double click not supported for available tiles" }

        val target = getClickTarget()
        uiDevice.doubleTapAt(target.x, target.y)
    }

    /** Clicks on the tile's badge, whether it's selected or not. */
    fun tapOnBadge() {
        val uiObject = waitForObj(selector)
        val point =
            if (selected) {
                Point(uiObject.visibleBounds.right, uiObject.visibleBounds.centerY())
            } else {
                Point(uiObject.visibleBounds.right, uiObject.visibleBounds.top)
            }
        uiObject.click(point)
    }

    /**
     * Drags the resizing handle to resize the tile.
     *
     * The direction of the drag depends on the tile's size. This action isn't supported for
     * [Format.AVAILABLE] tiles.
     */
    fun resizeDrag() {
        check(format != Format.AVAILABLE) { "Resizing not supported for available tiles" }

        val uiObject = waitForObj(selector)
        val centerRight = Point(uiObject.visibleBounds.right, uiObject.visibleBounds.centerY())
        val end =
            if (format == Format.LARGE) {
                uiObject.visibleCenter
            } else {
                Point(centerRight.x + uiObject.visibleBounds.width(), centerRight.y)
            }
        BetterSwipe.swipe(start = centerRight, end = end, interpolator = DecelerateInterpolator())
    }

    /**
     * Returns a safe [Point] to use for clicking on the tile.
     *
     * We're avoiding clicking on the center in case the upper right badge overlaps
     */
    fun getClickTarget(): Point {
        with(waitForObj(selector)) {
            return Point(visibleBounds.left, visibleCenter.y)
        }
    }

    private fun asLargeTile(): BySelector {
        return sysuiResSelector(LARGE_TILE_TAG).hasDescendant(By.descContains(tile.desc))
    }

    private fun asSmallTile(): BySelector {
        return sysuiResSelector(SMALL_TILE_TAG).hasDescendant(By.descContains(tile.desc))
    }

    private fun asSelectedTile(selector: () -> BySelector): BySelector {
        return selector().hasDescendant(By.descContains(RESIZING_CONTENT_DESCRIPTION))
    }

    private fun asAvailableTile(): BySelector {
        return sysuiResSelector(AVAILABLE_TILE_TAG).hasDescendant(By.descContains(tile.desc))
    }

    private companion object {
        const val LARGE_TILE_TAG = "qs_tile_large"
        const val SMALL_TILE_TAG = "qs_tile_small"
        const val AVAILABLE_TILE_TAG = "AvailableTileTestTag"
        const val RESIZING_CONTENT_DESCRIPTION = "toggle the tile's size"
    }
}
