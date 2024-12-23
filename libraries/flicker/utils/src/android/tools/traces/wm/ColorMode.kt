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

package android.tools.traces.wm

enum class ColorMode(val value: Int) {
    COLOR_MODE_INVALID(-1),
    // The default or native gamut of the display.
    COLOR_MODE_DEFAULT(0),
    COLOR_MODE_BT601_625(1),
    COLOR_MODE_BT601_625_UNADJUSTED(2),
    COLOR_MODE_BT601_525(3),
    COLOR_MODE_BT601_525_UNADJUSTED(4),
    COLOR_MODE_BT709(5),
    COLOR_MODE_DCI_P3(6),
    COLOR_MODE_SRGB(7),
    COLOR_MODE_ADOBE_RGB(8),
    COLOR_MODE_DISPLAY_P3(9);

    companion object {
        fun fromName(name: String?): ColorMode? {
            return name?.let { ColorMode.values().find { it.name == name } }
        }
    }
}
