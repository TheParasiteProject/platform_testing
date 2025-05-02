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

package android.platform.systemui_tapl.ui

import android.platform.systemui_tapl.utils.DeviceUtils.launcherResSelector
import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisible
import android.platform.uiautomatorhelpers.DeviceHelpers.click

/**
 * Provides an API to interact with the stashed bubble bar handle
 *
 * @see [BubbleBar] for collapsed bubble bar
 * @see [ExpandedBubbleBar] for expanded bubble bar
 */
class StashedBubbleBar {

    init {
        HANDLE_VIEW.assertVisible { "Failed while waiting for bubble bar handle to become visible" }
    }

    /**
     * Clicks on the stashed bubble bar handle to expand the bubble bar
     *
     * @return [ExpandedBubbleBar]
     */
    fun click(): ExpandedBubbleBar {
        HANDLE_VIEW.click()
        return ExpandedBubbleBar(BubbleBar().selectedBubble)
    }

    companion object {
        val HANDLE_VIEW = launcherResSelector("stashed_bubble_handle")
    }
}
