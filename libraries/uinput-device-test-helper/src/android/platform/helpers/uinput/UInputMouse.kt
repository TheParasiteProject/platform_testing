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

package android.platform.helpers.uinput

class UInputMouse
@JvmOverloads
constructor(
    override val productId: Int = MOUSE_PRODUCT_ID,
    override val vendorId: Int = MOUSE_VENDOR_ID,
    override val name: String = MOUSE_NAME,
    override val supportedKeys: List<Int> = emptyList(),
) : UInputDevice() {
    override val inputDeviceId = 2
    override val bus = "usb"

    override fun getRegisterCommand(): String {
        return """{
          "id": $inputDeviceId,
          "command": "register",
          "name": "$MOUSE_NAME",
          "vid": "$MOUSE_VENDOR_ID",
          "pid": "$MOUSE_PRODUCT_ID",
          "bus": "usb",
          "port": "usb:1",
          "configuration": [
            {
              "type": "UI_SET_EVBIT",
              "data": ["EV_REL", "EV_KEY"]
            },
            {
              "type": "UI_SET_RELBIT",
              "data": ["REL_X", "REL_Y"]
            },
            {
              "type": "UI_SET_KEYBIT",
              "data": [
                "BTN_LEFT",
                "BTN_RIGHT",
                "BTN_MIDDLE"
              ]
            }
          ]
        }"""
            .trimIndent()
            .replace("\n", "")
    }

    companion object {
        const val MOUSE_PRODUCT_ID = 0xabcd
        const val MOUSE_VENDOR_ID = 0x18d1
        const val MOUSE_NAME = "Test Mouse"
    }
}
