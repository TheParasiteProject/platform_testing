#  Copyright (C) 2024 The Android Open Source Project
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

"""Mobly base test class for Neaby Connections.

Override the NCBaseTestClass#_get_country_code method if the test requires
a special country code, the 'US' is used by default.
"""

import logging
import os
import time

from mobly import asserts
from mobly import base_test
from mobly import records
from mobly import utils
from mobly.controllers import android_device
from mobly.controllers.android_device_lib import errors
import yaml

from betocq import android_wifi_utils
from betocq import nc_constants
from betocq import setup_utils

NEARBY_SNIPPET_PACKAGE_NAME = 'com.google.android.nearby.mobly.snippet'
NEARBY_SNIPPET_2_PACKAGE_NAME = 'com.google.android.nearby.mobly.snippet.second'

_CONFIG_EXTERNAL_PATH = 'TBD'


class NCBaseTestClass(base_test.BaseTestClass):
  """The Base of Nearby Connection E2E tests."""

  def __init__(self, configs):
    super().__init__(configs)
    self.ads: list[android_device.AndroidDevice] = []
    self.advertiser: android_device.AndroidDevice = None
    self.discoverer: android_device.AndroidDevice = None
    self.test_parameters: nc_constants.TestParameters = (
        nc_constants.TestParameters.from_user_params(self.user_params)
    )
    self._nearby_snippet_apk_path: str = None
    self._nearby_snippet_2_apk_path: str = None
    self.performance_test_iterations: int = 1
    self.num_bug_reports: int = 0
    self._requires_2_snippet_apks = False
    self.__loaded_2_nearby_snippets = False
    self.__skipped_test_class = False

  def _get_skipped_test_class_reason(self) -> str | None:
    return None

  def setup_class(self) -> None:
    self._setup_openwrt_wifi()
    self.ads = self.register_controller(android_device, min_number=2)
    try:
      self.discoverer = android_device.get_device(
          self.ads, role='source_device'
      )
      self.advertiser = android_device.get_device(
          self.ads, role='target_device'
      )
    except errors.Error:
      logging.warning(
          'The source,target devices are not specified in testbed;'
          'The result may not be expected.'
      )
      self.advertiser, self.discoverer = self.ads

    utils.concurrent_exec(
        self._setup_android_hw_capability,
        param_list=[[ad] for ad in self.ads],
        raise_on_exception=True,
    )

    skipped_test_class_reason = self._get_skipped_test_class_reason()
    if skipped_test_class_reason:
      self.__skipped_test_class = True
      asserts.abort_class(skipped_test_class_reason)

    file_tag = 'files' if 'files' in self.user_params else 'mh_files'
    self._nearby_snippet_apk_path = self.user_params.get(file_tag, {}).get(
        'nearby_snippet', ['']
    )[0]
    if self.test_parameters.requires_bt_multiplex:
      self._requires_2_snippet_apks = True
      self._nearby_snippet_2_apk_path = self.user_params.get(file_tag, {}).get(
          'nearby_snippet_2', ['']
      )[0]

    # disconnect from all wifi automatically
    utils.concurrent_exec(
        android_wifi_utils.forget_all_wifi,
        param_list=[[ad] for ad in self.ads],
        raise_on_exception=True,
    )

    utils.concurrent_exec(
        self._setup_android_device,
        param_list=[[ad] for ad in self.ads],
        raise_on_exception=True,
    )

  def _setup_openwrt_wifi(self):
    """Sets up the wifi connection with OpenWRT."""
    if not self.user_params.get('use_auto_controlled_wifi_ap', False):
      return

    # TODO(xianyuanjia): Add OpenWRT setup logic here
    return

  def _setup_android_hw_capability(
      self, ad: android_device.AndroidDevice
  ) -> None:
    ad.android_version = int(ad.adb.getprop('ro.build.version.release'))

    if not os.path.isfile(_CONFIG_EXTERNAL_PATH):
      return

    config_path = _CONFIG_EXTERNAL_PATH
    with open(config_path, 'r') as f:
      rule = yaml.safe_load(f).get(ad.model, None)
      asserts.assert_is_not_none(
          rule, f'{ad} Model {ad.model} is not supported in config file'
      )
      for key, value in rule.items():
        setattr(ad, key, value)

  def _get_country_code(self) -> str:
    return 'US'

  def _setup_android_device(self, ad: android_device.AndroidDevice) -> None:
    ad.debug_tag = ad.serial + '(' + ad.adb.getprop('ro.product.model') + ')'
    if not ad.is_adb_root:
      if self.test_parameters.allow_unrooted_device:
        ad.log.info('Unrooted device is detected. Test coverage is limited')
      else:
        asserts.abort_all('The test only can run on rooted device.')

    setup_utils.disable_gms_auto_updates(ad)

    ad.debug_tag = ad.serial + '(' + ad.adb.getprop('ro.product.model') + ')'
    ad.log.info('try to install nearby_snippet_apk')
    if self._nearby_snippet_apk_path:
      setup_utils.install_apk(ad, self._nearby_snippet_apk_path)
    else:
      ad.log.warning(
          'nearby_snippet apk is not specified, '
          'make sure it is installed in the device'
      )

    ad.log.info('grant manage external storage permission')
    setup_utils.grant_manage_external_storage_permission(
        ad, NEARBY_SNIPPET_PACKAGE_NAME
    )
    ad.load_snippet('nearby', NEARBY_SNIPPET_PACKAGE_NAME)

    if self._requires_2_snippet_apks:
      ad.log.info('try to install nearby_snippet_2_apk')
      if self._nearby_snippet_2_apk_path:
        setup_utils.install_apk(ad, self._nearby_snippet_2_apk_path)
      else:
        ad.log.warning(
            'nearby_snippet_2 apk is not specified, '
            'make sure it is installed in the device'
        )
      setup_utils.grant_manage_external_storage_permission(
          ad, NEARBY_SNIPPET_2_PACKAGE_NAME
      )
      setup_utils.enable_bluetooth_multiplex(ad)
      ad.load_snippet('nearby2', NEARBY_SNIPPET_2_PACKAGE_NAME)
      self.__loaded_2_nearby_snippets = True
    if not ad.nearby.wifiIsEnabled():
      ad.nearby.wifiEnable()
    setup_utils.disconnect_from_wifi(ad)
    setup_utils.enable_logs(ad)

    setup_utils.disable_redaction(ad)

    setup_utils.set_country_code(ad, self._get_country_code())

  def setup_test(self):
    self.record_data({
        'Test Name': self.current_test_info.name,
        'sponge_properties': {
            'beto_team': 'Nearby Connections',
            'beto_feature': 'Nearby Connections',
        },
    })
    self._reset_nearby_connection()

  def _reset_wifi_connection(self) -> None:
    """Resets wifi connections on both devices."""
    self.discoverer.nearby.wifiClearConfiguredNetworks()
    self.advertiser.nearby.wifiClearConfiguredNetworks()
    time.sleep(nc_constants.WIFI_DISCONNECTION_DELAY.total_seconds())

  def _reset_nearby_connection(self) -> None:
    """Resets nearby connection."""
    self.discoverer.nearby.stopDiscovery()
    self.discoverer.nearby.stopAllEndpoints()
    self.advertiser.nearby.stopAdvertising()
    self.advertiser.nearby.stopAllEndpoints()
    if self.__loaded_2_nearby_snippets:
      self.discoverer.nearby2.stopDiscovery()
      self.discoverer.nearby2.stopAllEndpoints()
      self.advertiser.nearby2.stopAdvertising()
      self.advertiser.nearby2.stopAllEndpoints()
    time.sleep(nc_constants.NEARBY_RESET_WAIT_TIME.total_seconds())

  def _teardown_device(self, ad: android_device.AndroidDevice) -> None:
    ad.nearby.transferFilesCleanup()
    setup_utils.enable_gms_auto_updates(ad)

    if self.test_parameters.disconnect_wifi_after_test:
      setup_utils.disconnect_from_wifi(ad)

    ad.unload_snippet('nearby')
    if self.__loaded_2_nearby_snippets:
      ad.unload_snippet('nearby2')

  def teardown_test(self) -> None:
    utils.concurrent_exec(
        lambda d: d.services.create_output_excerpts_all(self.current_test_info),
        param_list=[[ad] for ad in self.ads],
        raise_on_exception=True,
    )

  def teardown_class(self) -> None:
    if self.__skipped_test_class:
      logging.info('Skipping teardown class.')
      return

    # handle summary results
    self._summary_test_results()

    utils.concurrent_exec(
        self._teardown_device,
        param_list=[[ad] for ad in self.ads],
        raise_on_exception=True,
    )

    if hasattr(self, 'openwrt') and hasattr(self, 'wifi_info'):
      self.openwrt.stop_wifi(self.wifi_info)

  def _summary_test_results(self) -> None:
    pass

  def on_fail(self, record: records.TestResultRecord) -> None:
    if self.__skipped_test_class:
      logging.info('Skipping on_fail.')
      return
    self.num_bug_reports = self.num_bug_reports + 1
    if self.num_bug_reports <= nc_constants.MAX_NUM_BUG_REPORT:
      logging.info('take bug report for failure')
      android_device.take_bug_reports(
          self.ads,
          destination=self.current_test_info.output_path,
      )