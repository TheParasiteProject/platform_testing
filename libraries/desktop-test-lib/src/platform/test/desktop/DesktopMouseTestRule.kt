/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package platform.test.desktop

import android.Manifest
import android.companion.virtual.VirtualDeviceManager
import android.companion.virtual.VirtualDeviceParams
import android.hardware.display.DisplayManager
import android.hardware.input.InputManager
import android.hardware.input.VirtualMouse
import android.hardware.input.VirtualMouseConfig
import android.os.Handler
import android.os.Looper
import android.platform.uiautomatorhelpers.AdoptShellPermissionsRule
import android.util.Log
import android.view.Display.DEFAULT_DISPLAY
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A [TestRule] to support [VirtualMouse] move and drag within a single display / crossing across
 * displays.
 */
class DesktopMouseTestRule() : TestRule {
    private val adoptShellPermissionsTestRule = AdoptShellPermissionsRule(*PERMISSIONS)
    private val fakeAssociationRule = FakeAssociationRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val displayManager = context.getSystemService(DisplayManager::class.java)
    private val resourceTracker = ResourceTracker(fakeAssociationRule, displayManager)
    private val ruleChain =
        RuleChain.outerRule(adoptShellPermissionsTestRule)
            .around(fakeAssociationRule)
            .around(resourceTracker)

    override fun apply(base: Statement, description: Description) =
        ruleChain.apply(base, description)

    /**
     * Internal test rule that tracks created virtual mouse and modified display topology and
     * ensures they are properly closed / restored after the test.
     */
    private class ResourceTracker(
        private val fakeAssociationRule: FakeAssociationRule,
        private val displayManager: DisplayManager,
    ) : ExternalResource() {
        private val context = InstrumentationRegistry.getInstrumentation().targetContext
        private val inputManager = context.getSystemService(InputManager::class.java)
        private val virtualDeviceManager =
            context.getSystemService(VirtualDeviceManager::class.java)

        // TODO: b/392534769 - Refactor this to use UinputMouse.
        private var virtualDevice: VirtualDeviceManager.VirtualDevice? = null
        private var virtualMouse: VirtualMouse? = null
        private val displayIdsWithMouseScalingDisabled = mutableListOf<Int>()

        val requireVirtualMouse: VirtualMouse
            get() = checkNotNull(virtualMouse) { "Failed to initialize VirtualMouse" }

        /**
         * Sets up a virtual mouse that will start at [DEFAULT_DISPLAY].
         *
         * Note: This will move any existing cursor in the same topology as [DEFAULT_DISPLAY], to
         * [DEFAULT_DISPLAY]. If the mouse needs to start at different display, first call
         * `DesktopMouseTestRule#move(displayId, x, y)`.
         */
        override fun before() = runBlocking {
            val createdVirtualDevice =
                virtualDeviceManager.createVirtualDevice(
                    fakeAssociationRule.associationInfo.id,
                    VirtualDeviceParams.Builder().build(),
                )
            virtualDevice = createdVirtualDevice

            val inputDeviceFlow = callbackFlow {
                val inputDeviceListener =
                    object : InputManager.InputDeviceListener {
                        override fun onInputDeviceAdded(deviceId: Int) {
                            val device = inputManager.getInputDevice(deviceId) ?: return
                            if (
                                device.vendorId == VIRTUAL_MOUSE_VENDOR_ID &&
                                    device.productId == VIRTUAL_MOUSE_PRODUCT_ID
                            ) {
                                trySend(deviceId)
                                close()
                            }
                        }

                        override fun onInputDeviceRemoved(deviceId: Int) {}

                        override fun onInputDeviceChanged(deviceId: Int) {}
                    }
                val handler = Handler(Looper.getMainLooper())
                inputManager.registerInputDeviceListener(inputDeviceListener, handler)
                awaitClose { inputManager.unregisterInputDeviceListener(inputDeviceListener) }
            }

            // Note: AssociatedDisplayId is not actually used in connected displays, since cursor
            // will be able to cross displays and no longer gets "associated" anymore, with an
            // exception if display is excluded from the current DisplayTopology.
            virtualMouse =
                createdVirtualDevice.createVirtualMouse(
                    VirtualMouseConfig.Builder()
                        .setVendorId(VIRTUAL_MOUSE_VENDOR_ID)
                        .setProductId(VIRTUAL_MOUSE_PRODUCT_ID)
                        .setInputDeviceName("VirtualMouse_ConnectedDisplaysTest")
                        .setAssociatedDisplayId(DEFAULT_DISPLAY)
                        .build()
                )

            withTimeoutOrNull(TIMEOUT) { inputDeviceFlow.take(1) }
                ?: error("Timed out waiting for input device to be added.")

            disableMouseScaling()
            // TODO: b/399523818 - Ensure cursor starts on DEFAULT_DISPLAY
            // TODO: b/380001108 - Adds util method (move, drag) for test writer to interact with
            //  the VirtualMouse
        }

        private fun disableMouseScaling() {
            for (display in displayManager.displays) {
                displayIdsWithMouseScalingDisabled += display.displayId
                inputManager.setMouseScalingEnabled(false, display.displayId)
            }
        }

        override fun after() {
            for (displayId in displayIdsWithMouseScalingDisabled) {
                try {
                    inputManager.setMouseScalingEnabled(true, displayId)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to restore mouse scaling for display#$displayId", e)
                }
            }
            virtualMouse?.close()
            virtualDevice?.close()
            virtualMouse = null
            virtualDevice = null
            super.after()
        }
    }

    private companion object {
        const val VIRTUAL_MOUSE_VENDOR_ID = 123
        const val VIRTUAL_MOUSE_PRODUCT_ID = 456
        const val TAG = "DesktopMouseTestRule"
        val TIMEOUT: Duration = 10.seconds
        val PERMISSIONS =
            arrayOf(
                Manifest.permission.ASSOCIATE_COMPANION_DEVICES,
                "android.permission.MANAGE_COMPANION_DEVICES",
                Manifest.permission.CREATE_VIRTUAL_DEVICE,
                Manifest.permission.INJECT_EVENTS,
                "android.permission.MANAGE_DISPLAYS",
                Manifest.permission.SET_POINTER_SPEED,
            )
    }
}
