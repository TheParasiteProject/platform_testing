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
"""
 Test to verify Reply sms sync in IVI device when replied from phone

 Steps include:
        1) Precall state check on IVI and phone devices.
        2) Send sms to paired phone from unpaired phone
        3) Reply to the sms from paired phone
        4) Verify the reply sms sync in IVI device
"""
import logging

from mobly import asserts
from utilities import constants
from utilities.main_utils import common_main
from utilities.common_utils import CommonUtils
from bluetooth_sms_test import bluetooth_sms_base_test
from mobly.controllers import android_device

class SMSReplyFromPhoneSync(bluetooth_sms_base_test.BluetoothSMSBaseTest):

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

        logging.info("Rebooting the phone.")
        self.target.unload_snippet('mbs')
        self.target.reboot()
        self.call_utils.wait_with_log(30)
        self.target.load_snippet('mbs', android_device.MBS_PACKAGE)

    def test_reply_from_phone_sms_sync(self):

        logging.info("Sending the sms from unpaired phone to paired phone")
        target_phone_number = self.target.mbs.getPhoneNumber()
        self.phone_notpaired.mbs.sendSms(target_phone_number,constants.SMS_REPLY_TEXT)
        self.call_utils.wait_with_log(10)

        # Verify the new UNREAD sms in IVI device
        self.call_utils.open_sms_app()
        asserts.assert_true(self.call_utils.verify_sms_preview_text(constants.SMS_REPLY_TEXT),
                    'Messages app should contain new test message but found none')
        asserts.assert_true(self.call_utils.verify_sms_app_unread_message(),
            'Message app should contain an unread test msg, but there are no unread messages')

        # REPLY to the message on paired phone
        self.call_utils.open_notification_on_phone(self.target)
        self.call_utils.wait_with_log(3)
        self.target.mbs.clickUIElementWithText(constants.SMS_REPLY_TEXT)
        self.target.mbs.clickUIElementWithText(constants.REPLY_SMS)
        self.call_utils.wait_with_log(3)

        # Verify the SYNC Reply sms in IVI device
        self.call_utils.press_home()
        self.call_utils.open_sms_app()
        asserts.assert_false(self.call_utils.verify_sms_app_unread_message(),
            'Message app should not contain the unread badge.')
        asserts.assert_true(self.call_utils.verify_sms_preview_text(constants.REPLY_SMS),
            'Messages app should the contain the reply message.')

    def teardown_test(self):
         # Go to home screen
         self.call_utils.press_home()
         super().teardown_no_video_recording()

if __name__ == '__main__':
  common_main()