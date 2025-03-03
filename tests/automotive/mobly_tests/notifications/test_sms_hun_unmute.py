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

from bluetooth_sms_test import bluetooth_sms_base_test
from mobly.controllers import android_device
from utilities import constants
from utilities.common_utils import CommonUtils
from utilities.faker import fake
from utilities.main_utils import common_main


class NotificationsSmsHunUnmute(
    bluetooth_sms_base_test.BluetoothSMSBaseTest
):

  def setup_class(self):
    super().setup_class()
    self.common_utils = CommonUtils(self.target, self.discoverer)

  def setup_test(self):
    logging.info("Pairing phone to car head unit.")
    self.bt_utils.pair_primary_to_secondary()
    self.call_utils.wait_with_log(constants.DEVICE_CONNECT_WAIT_TIME)
    self.common_utils.click_on_ui_element_with_text("Allow")

    logging.info("Clearing the sms from the phone.")
    self.call_utils.clear_sms_app(self.target)

    logging.info("Removing mbs snippet and rebooting the phone.")
    self.target.unload_snippet('mbs')
    self.target.reboot()
    self.call_utils.wait_with_log(constants.DEVICE_CONNECT_WAIT_TIME)
    self.target.load_snippet('mbs', android_device.MBS_PACKAGE)
    super().enable_recording()

  def test_sms_hun_unmute(self):
    """
    GIVEN a SMS HUN is displayed in the car's head unit,
    WHEN the SMS HUN is muted,
    THEN the new SMS HUN should not be displayed in the car's head unit for this conversation,
    AND the SMS HUN should be be displayed in notification center in the car's head unit.
    WHEN the SMS HUN is unmuted in SMS app in the car's head unit,
    THEN the new SMS HUN should be displayed in the car's head unit for this conversation.
    AND the SMS HUN should be be displayed in notification center in the car's head unit.
    """
    logging.info("Arrange: Get the phone number of phone to send the SMS.")
    receiver_phone_number = self.target.mbs.getPhoneNumber()
    sender_phone_number = self.phone_notpaired.mbs.getPhoneNumber()
    sms_text = fake.string

    logging.info(f"Act: Sending new SMS to {receiver_phone_number}")
    self.phone_notpaired.mbs.sendSms(receiver_phone_number, sms_text)

    logging.info("Assert: New SMS is displayed as a heads-up notification.")
    assert self.discoverer.mbs.isSmsHunDisplayed() is True, (
        "New SMS is not displayed as a heads-up notification."
    )
    assert self.discoverer.mbs.isSmsHunDisplayedWithTitle(sender_phone_number) is True, (
        "New SMS is not displayed as a heads-up notification with the correct title."
    )

    logging.info("Act: Mute the SMS in the car's head unit.")
    self.discoverer.mbs.muteSmsHun(sender_phone_number)

    logging.info("Assert: SMS HUN is dismissed.")
    assert self.discoverer.mbs.isSmsHunDisplayedWithTitle(sender_phone_number) is False, (
        "SMS HUN is not dismissed after mute."
    )

    logging.info("Assert: SMS is not displayed in notification center.")
    self.discoverer.mbs.waitForHunToDisappear()
    assert self.discoverer.mbs.isNotificationDisplayedInCenterWithTitle(sender_phone_number) is False, (
        "SMS is still displayed in the notification center after mute."
    )

    logging.info(f"Act: Unmute the conversation from the SMS app in the car's head unit with title {sender_phone_number}.")
    self.discoverer.mbs.unmuteConversationWithTitle(sender_phone_number)

    logging.info(f"Act: Sending new SMS to {receiver_phone_number}")
    self.phone_notpaired.mbs.sendSms(receiver_phone_number, sms_text + "2")

    logging.info("Assert: New SMS is displayed as a heads-up notification.")
    assert self.discoverer.mbs.isSmsHunDisplayedWithTitle(sender_phone_number) is True, (
        "New SMS is not displayed as a heads-up notification."
    )

    logging.info("Assert: New SMS is displayed in notification center.")
    self.discoverer.mbs.waitForHunToDisappear()
    assert self.discoverer.mbs.isNotificationDisplayedInCenterWithTitle(sender_phone_number) is True, (
        "New SMS is not displayed in the notification center."
    )

  def teardown_test(self):
    self.call_utils.press_home()

    try:
      super().teardown_test()
    except Exception as e:  # pylint: disable=broad-except
      logging.info("Failed to teardown test: %s", e)


if __name__ == "__main__":
  common_main()
