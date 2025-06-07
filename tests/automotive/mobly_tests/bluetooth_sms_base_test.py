"""Bluetooth ThreeDeviceTestBed Base Test

This test class serves as a base class for tests which needs three devices
"""

import logging

from bluetooth_test import bluetooth_base_test
from mobly.controllers import android_device
from utilities import bt_utils
from utilities import spectatio_utils
from utilities.main_utils import common_main
from utilities.video_utils_service import VideoRecording


class BluetoothSMSBaseTest(bluetooth_base_test.BluetoothBaseTest):

  def setup_class(self):
    # Registering android_device controller module and declaring the three devices.
    self.ads = self.register_controller(android_device, min_number=3)

    # The dicoverers is the car head unit.
    self.discoverer = android_device.get_device(self.ads, label='auto')
    self.discoverer.debug_tag = 'discoverer'
    self.discoverer.load_snippet('mbs', android_device.MBS_PACKAGE)

    # The phone device used to connect to the car.
    self.target = android_device.get_device(self.ads, label='phone')
    self.target.debug_tag = 'target'
    self.target.load_snippet('mbs', android_device.MBS_PACKAGE)

    # The extra phone device used to perform actions (make calls, send SMS).
    self.phone_notpaired = android_device.get_device(
        self.ads,
        label='phone_notpaired',
    )
    self.phone_notpaired.debug_tag = 'phone_notpaired'
    self.phone_notpaired.load_snippet('mbs', android_device.MBS_PACKAGE)

    self.call_utils = spectatio_utils.CallUtils(self.discoverer)
    self.bt_utils = bt_utils.BTUtils(self.discoverer, self.target)

    logging.info('Initializing video services')
    self.video_utils_service = VideoRecording(
        self.discoverer,
        self.__class__.__name__,
    )
    self.video_utils_service_target = VideoRecording(
        self.target,
        self.__class__.__name__,
    )
    self.video_utils_service_phone_notpaired = VideoRecording(
        self.phone_notpaired,
        self.__class__.__name__,
    )
    self.call_utils.press_phone_home_icon_using_adb_command(
        self.phone_notpaired
    )

  def hu_recording_handler(self):
    super().hu_recording_handler()
    logging.info("Stopping the screen recording on phone_notpaired")
    self.video_utils_service_phone_notpaired.stop_screen_recording()
    logging.info("Pull the screen recording from phone_notpaired")
    self.video_utils_service_phone_notpaired.pull_recording_file(self.log_path)
    logging.info("delete the screen recording from phone_notpaired")
    self.video_utils_service_phone_notpaired.delete_screen_recording_from_device()

  def enable_recording(self):
    super().enable_recording()
    logging.info("Enabling video recording for phone_notpaired")
    self.video_utils_service_phone_notpaired.enable_screen_recording()
    logging.info("Video recording started on phone_notpaired")

if __name__ == '__main__':
  common_main()
