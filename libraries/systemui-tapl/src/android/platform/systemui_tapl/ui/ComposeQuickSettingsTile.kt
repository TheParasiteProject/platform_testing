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

import android.platform.systemui_tapl.utils.DeviceUtils.sysuiResSelector
import android.platform.systemui_tapl.utils.SETTINGS_PACKAGE
import android.platform.test.scenario.tapl_common.Gestures
import android.platform.test.scenario.tapl_common.Gestures.shortClick
import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisible
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForObj
import android.platform.uiautomatorhelpers.WaitUtils.ensureThat
import android.text.TextUtils
import android.view.Display.DEFAULT_DISPLAY
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiObject2
import com.google.common.truth.Truth.assertWithMessage
import kotlin.reflect.KClass

/**
 * Object to encapsulate a tile. See https://hsv.googleplex.com/4910828112314368
 *
 * In order to interact with the tile, [getBehavior] needs to be called, with the type of behavior
 * needed. There are also convenience methods for calling [click], [toggleAndAssertToggled], and
 * [longPress]. These methods will fail the test if the tile doesn't support that interaction.
 */
abstract class ComposeQuickSettingsTile private constructor(val displayId: Int = DEFAULT_DISPLAY) {
    /**
     * Representation of the tile object. This should be made to retrieve the object every time (if
     * possible) to prevent stale objects.
     */
    protected abstract val tile: UiObject2

    /** Whether the tile is small (icon only) or large (icon + text). */
    val isSmallTile: Boolean
        get() {
            val res = tile.resourceName
            return when {
                res.endsWith(SMALL_TILE_TAG) -> true
                res.endsWith(LARGE_TILE_TAG) -> false
                else -> error("Tile doesn't have a valid resource name: $res")
            }
        }

    /** The human readable name of the tile. */
    val tileName: String
        get() =
            if (!TextUtils.isEmpty(tile.contentDescription)) {
                tile.contentDescription
            } else {
                tile.getTextFromSelfOrChild()
            }

    /**
     * Get a specific behavior for the tile, by class. Prefer using [getBehavior] as it will return
     * a casted value. This will be `null` if the tile does not support that behavior.
     *
     * The behavior is created new every time it's requested, tied to the backing UiObject2, so
     * prefer creating it every time it's needed instead of storing it.
     */
    fun <T : TileBehavior> getBehavior(behaviorType: KClass<T>): TileBehavior? {
        return when (behaviorType) {
            Toggleable::class -> tile.takeIf { it.isCheckable }?.let { ToggleableImpl(it) }
            LongPressable::class ->
                tile.takeIf { it.isLongClickable }?.let { LongPressableImpl(it) }
            Clickable::class -> tile.takeIf { it.isClickable }?.let { ClickableImpl(it) }
            DualTarget::class -> tile.findObject(INNER_TARGET_SELECTOR)?.let { DualTargetImpl(it) }
            ToggleableDualTarget::class ->
                tile.findObject(INNER_TARGET_SELECTOR)?.let {
                    it.takeIf { it.isCheckable }?.let { DualTargetImpl(it) }
                }
            else -> null
        }
    }

    /** See [getBehavior]. */
    inline fun <reified T : TileBehavior> getBehavior(): T? {
        return getBehavior(T::class) as? T
    }

    /**
     * Perform a click on the tile with no validation of the effect. This will fail if the tile does
     * not support [Clickable].
     *
     * See [Clickable.click]
     */
    fun click() {
        getBehavior<Clickable>()!!.click()
    }

    /**
     * Toggle the current checked state of the tile, validating that the state has changed. This
     * will fail if the tile does not support [Toggleable].
     *
     * See [Toggleable.toggleAndAssertToggled]
     */
    fun toggleAndAssertToggled(extraValidation: () -> Unit = {}) {
        getBehavior<Toggleable>()!!.toggleAndAssertToggled(extraValidation)
    }

    /**
     * Perform a long press on the tile, validating that [expectedSettingsPackage] (or
     * [SETTINGS_PACKAGE] if `null`) is visible afterwards. This will fail if the tile does not
     * support [LongPressable].
     *
     * See [LongPressable.longPress]
     */
    fun longPress(expectedSettingsPackage: String? = null) {
        getBehavior<LongPressable>()!!.longPress(expectedSettingsPackage)
    }

    companion object {
        /** Create a [ComposeQuickSettingsTile] wrapper from a fixed [tile] ui object. */
        fun createFrom(
            tile: UiObject2,
            displayId: Int = DEFAULT_DISPLAY,
        ): ComposeQuickSettingsTile {
            return object : ComposeQuickSettingsTile(displayId) {
                override val tile: UiObject2
                    get() = tile
            }
        }

        /**
         * Create a [ComposeQuickSettingsTile] wrapper based on a [selector]. The wrapper will
         * re-fetch the ui object every time it's needed, giving more flexibility in case of stale.
         */
        fun createFrom(
            selector: BySelector,
            displayId: Int = DEFAULT_DISPLAY,
        ): ComposeQuickSettingsTile {
            return object : ComposeQuickSettingsTile(displayId) {
                override val tile: UiObject2
                    get() = waitForObj(selector)
            }
        }

        /** See https://hsv.googleplex.com/4910828112314368?node=37 */
        fun smallTileSelector(description: String, displayId: Int = DEFAULT_DISPLAY): BySelector {
            return sysuiResSelector(SMALL_TILE_TAG, displayId).descStartsWith(description)
        }

        /** See https://hsv.googleplex.com/4910828112314368?node=28 */
        fun largeTileSelector(description: String, displayId: Int = DEFAULT_DISPLAY): BySelector {
            return sysuiResSelector(LARGE_TILE_TAG, displayId)
                .hasChild(By.displayId(displayId).textStartsWith(description))
        }

        fun UiObject2.assertIsTile() {
            assertWithMessage("Tile has id $resourceName which is not a tile id")
                .that(
                    resourceName?.endsWith(SMALL_TILE_TAG) ?: false ||
                        resourceName?.endsWith(LARGE_TILE_TAG) ?: false
                )
                .isTrue()
        }

        const val SMALL_TILE_TAG = "qs_tile_small"
        const val LARGE_TILE_TAG = "qs_tile_large"
        private const val TOGGLE_TARGET_TAG = "qs_tile_toggle_target"

        private val INNER_TARGET_SELECTOR = sysuiResSelector(TOGGLE_TARGET_TAG)
    }
}

/** Behavior for a tile */
sealed interface TileBehavior

/** Behavior for clickable tiles. */
interface Clickable : TileBehavior {
    /** Click on the tile. No verification is performed. */
    fun click()
}

private class ClickableImpl(private val tile: UiObject2) : Clickable {
    init {
        check(tile.isClickable)
    }

    override fun click() {
        shortClick(tile, "Tile")
    }
}

/**
 * Behavior for tiles that are toggleable. This means that clicking on them will toggle them between
 * and Off state and an On state
 */
interface Toggleable : TileBehavior {
    /** Whether the tile is currently in its On state */
    val isChecked: Boolean

    /**
     * Toggle the tile between On/Off. Validates that the tile has changed checked state.
     *
     * [extraValidation] can be provided and will be performed immediately after clicking on the
     * tile, before waiting for the tile to change state. This can be used for verifying ephemeral
     * effects.
     */
    fun toggleAndAssertToggled(extraValidation: () -> Unit = {})

    /** Asserts the current checked state with a nice message. */
    fun assertCheckedStatus(checked: Boolean)
}

private open class ToggleableImpl(private val tile: UiObject2) : Toggleable {
    init {
        check(tile.isCheckable)
        check(tile.isClickable)
    }

    override val isChecked: Boolean
        get() = tile.isChecked

    override fun toggleAndAssertToggled(extraValidation: () -> Unit) {
        val wasChecked = isChecked
        shortClick(tile, "Tile")
        extraValidation()
        assertCheckedStatus(!wasChecked)
    }

    override fun assertCheckedStatus(checked: Boolean) {
        val expectedState = if (checked) "checked" else "unchecked"
        ensureThat("tile is $expectedState") { isChecked == checked }
    }
}

/** Behavior for tiles that support long press. */
interface LongPressable : TileBehavior {
    /**
     * Long press on the tile. Validates that a settings activity with the correct package was
     * launched.
     */
    fun longPress(expectedSettingsPackage: String? = null)
}

private class LongPressableImpl(private val tile: UiObject2) : LongPressable {
    init {
        check(tile.isLongClickable)
    }

    override fun longPress(expectedSettingsPackage: String?) {
        Gestures.longClickDownUp(tile, "Quick settings tile", tile.displayId) {
            val packageName = expectedSettingsPackage ?: SETTINGS_PACKAGE
            By.displayId(tile.displayId).pkg(packageName).assertVisible {
                "$packageName didn't appear"
            }
        }
    }
}

/**
 * Behavior for tiles that support a dual target. The dual target is not necessarily a toggle
 * between On/Off
 */
interface DualTarget : Clickable, LongPressable

/** Behavior for tiles that support a dual target that is an On/Off toggle */
interface ToggleableDualTarget : Toggleable, DualTarget

private class DualTargetImpl(innerTarget: UiObject2) :
    ToggleableDualTarget,
    Toggleable by ToggleableImpl(innerTarget),
    Clickable by ClickableImpl(innerTarget),
    LongPressable by LongPressableImpl(innerTarget)

private fun UiObject2.getTextFromSelfOrChild(): String {
    return if (!TextUtils.isEmpty(text)) {
        text
    } else {
        children.firstOrNull { !TextUtils.isEmpty(it.text) }?.text ?: ""
    }
}
