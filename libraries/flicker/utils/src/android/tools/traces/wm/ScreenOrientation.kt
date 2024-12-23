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

enum class ScreenOrientation(val value: Int) {
    SCREEN_ORIENTATION_UNSET(-2),
    SCREEN_ORIENTATION_UNSPECIFIED(-1),
    SCREEN_ORIENTATION_LANDSCAPE(0),
    SCREEN_ORIENTATION_PORTRAIT(1),
    SCREEN_ORIENTATION_USER(2),
    SCREEN_ORIENTATION_BEHIND(3),
    SCREEN_ORIENTATION_SENSOR(4),
    SCREEN_ORIENTATION_NOSENSOR(5),
    SCREEN_ORIENTATION_SENSOR_LANDSCAPE(6),
    SCREEN_ORIENTATION_SENSOR_PORTRAIT(7),
    SCREEN_ORIENTATION_REVERSE_LANDSCAPE(8),
    SCREEN_ORIENTATION_REVERSE_PORTRAIT(9),
    SCREEN_ORIENTATION_FULL_SENSOR(10),
    SCREEN_ORIENTATION_USER_LANDSCAPE(11),
    SCREEN_ORIENTATION_USER_PORTRAIT(12),
    SCREEN_ORIENTATION_FULL_USER(13),
    SCREEN_ORIENTATION_LOCKED(14);

    companion object {
        fun fromName(name: String?): ScreenOrientation? {
            return name?.let { values().find { it.name == name } }
        }
    }
}
