"""
  Copyright (C) 2023 The Android Open Source Project

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
  1. Pair Bluetooth Device with all supported profiles
  2. Launch Settings - Bluetooth page
  3. Tap the Media button. Verify: Media Button turns to Grey
  4. Launch Media Center from Media Widget and verify no media displayed
  5. Repeat Enable-Disable Media profile multiple times via Media button

"""

from mobly import asserts
from utilities.main_utils import common_main
from utilities.common_utils import CommonUtils
from bluetooth_test import bluetooth_base_test
from utilities import constants
import logging

class BluetoothMediaStatusOnMediaCenter(bluetooth_base_test.BluetoothBaseTest):

    def setup_class(self):
      super().setup_class()
      self.common_utils = CommonUtils(self.target, self.discoverer)
      super().enable_recording()
      self.call_utils.press_home()

    def setup_test(self):
        # Pair the devices
        self.common_utils.grant_local_mac_address_permission()
        self.bt_utils.pair_primary_to_secondary()
        self.target_name = self.target.mbs.btGetName()

    def test_media_status_on_media_center(self):
        # Log BT Connection State after pairing
        bt_connection_state=self.call_utils.get_bt_connection_status_using_adb_command(self.discoverer)
        logging.info("BT State after pairing : <%s>", bt_connection_state)

        for i in range(0,2):
          # Navigate to the bluetooth settings page
          self.call_utils.open_bluetooth_settings()
          # Disable media for the listed paired device via the preference button
          self.call_utils.press_media_toggle_on_device(self.target_name)
          # Confirm that the media button is unchecked
          asserts.assert_false(
              self.discoverer.mbs.isMediaPreferenceChecked(),
              "Expected media button to be unchecked after pressing it.")

          # Launch media center and check media status is disconnected
          self.call_utils.open_bluetooth_media_app()
          asserts.assert_true(self.call_utils.is_connect_to_bluetooth_label_visible_on_bluetooth_audio_page(), "Connect to Bluetooth Label is not visible")

          # Go back to the bluetooth settings page and enable media via the preference button
          self.call_utils.press_home()
          self.call_utils.open_bluetooth_settings()
          self.call_utils.press_media_toggle_on_device(self.target_name)
          self.call_utils.open_bluetooth_settings_form_status_bar()

          # Confirm that the media button is re-enabled
          asserts.assert_true(
              self.discoverer.mbs.isMediaPreferenceChecked(),
              "Expected media button to be checked after pressing it a second time.")

          # Launch media center and check media status is connected
          self.call_utils.open_bluetooth_media_app()
          asserts.assert_false(self.call_utils.is_connect_to_bluetooth_label_visible_on_bluetooth_audio_page(), "Connect to Bluetooth Label is visible")

if __name__ == '__main__':
    # Take test args
    common_main()