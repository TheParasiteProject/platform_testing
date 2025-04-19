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

package android.platform.systemui_tapl.ui

import android.platform.systemui_tapl.ui.ComposeQuickSettingsTile.Companion.assertIsTile
import android.platform.systemui_tapl.utils.DeviceUtils.LONG_WAIT
import android.platform.systemui_tapl.utils.DeviceUtils.sysuiResSelector
import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisibility
import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisible
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForFirstObj
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForObj
import android.view.Display.DEFAULT_DISPLAY
import androidx.test.uiautomator.UiObject2
import com.android.systemui.Flags

/**
 * System UI test automation object representing quick quick settings in the notification shade.
 *
 * This is the area that contains a few tiles and doesn't have pages, appearing on top of the
 * Notification space.
 *
 * https://hsv.googleplex.com/4814389392703488?node=15#
 */
class QuickQuickSettings internal constructor(val displayId: Int = DEFAULT_DISPLAY) {

    private val qsTileLayoutSelector = sysuiResSelector("qqs_tile_layout", displayId)
    private val qqsTilesContainer: UiObject2

    init {
        qsContainerSelector(displayId).assertVisible(timeout = LONG_WAIT) {
            "Quick quick settings not visible"
        }
        qqsTilesContainer =
            waitForObj(qsTileLayoutSelector) { "Quick quick settings does not have a tile layout" }
    }

    /**
     * Get a list of [QuickSettingsTile] objects, each representing one of the tiles visible in the
     * QuickQuickSettings container. Will fail if there's an element that's not a tile (i.e.,
     * doesn't have the label view as https://hsv.googleplex.com/4814389392703488?node=22#), only
     * when the [qsUiRefactorComposeFragment] flag is false.
     */
    fun getVisibleTiles(): List<QuickQuickSettingsTile> {
        val uiTiles = qqsTilesContainer.children
        if (!Flags.qsUiRefactorComposeFragment()) {
            uiTiles.forEach { tile ->
                tile.assertVisibility(tileLabelSelector(displayId), visible = true)
            }
        }
        return uiTiles.map { QuickQuickSettingsTile(it, displayId) }
    }

    fun getVisibleComposeTiles(): List<ComposeQuickSettingsTile> {
        val uiChildren = qqsTilesContainer.children
        val largeTileSelector = sysuiResSelector(ComposeQuickSettingsTile.LARGE_TILE_TAG, displayId)
        val smallTileSelector = sysuiResSelector(ComposeQuickSettingsTile.SMALL_TILE_TAG, displayId)
        val uiTiles =
            uiChildren.map { child ->
                val (tile, _) =
                    child.waitForFirstObj(smallTileSelector, largeTileSelector) {
                        "No tile object found in child $child"
                    }
                tile.assertIsTile()
                tile
            }
        return uiTiles.map { ComposeQuickSettingsTile.createFrom(it, displayId) }
    }

    companion object {
        @JvmStatic
        fun qsContainerSelector(displayId: Int = DEFAULT_DISPLAY) =
            sysuiResSelector("quick_qs_panel", displayId)

        @JvmStatic
        fun tileLabelSelector(displayId: Int = DEFAULT_DISPLAY) =
            sysuiResSelector("tile_label", displayId)
    }
}
