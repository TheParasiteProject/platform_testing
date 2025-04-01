"""
  Copyright (C) 2025 The Android Open Source Project

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.



  Test Steps:
  (0. Flash device)
  1. Pair Mobile device
  2. Mobile Device:  disconnect IVI-Bluetooth Device
  3. IVI: Go to Settings - Bluetooth
  Verify: Paired Device connection status changed from Connected to Disconnected
  4. Android Devices:Mobile Device:  Launch Settings - Bluetooth - Connect IVI Bluetooth Device
  Verify:Mobile device reconnected in both Phone and  IVI. Status connected displayed in paired device

"""

import logging

from mobly import asserts
from utilities.main_utils import common_main
from bluetooth_test import bluetooth_base_test
from mobly.controllers import android_device
from utilities import constants

class BluetoothDisableFromPhone(bluetooth_base_test.BluetoothBaseTest):

    def setup_test(self):
        # Pair caller phone with automotive device
        self.bt_utils.pair_primary_to_secondary()
        super().enable_recording()

    def test_disable_from_phone(self):
        self.discoverer = android_device.get_device(self.ads, label='auto')
        self.target = android_device.get_device(self.ads, label='phone')
        self.call_utils.open_bluetooth_settings()

        #Disconnect seahawk device from mobile phone and verify it is reflected in seahawk device
        self.target.mbs.btDisable()
        self.discoverer.mbs.waitUntilConnectionStatus(constants.DISCONNECTED_SUMMARY_STATUS)
        asserts.assert_true(
            self.discoverer.mbs.hasUIElementWithText(constants.DISCONNECTED_SUMMARY_STATUS),
            'Failed to disconnect from mobile device')

        #Connect seahawk device from mobile phone and verify it is reflected in seahawk device
        self.target.mbs.btEnable()
        self.discoverer.mbs.waitUntilConnectionStatus(constants.CONNECTED_SUMMARY_STATUS)
        asserts.assert_true(
            self.discoverer.mbs.hasUIElementWithText(constants.CONNECTED_SUMMARY_STATUS),
            'Failed to connect from mobile device')

if __name__ == '__main__':
    # Take test args
    common_main()