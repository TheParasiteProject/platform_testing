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

/**
 * A physical display device returned by a [PhysicalDeviceController].
 *
 * @param d The underlying [DisplayDevice].
 */
data class PhysicalDisplayDevice(val d: DisplayDevice) : DisplayDevice by d

/** A controller for physical peripherals. */
class PhysicalDeviceController : PeripheralsController {
    override fun requestPeripherals(request: PeripheralsRequest): PeripheralsResponse {
        // TODO: b/351118894 - Implement this.
        return PeripheralsResponse()
    }
}
