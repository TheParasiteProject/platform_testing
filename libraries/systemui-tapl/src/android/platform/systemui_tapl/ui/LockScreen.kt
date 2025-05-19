/*
 * Copyright (C) 2024 The Android Open Source Project
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

package android.platform.systemui_tapl.ui

import android.graphics.PointF
import android.platform.systemui_tapl.controller.LockscreenController
import android.platform.systemui_tapl.ui.CommunalHub.Companion.COMMUNAL_SELECTOR
import android.platform.systemui_tapl.utils.DeviceUtils.sysuiResSelector
import android.platform.uiautomatorhelpers.BetterSwipe
import android.platform.uiautomatorhelpers.DeviceHelpers.assertInvisible
import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisible
import android.platform.uiautomatorhelpers.DeviceHelpers.betterSwipe
import android.platform.uiautomatorhelpers.DeviceHelpers.uiDevice
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForObj
import android.platform.uiautomatorhelpers.FLING_GESTURE_INTERPOLATOR
import android.view.Display.DEFAULT_DISPLAY
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import com.android.launcher3.tapl.LauncherInstrumentation
import com.android.launcher3.tapl.Workspace
import com.android.systemui.Flags.sceneContainer
import com.google.common.truth.Truth.assertWithMessage

/** System UI test automation object representing the lock screen. */
class LockScreen internal constructor(val displayId: Int = DEFAULT_DISPLAY) {
    private val lockScreenIconSelector: BySelector =
        sysuiResSelector("device_entry_icon_view", displayId)

    private val lockScreenSelector: BySelector = lockScreenSelector(displayId)

    private val swipeableArea: BySelector =
        if (sceneContainer()) {
            sysuiResSelector("shared_notification_container", displayId)
        } else {
            sysuiResSelector("notification_panel", displayId)
        }

    init {
        lockScreenSelector.assertVisible { "Lockscreen is not visible on display $displayId" }
    }

    /**
     * Opens expanded quick settings shade via swipe on the portrait lock screen. When you swipe
     * from the top area of the screen instead of opening normal notification shade we open expanded
     * quick settings. This helper methods allows to perform this gesture. The difference from
     * opening normal shade gesture is that the startY position is higher. It is handled in
     * [NotificationPanelViewController.shouldQuickSettingsIntercept].
     */
    fun openQuickSettings(): QuickSettings {
        val device = uiDevice
        device.betterSwipe(
            startX = device.displayWidth / 2,
            startY = 0,
            endX = device.displayWidth / 2,
            endY = device.displayHeight * 2 / 3,
            interpolator = FLING_GESTURE_INTERPOLATOR,
        )
        return QuickSettings()
    }

    /**
     * Returns the lockscreen shade (the one visible without the need to pull it down). Fails if not
     * visible.
     */
    val notificationShade: LockscreenNotificationShade
        get() = LockscreenNotificationShade()

    /** Swipes up to the unlocked state. */
    fun swipeUpToUnlock(): Workspace {
        swipeUp()
        lockScreenSelector.assertInvisible { "Lockscreen still visible after swiping up." }
        assertWithMessage("Device is still locked after swiping up")
            .that(LockscreenController.get().isDeviceLocked)
            .isFalse()
        return Root.get().goHomeViaKeycode()
    }

    /** Uses home key to get to the unlocked state, skipping potentially flaky gesture. */
    fun unlockDirectly() {
        uiDevice.pressMenu()
        lockScreenSelector.assertInvisible { "Lockscreen still visible after swiping up." }
        assertWithMessage("Device is still locked after swiping up")
            .that(LockscreenController.get().isDeviceLocked)
            .isFalse()

        // Without this part, the launcher might not be ready when tests start to exercise it.
        uiDevice.pressHome()
        val instrumentation = LauncherInstrumentation()
        instrumentation.setExpectedRotation(uiDevice.displayRotation)
        instrumentation.waitForLauncherInitialized()
    }

    /** Swipes left to access the Communal Hub */
    fun swipeLeftToCommunal(): CommunalHub {
        swipeLeft()
        COMMUNAL_SELECTOR.assertVisible { "Communal Hub is not visible after swiping left" }
        return CommunalHub()
    }

    /**
     * Returns bounds of the lock icon at the bottom of the screen. HSV:
     * https://hsv.googleplex.com/5632535322165248
     */
    val lockIcon: LockscreenLockIcon
        get() {
            val lockIcon = waitForObj(lockScreenIconSelector) { "Lockscreen lock icon not found" }
            return LockscreenLockIcon(/* rect= */ lockIcon.visibleBounds)
        }

    /**
     * Returns user switcher on the lockscreen. HSV:
     * https://hsv.googleplex.com/5452172222267392?node=50
     */
    val userSwitcher: LockscreenUserSwitcher
        get() = LockscreenUserSwitcher()

    /** Returns the lockscreen StatusBar. Fails if not visible. */
    val statusBar: LockscreenStatusBar
        get() = LockscreenStatusBar()

    /** Swipes up to the bouncer. */
    fun swipeUpToBouncer(): Bouncer {
        swipeUp()
        return Bouncer(null)
    }

    /** Returns the bouncer. Fails if the bouncer is not visible. */
    fun getBouncer(): Bouncer {
        return Bouncer(null)
    }

    private fun swipeUp() {
        lockScreenSelector.assertVisible { "Lockscreen is not visible" }
        val swipeableArea = waitForObj(swipeableArea) { "Swipeable area not found" }
        // shift swipe gesture over to left so we don't begin the gesture on the lock icon
        //   this can be removed if b/229696938 gets resolved to allow for swiping on the icon
        val bounds = swipeableArea.visibleBounds
        val swipeX = bounds.left + bounds.width() / 4f
        BetterSwipe.swipe(
            PointF(swipeX, bounds.bottom - 1f),
            PointF(swipeX, bounds.top.toFloat()),
            displayId = displayId,
        )
    }

    private fun swipeLeft() {
        lockScreenSelector.assertVisible { "Lockscreen is not visible" }
        val swipeableArea = waitForObj(swipeableArea) { "Swipeable area not found" }
        val bounds = swipeableArea.visibleBounds
        val swipeY = bounds.top + bounds.height() / 2f
        BetterSwipe.swipe(
            PointF(bounds.right - 1f, swipeY),
            PointF(bounds.left + bounds.width() / 2f, swipeY),
            displayId = displayId,
        )
    }

    companion object {
        // https://hsv.googleplex.com/5130837462876160?node=117
        fun lockScreenSelector(displayId: Int = DEFAULT_DISPLAY): BySelector =
            if (sceneContainer()) {
                By.displayId(displayId).res("element:lockscreen")
            } else {
                sysuiResSelector("keyguard_indication_area", displayId)
            }

        // https://hsv.googleplex.com/5656353459666944
        val RON_AOD_SKELETON = sysuiResSelector("aod_promoted_notification_frame")
    }
}
