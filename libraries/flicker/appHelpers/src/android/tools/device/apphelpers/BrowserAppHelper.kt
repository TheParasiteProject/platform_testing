/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.tools.device.apphelpers

import android.app.Instrumentation
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Point
import android.net.Uri
import android.tools.traces.component.ComponentNameMatcher
import android.tools.traces.component.IComponentNameMatcher
import android.tools.traces.parsers.WindowManagerStateHelper
import android.tools.traces.wm.WindowingMode
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import java.time.Duration

/**
 * Helper to launch the default browser app (compatible with AOSP)
 *
 * This helper has no other functionality but the app launch.
 *
 * This helper is used to launch an app after some operations (e.g., navigation mode change), so
 * that the device is stable before executing flicker tests
 */
class BrowserAppHelper
@JvmOverloads
constructor(
    instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation(),
    pkgManager: PackageManager = instrumentation.context.packageManager,
    appName: String = getBrowserName(pkgManager),
    appComponent: IComponentNameMatcher = getBrowserComponent(pkgManager),
) : StandardAppHelper(instrumentation, appName, appComponent) {
    override val openAppIntent =
        pkgManager.getLaunchIntentForPackage(packageName)
            ?: error("Unable to find intent for browser")

    private val device = UiDevice.getInstance(instrumentation)

    fun openThreeDotsMenu() {
        device
            .wait(Until.findObject(By.res(packageName, "menu_button")), WAIT_TIME_IN_MILLISECONDS)
            .click()
    }

    fun clickNewTabInMenu() {
        device
            .wait(
                Until.findObject(By.res(packageName, "new_tab_menu_id")),
                WAIT_TIME_IN_MILLISECONDS,
            )
            .click()
    }

    /**
     * Simulates the UI action of "tearing" a tab out of its current window.
     *
     * This gesture is intended to trigger the tab detachment behavior in applications that support
     * it. Note: The effectiveness of this function depends on the hardcoded start/end points
     * aligning with the actual UI elements (tab location, drop target area) in the application
     * under test.
     *
     * @param device The UiDevice instance used to perform the drag interaction on the device.
     * @param wmHelper A helper class instance used to retrieve window and display dimension
     *   information.
     * @param direction A direction where the tab should be dragged to.
     */
    fun performTabTearing(
        wmHelper: WindowManagerStateHelper,
        direction: TabDraggingDirection = TabDraggingDirection.TOP_LEFT,
    ) {
        require(isFreeform(wmHelper)) { "Windowing mode should be WINDOWING_MODE_FREEFORM" }
        val windowBounds = wmHelper.getWindowRegion(this).bounds
        val windowWidthInPx = windowBounds.width()
        val windowWidthInDp =
            Math.round(
                windowWidthInPx.toFloat() / context.getResources().getDisplayMetrics().density
            )
        require(windowWidthInDp >= MIN_WINDOW_WIDTH_FOR_TAB_TEARING_DP) {
            "Window width is to small to drag one of its tabs"
        }
        // Define the drag start point. Since the tab is rendered directly on a surface, we cannot
        // easily query its exact bounds. Instead, we use hardcoded offsets (300px right, 40px down)
        // from the window's top-left corner, hoping to reliably land the starting point somewhere
        // on the draggable tab area.
        val startDraggingPoint =
            Point(/* x= */ windowBounds.left + 300, /* y= */ windowBounds.top + 40)
        // Define the drag end point. This point is intentionally outside the original window bounds
        // (100px left, 100px up from the window's top-left corner) to simulate dragging the tab
        // clear of the window, triggering the "tear" action.
        val endDraggingPoint =
            when (direction) {
                TabDraggingDirection.TOP_LEFT ->
                    Point(/* x= */ windowBounds.left - 100, /* y= */ windowBounds.top - 100)
            }
        device.drag(
            startDraggingPoint.x,
            startDraggingPoint.y,
            endDraggingPoint.x,
            endDraggingPoint.y,
            /* steps= */ 100,
        )
    }

    fun isFreeform(wmHelper: WindowManagerStateHelper): Boolean =
        wmHelper.getWindow(this)?.windowingMode == WindowingMode.WINDOWING_MODE_FREEFORM.value

    companion object {
        enum class TabDraggingDirection {
            TOP_LEFT
        }

        private val WAIT_TIME_IN_MILLISECONDS = Duration.ofSeconds(3).toMillis()
        private const val MIN_WINDOW_WIDTH_FOR_TAB_TEARING_DP = 600

        private fun getBrowserIntent(): Intent {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return intent
        }

        private fun getBrowserName(pkgManager: PackageManager): String {
            val intent = getBrowserIntent()
            val resolveInfo =
                pkgManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
                    ?: error("Unable to resolve browser activity")

            return resolveInfo.loadLabel(pkgManager).toString()
        }

        private fun getBrowserComponent(pkgManager: PackageManager): IComponentNameMatcher {
            val intent = getBrowserIntent()
            val resolveInfo =
                pkgManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
                    ?: error("Unable to resolve browser activity")
            return ComponentNameMatcher(resolveInfo.activityInfo.packageName, className = "")
        }
    }
}
