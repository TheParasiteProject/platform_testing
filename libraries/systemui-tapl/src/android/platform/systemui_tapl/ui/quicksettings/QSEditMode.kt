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
import android.platform.uiautomatorhelpers.DeviceHelpers
import android.platform.uiautomatorhelpers.DeviceHelpers.assertInvisible
import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisible
import android.platform.uiautomatorhelpers.DeviceHelpers.click
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForObj
import android.view.Display.DEFAULT_DISPLAY
import android.view.animation.DecelerateInterpolator
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiObject2
import java.time.Duration
import org.junit.Assert.assertNotNull

/** System UI test automation object representing QS edit mode. */
class QSEditMode(val displayId: Int = DEFAULT_DISPLAY) {
    init {
        EDIT_MODE_TITLE_SELECTOR.assertVisible()
    }

    /** Closes edit mode and returns to QS. */
    fun close() {
        BACK_ARROW_SELECTOR.click()
        CURRENT_TILES_GRID_SELECTOR.assertInvisible { "QS edit mode is visible" }
    }

    /**
     * Scrolls edit mode until [tile] is visible.
     *
     * This will throw an [AssertionError] if the tile isn't found.
     *
     * @param tile the [QSEditTile] to find when scrolling
     * @param scrollingUp whether or not we should scroll up to find the tile. The direction refers
     *   to the gesture, a drag UP scrolls to reveal tiles further down in the list.
     */
    fun scrollToTile(tile: QSEditTile, scrollingUp: Boolean) {
        if (DeviceHelpers.waitForNullableObj(tile.selector) == null) {
            val gridUiObject = waitForObj(EDIT_MODE_ROOT_SELECTOR)
            val selector = tile.selector
            gridUiObject.scrollUntilFound(selector, scrollingUp).let {
                assertNotNull("Could not find tile $selector", it)
            }
        }
    }

    /**
     * Long presses [source] and drags to [target]
     *
     * This will throw an error if [source] or [target] isn't found.
     */
    fun longPressAndDrag(source: QSEditTile, target: QSEditTile) {
        val targetUiObject = waitForObj(target.selector)
        BetterSwipe.swipe(source.getClickTarget()) {
            // Pause before the swipe to simulate a long press
            pause()

            // Drag to the target's center
            to(
                end = targetUiObject.visibleCenter,
                duration = Duration.ofMillis(1000),
                interpolator = DecelerateInterpolator(),
            )

            // Pause at the end of the gesture before releasing to make sure the list is updated
            pause()
        }
    }

    private fun UiObject2.scrollUntilFound(selector: BySelector, scrollingUp: Boolean): UiObject2? {
        val offset = visibleBounds.height() * .25f * if (scrollingUp) -1 else 1
        (0 until 10).forEach { _ ->
            val f = findObject(selector)
            if (f != null) return f

            // Swipe from the center of the screen to avoid closing the shade accidentally
            BetterSwipe.swipe(
                visibleCenter,
                Point(visibleCenter.x, visibleCenter.y + offset.toInt()),
            )
        }
        return null
    }

    private companion object {
        // https://hsv.googleplex.com/4773243557773312?node=37
        private val EDIT_MODE_ROOT_SELECTOR = sysuiResSelector("EditModeRoot")

        // https://hsv.googleplex.com/4784770226585600?node=20
        private val CURRENT_TILES_GRID_SELECTOR = sysuiResSelector("CurrentTilesGrid")

        // https://hsv.googleplex.com/4784770226585600?node=97
        private val EDIT_MODE_TITLE_SELECTOR = By.text("Edit tiles")

        // https://hsv.googleplex.com/4784770226585600?node=95
        private val BACK_ARROW_SELECTOR = By.desc("Navigate up")
    }
}
