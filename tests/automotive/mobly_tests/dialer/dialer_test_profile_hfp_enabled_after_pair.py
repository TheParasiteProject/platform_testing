#  Copyright (C) 2025 The Android Open Source Project
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

"""Test of hfp profile after pairing
 Steps include:
        1) Pair the devices
        3) Check the HfpClientConnectionService
        4) Make a Phone call
        5) End call on IVI device
        6) Assert dialed number on the IVI device same as called ten digits number
"""
from mobly import asserts
from bluetooth_test import bluetooth_base_test
import logging
from utilities import constants
from utilities.main_utils import common_main


class BluetoothHFPDialTest(bluetooth_base_test.BluetoothBaseTest):

    def setup_test(self):
        logging.info("Pairing phone to car head unit.")
        self.bt_utils.pair_primary_to_secondary()
        super().enable_recording()
        self.target_name = self.target.mbs.btGetName()

    def test_hfp_enabled_after_pairing(self):
       logging.info("Validating HFP Profile after pairing")
       bt_hfp_status_txt = self.call_utils.get_bt_profile_status_using_adb_command(self.discoverer, constants.BLUETOOTH_HFP)
       logging.info("BT HFP Status: <%s>", bt_hfp_status_txt)
       asserts.assert_true('HfpClientDeviceBlock' in bt_hfp_status_txt, 'HFP Profile is not mapped')

       logging.info("Navigate to Bluetooth Settings")
       self.call_utils.open_bluetooth_settings()
       self.call_utils.wait_with_log(10)

       logging.info("Navigate to Bluetooth Summary Screen")
       self.discoverer.mbs.pressDeviceInBluetoothSettings(self.target_name)
       asserts.assert_true(self.call_utils.is_phone_profile_enabled(), 'HFP Profile is not enabled on Summary Page')

    def teardown_test(self):
        self.call_utils.wait_with_log(5)
        self.call_utils.press_home()
        super().teardown_test()

if __name__ == '__main__':
    common_main()