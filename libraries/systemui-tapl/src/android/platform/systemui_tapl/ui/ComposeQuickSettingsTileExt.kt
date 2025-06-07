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

fun ComposeQuickSettingsTile.clickInternetTileToOpenDialog(): InternetDialog {
    click()
    return InternetDialog(displayId)
}

fun ComposeQuickSettingsTile.clickBluetoothTileToOpenDialog(): BluetoothDialog {
    clickToOpenDialogOnDualTarget()
    return BluetoothDialog(displayId)
}

fun ComposeQuickSettingsTile.clickFlashlightTileToOpenDialog(): FlashlightDialog {
    clickToOpenDialogOnDualTarget()
    return FlashlightDialog(displayId)
}

fun ComposeQuickSettingsTile.clickModesTileToOpenDialog(): ModesDialog {
    clickToOpenDialogOnDualTarget()
    return ModesDialog(displayId)
}

private fun ComposeQuickSettingsTile.clickToOpenDialogOnDualTarget() {
    if (isSmallTile) {
        longPress()
    } else {
        click()
    }
}

/** Returns a Toggleable behavior for a dual target tile, regardless of the tile size. */
fun ComposeQuickSettingsTile.getToggleableBehaviorForDualTarget(): Toggleable {
    return if (isSmallTile) {
        getBehavior<Toggleable>()!!
    } else {
        getBehavior<ToggleableDualTarget>()!!
    }
}
