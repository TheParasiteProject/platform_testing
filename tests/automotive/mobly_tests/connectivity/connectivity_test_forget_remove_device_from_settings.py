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
  1. Navigate to bluetooth settings
  2.  press Paired device (on device listing) to Forget/remove paired device.

"""

import logging
from mobly import asserts
from utilities import constants
from utilities.main_utils import common_main
from bluetooth_test import bluetooth_base_test
from utilities.common_utils import CommonUtils
from mobly.controllers import android_device
from mobly.controllers.android_device_lib.snippet_client_v2 import Config


class BluetoothForgetDeviceFromSettingsTest(bluetooth_base_test.BluetoothBaseTest):

    def setup_class(self):
        super().setup_class()
        self.common_utils = CommonUtils(self.target, self.discoverer)
        super().enable_recording()
        self.call_utils.press_home()

    def setup_test(self):
        # Pair the devices
        self.bt_utils.pair_primary_to_secondary()

    def test_forget_remove_device_from_settings(self):
        # Log BT Connection State after pairing
        bt_connection_state = self.call_utils.get_bt_connection_status_using_adb_command(
            self.discoverer)
        logging.info("BT State after pairing : <%s>", bt_connection_state)

        # Allow time for connection to fully sync.
        self.call_utils.wait_with_log(constants.WAIT_THIRTY_SECONDS)

        bt_connection_state = self.call_utils.get_bt_connection_status_using_adb_command(
            self.discoverer)
        logging.info("BT Connection State after wait time : <%s>", bt_connection_state)

        # Navigate to Bluetooth Settings
        self.call_utils.open_bluetooth_settings_form_status_bar()
        self.call_utils.wait_with_log(5)
        # Press the paired target device
        self.discoverer.mbs.waitUntilConnectionStatus("Connected")
        self.discoverer.mbs.pressDeviceInBluetoothSettings(self.target.mbs.btGetName())
        self.call_utils.press_forget()
        self.call_utils.wait_with_log(5)
        # Verify that paired device is not listed
        asserts.assert_false(
            self.common_utils.has_ui_element_with_text(self.target.mbs.btGetName()),
            "Device is still paired in settings ")

    def teardown_test(self):
        discoverer_address = self.discoverer.mbs.btGetAddress()
        self.target.mbs.btUnpairDevice(discoverer_address)
        self.discoverer.mbs.btDisable()
        self.target.mbs.btDisable()
        super().hu_recording_handler()


if __name__ == '__main__':
    # Take test args
    common_main()
