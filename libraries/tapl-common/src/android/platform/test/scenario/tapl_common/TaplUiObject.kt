/*
 * Copyright (C) 2022 The Android Open Source Project
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
package android.platform.test.scenario.tapl_common

import android.graphics.Point
import android.graphics.Rect
import android.platform.uiautomator_helpers.BetterSwipe
import android.platform.uiautomator_helpers.PRECISE_GESTURE_INTERPOLATOR
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until

/**
 * Ui object with diagnostic metadata and flake-free gestures.
 * @param [uiObject] UI Automator object
 * @param [name] Name of the object for diags
 */
class TaplUiObject constructor(val uiObject: UiObject2, private val name: String) {
    // Margins used for gestures (avoids touching too close to the object's edge).
    private var mMarginLeft = 5
    private var mMarginTop = 5
    private var mMarginRight = 5
    private var mMarginBottom = 5

    /** Sets the margins used for gestures in pixels. */
    fun setGestureMargin(margin: Int) {
        setGestureMargins(margin, margin, margin, margin)
    }

    /** Sets the margins used for gestures in pixels. */
    fun setGestureMargins(left: Int, top: Int, right: Int, bottom: Int) {
        mMarginLeft = left
        mMarginTop = top
        mMarginRight = right
        mMarginBottom = bottom
    }

    /** Returns this object's visible bounds with the margins removed. */
    private fun getVisibleBoundsForGestures(): Rect {
        val ret: Rect = uiObject.visibleBounds
        ret.left = ret.left + mMarginLeft
        ret.top = ret.top + mMarginTop
        ret.right = ret.right - mMarginRight
        ret.bottom = ret.bottom - mMarginBottom
        return ret
    }

    /** Wait for the object to become clickable and enabled, then clicks the object. */
    fun click() {
        Gestures.click(uiObject, name)
    }

    /**
     * Waits for a child UI object with a given resource id. Fails if the object is not visible.
     *
     * @param [resourceId] Resource id.
     * @param [childObjectName] Name of the object for diags.
     * @return The found UI object.
     */
    fun waitForChildObject(childResourceId: String, childObjectName: String): TaplUiObject {
        val selector = By.res(uiObject.applicationPackage, childResourceId)
        val childObject =
            uiObject.wait(Until.findObject(selector), TaplUiDevice.WAIT_TIME.toMillis())
                ?: throw AssertionError(
                    "UI object '$childObjectName' is not found in '$name'; selector: $selector."
                )
        return TaplUiObject(childObject, childObjectName)
    }

    /**
     * Performs a horizontal or vertical swipe over an area.
     *
     * @param area The area to swipe over.
     * @param direction The direction in which to swipe.
     * @param percent The size of the swipe as a percentage of the total area.
     */
    private fun scrollRect(area: Rect, direction: Direction, percent: Float) {
        val start: Point
        val end: Point
        when (direction) {
            Direction.LEFT -> {
                start = Point(area.right, area.centerY())
                end = Point(area.right - (area.width() * percent).toInt(), area.centerY())
            }
            Direction.RIGHT -> {
                start = Point(area.left, area.centerY())
                end = Point(area.left + (area.width() * percent).toInt(), area.centerY())
            }
            Direction.UP -> {
                start = Point(area.centerX(), area.bottom)
                end = Point(area.centerX(), area.bottom - (area.height() * percent).toInt())
            }
            Direction.DOWN -> {
                start = Point(area.centerX(), area.top)
                end = Point(area.centerX(), area.top + (area.height() * percent).toInt())
            }
            else -> throw RuntimeException()
        }

        BetterSwipe.from(start).to(end, interpolator = PRECISE_GESTURE_INTERPOLATOR).release()
    }

    /**
     * Performs a scroll gesture on this object.
     *
     * @param direction The direction in which to scroll.
     * @param percent The distance to scroll as a percentage of this object's visible size.
     */
    fun scroll(direction: Direction, percent: Float) {
        require(percent >= 0.0f) { "Percent must be greater than 0.0f" }
        require(percent <= 1.0f) { "Percent must be less than 1.0f" }

        // To scroll, we swipe in the opposite direction
        val swipeDirection: Direction = Direction.reverse(direction)

        // Scroll by performing repeated swipes
        val bounds: Rect = getVisibleBoundsForGestures()
        val segment = Math.min(percent, 1.0f)
        scrollRect(bounds, swipeDirection, segment)
    }
}
