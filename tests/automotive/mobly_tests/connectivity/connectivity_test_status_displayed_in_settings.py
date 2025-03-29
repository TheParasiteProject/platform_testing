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
   1. Launch Settings --> Tap on Bluetooth - Turn OFF Bluetooth using Toggle button and wait for the device to disconnect
   Verify: Bluetooth Device disconnected
   2. Turn On Bluetooth using toggle button and wait for the device to reconnect the paired device
   Verify: Bluetooth Device reconnected. Device status is 'Connected'
   3. Repeat the Step 1 and 2, two times
   Verify:  Device reconnecting
"""

from bluetooth_test import bluetooth_base_test
from mobly import asserts
from utilities.main_utils import common_main
from utilities import constants
import logging

class BluetoothConnectionStatusOnSettings(bluetooth_base_test.BluetoothBaseTest):


   def setup_test(self):
       # Pair the devices
       self.bt_utils.pair_primary_to_secondary()
       super().enable_recording()

   def test_connection_status_displayed_on_device_screen(self):
       # Log BT Connection State after pairing
       bt_connection_state=self.call_utils.get_bt_connection_status_using_adb_command(self.discoverer)
       logging.info("BT State after pairing: <%s>", bt_connection_state)

       for i in range(0, 2):
           # Open bluetooth settings.
           self.call_utils.open_bluetooth_settings()

           # Find the target device and disconnect it on the bluetooth settings page
           target_name = self.target.mbs.btGetName()
           self.call_utils.press_bluetooth_toggle_on_device(target_name)
           self.discoverer.mbs.waitUntilConnectionStatus(constants.DISCONNECTED_SUMMARY_STATUS)

           #Validate whether Disconnected status is displayed or not
           asserts.assert_true(
              self.discoverer.mbs.hasUIElementWithText(constants.DISCONNECTED_SUMMARY_STATUS),
              "Expected Disconnected status in Bluetooth settings")

           # Connect again in bluetooth settings
           self.call_utils.press_bluetooth_toggle_on_device(target_name)
           self.discoverer.mbs.waitUntilConnectionStatus(constants.CONNECTED_SUMMARY_STATUS)

           #Validate whether Disconnected status is displayed or not
           asserts.assert_true(
              self.discoverer.mbs.hasUIElementWithText(constants.CONNECTED_SUMMARY_STATUS),
              "Expected Connected status in Bluetooth settings")

           self.call_utils.press_home()


if __name__ == '__main__':
   # Take test args
   common_main()