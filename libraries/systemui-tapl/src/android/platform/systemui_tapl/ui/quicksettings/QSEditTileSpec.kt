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

/**
 * Enum class representing different Quick Settings (QS) tiles to use for edit mode tests.
 *
 * @property desc The label of the QS tile.
 * @property spec The tile's tilespec.
 */
enum class QSEditTileSpec(val desc: String, val spec: String) {
    INTERNET("Internet", "internet"),
    BLUETOOTH("Bluetooth", "bt"),
    MODES("Modes", "dnd"),
    AUTO_ROTATE("Auto-rotate", "rotation"),
    DARK_THEME("Dark theme", "dark"),
    ALARM("Alarm", "alarm"),
    FLASHLIGHT("Flashlight", "flashlight"),
    CAMERA("Camera access", "cameratoggle"),
    MIC("Mic access", "mictoggle"),
    NIGHT_LIGHT("Night Light", "night"),
    SCREEN_RECORD("Screen record", "screenrecord"),
    BATTERY_SAVER("Battery Saver", "battery"),
}
