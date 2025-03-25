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


class NotificationsSmsHunMultipleConversations(
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

  def test_sms_hun_multiple_conversations(self):
    """
    GIVEN the phone which is paired to the car,
    WHEN multiple SMS are sent to paired phone,
    THEN all SMS clubbed in one notification in notification center,
    AND notification conversation collapsible and displaying all conversations.
    """
    receiver_phone_number = self.target.mbs.getPhoneNumber()
    sender_phone_number = self.phone_notpaired.mbs.getPhoneNumber()
    number_of_sms = 3
    sms_texts = [fake.string for _ in range(number_of_sms)]

    logging.info(f"Act: Sending multiple SMS to {receiver_phone_number}")
    for sms_text in sms_texts:
      self.phone_notpaired.mbs.sendSms(receiver_phone_number, sms_text)
      assert self.discoverer.mbs.isSmsHunDisplayedWithTitle(sender_phone_number) is True, (
        "New SMS is not displayed as a heads-up notification with the correct title."
      )
      self.discoverer.mbs.waitForHunToDisappear()

    logging.info("Assert: SMS is displayed in the notification center on the car.")
    assert self.discoverer.mbs.isNotificationDisplayedInCenterWithTitle(sender_phone_number) is True, (
      "New SMS is not displayed in the notification center."
    )

    logging.info("Assert: SMS notification count is correct.")
    logging.info(f"SMS notification count: {self.discoverer.mbs.getSmsNotificationCount(sender_phone_number)}")
    assert self.discoverer.mbs.getSmsNotificationCount(sender_phone_number) == number_of_sms - 1, (
        "New SMS notification count is not correct."
    )

    logging.info("Assert: SMS notification is collapsible.")
    sms_content = self.discoverer.mbs.getSmsNotificationContent(sender_phone_number)
    assert sms_content.split() == sms_texts[::-1], (
        f"SMS notification content is not correct. Expected: {sms_texts[::-1]}, Actual: {sms_content}"
    )

  def teardown_test(self):
    self.call_utils.press_home()

    try:
      super().teardown_test()
    except Exception as e:  # pylint: disable=broad-except
      logging.info("Failed to teardown test: %s", e)


if __name__ == "__main__":
  common_main()
