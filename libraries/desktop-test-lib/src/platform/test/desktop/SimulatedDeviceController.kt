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

import android.graphics.Point
import android.hardware.display.DisplayManager
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertWithMessage

/**
 * A simulated display device returned by a [SimulatedDeviceController].
 *
 * @param d The underlying [DisplayDevice].
 */
data class SimulatedDisplayDevice(val d: DisplayDevice) : DisplayDevice by d

/** A controller for simulated peripherals. */
class SimulatedDeviceController : PeripheralsController {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val displayManager = context.getSystemService(DisplayManager::class.java)
    private val simulatedDisplayTestRule = SimulatedConnectedDisplayTestRule()
    private var currentDisplaysPeripherals = emptyList<Pair<DisplayPeripheral, Int>>()

    override fun requestPeripherals(request: PeripheralsRequest): PeripheralsResponse {
        request.validate(PeripheralType.SIMULATED, PeripheralType.PHYSICAL_OR_SIMULATED)

        val displayPeripherals = mutableListOf<DisplayPeripheral>()

        request.peripherals.forEach {
            when (it) {
                is DisplayPeripheral -> displayPeripherals.add(it)
            }
        }

        return setupSimulatedDisplays(displayPeripherals)
    }

    private fun setupSimulatedDisplays(peripherals: List<DisplayPeripheral>): PeripheralsResponse {
        if (peripherals.isEmpty() && currentDisplaysPeripherals.isEmpty()) {
            return PeripheralsResponse()
        }

        val displayConfigs: List<Point> = peripherals.map { Point(it.size.width, it.size.height) }
        val addedDisplayIds = simulatedDisplayTestRule.setupTestDisplays(displayConfigs)
        assertWithMessage("Failed to setup all requested simulated displays")
            .that(addedDisplayIds.size)
            .isEqualTo(peripherals.size)

        val removedDisplays = currentDisplaysPeripherals.filter { it.first !in peripherals }
        currentDisplaysPeripherals = peripherals.zip(addedDisplayIds)

        val addedResponse =
            PeripheralsResponse(
                currentDisplaysPeripherals.map { (peripheral, displayId) ->
                    SimulatedDisplayDevice(
                        AnyDisplayDevice(
                            peripheral,
                            connected = true,
                            displayId,
                            displayManager.getDisplay(displayId),
                        )
                    )
                }
            )

        val removedResponse =
            PeripheralsResponse(
                removedDisplays.map { (peripheral, displayId) ->
                    SimulatedDisplayDevice(
                        AnyDisplayDevice(
                            peripheral,
                            connected = false,
                            displayId,
                            display = null,
                        )
                    )
                }
            )

        return addedResponse + removedResponse
    }
}
