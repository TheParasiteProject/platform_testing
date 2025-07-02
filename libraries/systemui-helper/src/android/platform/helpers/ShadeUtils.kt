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

package android.platform.helpers

import android.content.Context
import android.platform.uiautomatorhelpers.DeviceHelpers
import android.provider.Settings
import com.android.systemui.Flags

object ShadeUtils {

    /**
     * @return true if the device has a config, that should display SingleShade.
     *
     * This is useful for tests that are validating SingleShade specific scenarios.
     */
    @JvmStatic
    fun isSingleShadeConfig(): Boolean =
            if (Flags.sceneContainer()) {
                // TODO(429153906) use a display-aware context.
                !shouldShowDualShade(DeviceHelpers.context)
            } else {
                !CommonUtils.isSplitShade()
            }

    /**
     * @return tue if the device has a config, that should display DualShade.
     *
     * This is useful for tests that are validating DualShade specific scenarios.
     */
    @JvmStatic
    fun isDualShadeConfig(): Boolean =
        // TODO(429153906) use a display-aware context.
        Flags.sceneContainer() && shouldShowDualShade(DeviceHelpers.context)

    /**
     * @return true if the device has a config, that should display SplitShade. Always false, when
     *   [Flags.sceneContainer] is enabled.
     */
    @JvmStatic
    fun isSplitShadeConfig(): Boolean = !Flags.sceneContainer() && CommonUtils.isSplitShade()

    private const val COMPACT_SCREEN_MAX_DPS = 600

    private fun shouldShowDualShade(context: Context) =
        isDualShadeSettingEnabled(context) || isShadeLayoutWide(context)

    private fun isDualShadeSettingEnabled(context: Context) =
        Settings.Secure.getInt(context.contentResolver, Settings.Secure.DUAL_SHADE, 0) == 1

    private fun isShadeLayoutWide(context: Context) =
        context.resources.configuration.screenWidthDp >= COMPACT_SCREEN_MAX_DPS
}
