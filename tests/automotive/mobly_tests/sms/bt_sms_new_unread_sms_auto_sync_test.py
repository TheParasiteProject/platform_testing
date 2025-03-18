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

import logging

from mobly import asserts
from utilities import constants
from utilities.main_utils import common_main
from utilities.common_utils import CommonUtils
from bluetooth_sms_test import bluetooth_sms_base_test
from mobly.controllers import android_device

# Number of seconds for the target to stay discoverable on Bluetooth.
DISCOVERABLE_TIME = 60

class SMSNewUnreadSMSAutoSyncTest(bluetooth_sms_base_test.BluetoothSMSBaseTest):

    def setup_class(self):
        super().setup_class()
        self.common_utils = CommonUtils(self.target, self.discoverer)

    def setup_test(self):

        logging.info("Pairing phone to car head unit.")
        self.bt_utils.pair_primary_to_secondary()

        logging.info("wait for user permissions popup & give contacts and sms permissions")
        self.call_utils.wait_with_log(20)
        self.common_utils.click_on_ui_element_with_text('Allow')

        logging.info("Clearing the sms from the phone.")
        self.call_utils.clear_sms_app(self.target)
        self.call_utils.wait_with_log(10)

        logging.info("Rebooting the phone.")
        self.target.unload_snippet('mbs')
        self.target.reboot()
        self.call_utils.wait_with_log(30)
        self.target.load_snippet('mbs', android_device.MBS_PACKAGE)

    def test_new_unread_sms_auto_sync(self):
        # To test that new unread sms appear on HU

        # Open the sms app
        self.call_utils.open_sms_app()

        logging.info("Verifying that there is no existing test sms ")
        asserts.assert_false(self.common_utils.has_ui_element_with_text(constants.SMS_TEXT),
            'Messages app should not contain old test msg, app did not clear.')

        logging.info("Sending the sms from unpaired phone to paired phone")
        target_phone_number = self.target.mbs.getPhoneNumber()
        logging.info(target_phone_number)
        self.phone_notpaired.mbs.sendSms(target_phone_number,constants.SMS_TEXT)
        self.call_utils.wait_with_log(constants.BT_DEFAULT_TIMEOUT)

        logging.info("Performing the verifications")
        asserts.assert_true(self.call_utils.verify_sms_preview_text(constants.SMS_TEXT),
            'Messages app should contain new test message but found none')
        asserts.assert_true(self.call_utils.verify_sms_app_unread_message(),
            'Messages app should have unread badge, but found none')
        asserts.assert_true(self.call_utils.verify_sms_preview_timestamp(),
            'Messages app contain the timestamp')

    def teardown_test(self):
        # Go to home screen
        self.call_utils.press_home()
        self.call_utils.open_sms_app()
        super().teardown_no_video_recording()

if __name__ == '__main__':
    common_main()