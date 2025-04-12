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

import com.google.common.truth.Truth.assertThat
import kotlin.test.fail
import org.junit.Rule
import org.junit.Test

/** Tests for [PeripheralDeviceTestRule]. */
public class PeripheralDeviceTest {
    @get:Rule val peripheralDeviceRule = PeripheralDeviceTestRule()

    @Test
    fun testSimulatedDisplay() {
        val response =
            peripheralDeviceRule.requestPeripherals(
                DisplayPeripheral(PeripheralType.SIMULATED, DisplaySize.SIZE_1080P)
            )
        assertThat(response.devices.filter { it.connected }).hasSize(1)
        response.devices.forEach {
            when (it) {
                is SimulatedDisplayDevice -> assertThat(it.displayId).isGreaterThan(0)
                else -> fail("Unexpected peripheral device: $it")
            }
        }
    }

    @Test
    fun testPhysicalDisplay() {
        val response =
            peripheralDeviceRule.requestPeripherals(
                DisplayPeripheral(PeripheralType.PHYSICAL, DisplaySize.SIZE_1080P)
            )
        assertThat(response.devices.filter { it.connected }).hasSize(1)
        response.devices.forEach {
            when (it) {
                is PhysicalDisplayDevice -> assertThat(it.displayId).isGreaterThan(0)
                else -> fail("Unexpected peripheral device: $it")
            }
        }
    }

    @Test
    fun testPhysicalOrSimulatedDisplay() {
        val response =
            peripheralDeviceRule.requestPeripherals(
                DisplayPeripheral(PeripheralType.PHYSICAL_OR_SIMULATED, DisplaySize.SIZE_1080P)
            )
        assertThat(response.devices.filter { it.connected }).hasSize(1)
        response.devices.forEach {
            when (it) {
                is DisplayDevice -> assertThat(it.displayId).isGreaterThan(0)
                else -> fail("Unexpected peripheral device: $it")
            }
        }
    }
}
