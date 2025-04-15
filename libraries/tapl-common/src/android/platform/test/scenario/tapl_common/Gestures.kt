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

import android.os.SystemClock.sleep
import android.platform.uiautomatorhelpers.BetterSwipe
import android.platform.uiautomatorhelpers.BetterSwipe.Swipe
import android.platform.uiautomatorhelpers.LongPressTimeoutExtender
import android.platform.uiautomatorhelpers.WaitUtils.ensureThat
import android.provider.Settings
import android.view.Display.DEFAULT_DISPLAY
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiObject2
import java.time.Duration

/**
 * A collection of gestures for UI objects that implements flake-proof patterns and adds
 * diagnostics. Don't use these gestures directly from the test, this class should be used only by
 * TAPL.
 */
object Gestures {
    private val WAIT_TIME = Duration.ofSeconds(10)

    private fun waitForObjectCondition(
        objectName: String,
        conditionName: String,
        condition: () -> Boolean,
    ) {
        ensureThat(
            condition = condition,
            description = "UI object '$objectName' becomes $conditionName",
            timeout = WAIT_TIME,
        )
    }

    internal fun waitForObjectEnabled(uiObject: UiObject2, objectName: String) {
        waitForObjectCondition(objectName, "enabled") { uiObject.isEnabled() }
    }

    private fun waitForObjectClickable(uiObject: UiObject2, waitReason: String) {
        waitForObjectCondition(waitReason, "clickable") { uiObject.isClickable() }
    }

    private fun waitForObjectLongClickable(uiObject: UiObject2, waitReason: String) {
        waitForObjectCondition(waitReason, "long-clickable") { uiObject.isLongClickable() }
    }

    internal fun waitForObjectScrollable(uiObject: UiObject2, waitReason: String) {
        waitForObjectCondition(waitReason, "scrollable") { uiObject.isScrollable() }
    }

    /**
     * Wait for the object to become clickable and enabled, then clicks the object.
     *
     * @param [uiObject] The object to click
     * @param [objectName] Name of the object for diags
     */
    @JvmStatic
    fun click(uiObject: UiObject2, objectName: String) {
        try {
            waitForObjectEnabled(uiObject, objectName)
            waitForObjectClickable(uiObject, objectName)
            clickNow(uiObject)
        } catch (e: StaleObjectException) {
            throw AssertionError(
                "UI object '$objectName' has disappeared from the screen during the click gesture.",
                e,
            )
        }
    }

    /**
     * Wait for the object to become clickable and enabled, then performs a short click on the
     * object, but with extended long press duration using
     * [LongPressTimeoutExtender.executeWithExtendedLongPressTimeout] to avoid it being mistaken
     * with a long press.
     *
     * @param [uiObject] The object to click
     * @param [objectName] Name of the object for diags
     */
    @JvmStatic
    fun shortClick(uiObject: UiObject2, objectName: String) {
        LongPressTimeoutExtender.executeWithExtendedLongPressTimeout { click(uiObject, objectName) }
    }

    /**
     * Waits for the object to become long-clickable and enabled, then presses the object down.
     *
     * @param [uiObject] The object to click
     * @param [objectName] Name of the object for diags
     * @param [whileHoldingFn] lambda to call between press and release.
     */
    @JvmStatic
    fun longClickDownUp(
        uiObject: UiObject2,
        objectName: String,
        displayId: Int = DEFAULT_DISPLAY,
        whileHoldingFn: (Swipe.() -> Unit),
    ) {
        try {
            waitForObjectEnabled(uiObject, objectName)
            waitForObjectLongClickable(uiObject, objectName)
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            BetterSwipe.swipe(uiObject.visibleCenter, displayId) {

                // press for twice the long press timeout, just to make sure.
                val longPressMsec =
                    Settings.Secure.getInt(context.contentResolver, "long_press_timeout")
                sleep(longPressMsec.toLong() * 2)
                whileHoldingFn()
            }
        } catch (e: StaleObjectException) {
            throw AssertionError(
                "UI object '$objectName' has disappeared from " +
                    "the screen during the long click gesture.",
                e,
            )
        }
    }

    /**
     * Click on the ui object right away without waiting for animation.
     *
     * [UiObject2.click] would wait for all animations finished before clicking. Not waiting for
     * animations because in some scenarios there is a playing animations when the click is
     * attempted.
     */
    private fun clickNow(uiObject: UiObject2) {
        BetterSwipe.swipe(uiObject.visibleCenter, uiObject.displayId)
    }
}
