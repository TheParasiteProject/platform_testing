"""Library for statsd metrics collection."""

import dataclasses
import pathlib
import random
from typing import Any

import stats_log_pb2
from mobly.controllers import android_device
from mobly.controllers.android_device_lib.services import base_service

from google.protobuf import json_format


@dataclasses.dataclass
class StatsdData:
  """Data collected by statsd.

  Includes metadata to allow convenient export and visualization.

  Attributes:
    uid: str, the UID associated with the config used to collect the metrics.
    config_name: str, the name of the config used to collect the metrics.
    data: dict, key-value pairs of the collected metrics.
    device_info: dict, key-value pairs of the device's info, such as serial
      number and model.
  """

  uid: str
  config_name: str
  data: dict[str, Any]
  device_info: dict[str, Any]


@dataclasses.dataclass
class StatsdCollectorConfig:
  """Config for the StatsdCollector."""

  # Keeps statsd configs active even when stop() is called, e.g. during device
  # reboot. If True, users must manually stop collection with
  # remove_all_configs() upon test end.
  configs_persist: bool = False


class StatsdCollector(base_service.BaseService):
  """Collector for statsd.

  Manages the collection configs and the associated UIDs for a particular
  device's statsd collection lifecycle.
  """

  BLUETOOTH_CONNECTION_LATENCY_METRIC_KEY = "bluetooth_connection_latency"

  BLUETOOTH_PROFILES_MAP = {
      1 : "headset",
      2 : "a2dp",
      5 : "pan",
      9 : "map",
      11 : "a2dp_sink",
      12 : "avrcp_controller",
      16 : "headset_client",
      17 : "pbap_client",
      18 : "map_client"
  }

  BLUETOOTH_CONNECTION_STATE_MAP = {
      "CONNECTION_STATE_DISCONNECTED" : 0,
      "CONNECTION_STATE_CONNECTING" : 1,
      "CONNECTION_STATE_CONNECTED" : 2,
      "CONNECTION_STATE_DISCONNECTING" : 3,
  }

  def __init__(
      self,
      device: android_device.AndroidDevice,
      configs: StatsdCollectorConfig | None = None,
  ) -> None:
    """Constructor for the collector class.

    Args:
      device: The device to collect from.
      configs: Optional configuration for the collector.
    """
    super().__init__(device, configs)
    self._collector_config = self._configs or StatsdCollectorConfig()
    self._config_names = {}

  def _run_stats_cmd(self, args: list[str]) -> bytes:
    """Runs the specified `cmd stats` command.

    Args:
      args: Input to 'adb shell cmd stats'.

    Returns:
      Stdout of the command.
    """
    return self._device.adb.shell(['cmd', 'stats', *args])

  def _dump_report(
      self, uid: str, include_current_bucket: bool = True
  ) -> bytes:
    """Runs the command to dump the metrics report for the given UID.

    Args:
      uid: The uid of the config to dump the metrics report.
      include_current_bucket: If true then includes the current bucket data.

    Returns:
      The dumped metrics report as bytestring.
    """
    flag = '--include_current_bucket' if include_current_bucket else ''
    return self._run_stats_cmd(['dump-report', uid, flag, '--proto'])

  def _assert_uid_exists(self, uid: str) -> None:
    """Verify that a statsd config exists for the given UID.

    Args:
      uid: The checked UID.

    Raises:
      RuntimeError: if the given UID does not exist in this collector.
    """
    if uid not in self._config_names:
      raise RuntimeError('No statsd config found for the given UID.')

  @property
  def is_alive(self) -> bool:
    """Alive when at least one config is active."""
    return bool(self._config_names)

  def add_config(self, config_path: str, config_name: str | None = None) -> str:
    """Adds a configuration (as a binary proto file) to the stats service.

    This generates a UID and adds the given config to the device's stats service
    under this UID. This collector keeps track of all the added configs as well.

    Args:
      config_path: Path to a proto that defines a metric collection.
      config_name: Custom name to specify the given config. Defaults to the
        config's filename (without extensions).

    Returns:
      The UID of the added config.
    """
    # Generate a new UID
    uid = str(random.randint(1, int(1e10)))

    self._config_names[uid] = None
    self.update_config(uid, config_path, config_name)
    return uid

  def update_config(
      self, uid: str, config_path: str, config_name: str | None = None
  ) -> None:
    """Updates a config that's already added.

    Args:
      uid: The uid of the metric config to update.
      config_path: Path to a proto that defines a metric collection.
      config_name: Custom name to specify the given config. Defaults to the
        config's filename (without extensions).

    Raises:
      RuntimeError: if the given UID does not exist in this collector.
    """
    self._assert_uid_exists(uid)

    self._config_names[uid] = config_name or pathlib.Path(config_path).stem

    update_cmd = [
        "cat",
        config_path,
        "|",
        "cmd",
        "stats",
        "config",
        "update",
        uid,
    ]
    self._device.adb.shell(" ".join(update_cmd))

    self._device.log.debug(
        'Added/updated statsd config %s at UID=%s.'
        % (self._config_names[uid], uid)
    )

  def remove_all_configs(self) -> None:
    """Removes all added configs from the stats service."""
    self._run_stats_cmd(['config', 'remove'])
    self._config_names.clear()
    self._device.log.debug('Removed all existing statsd configs.')

  def get_metrics_report_proto(
      self, uid: str, include_current_bucket: bool = True
  ) -> stats_log_pb2.ConfigMetricsReportList:
    """Gets the metrics (as proto) collected by the config with the given UID.

    Note: This operation will clear the collected data from device.

    Args:
      uid: The UID of the config to get metrics for.
      include_current_bucket: If true then includes the current bucket data.

    Returns:
      The dumped stats_log_pb2.ConfigMetricsReportList.

    Raises:
      RuntimeError: if the given UID does not exist in this collector.
    """
    self._assert_uid_exists(uid)

    self._device.log.debug(
        'Collecting statsd metrics for %s at UID=%s.'
        % (self._config_names[uid], uid)
    )

    report_list = stats_log_pb2.ConfigMetricsReportList()
    report_list.ParseFromString(
        self._dump_report(uid, include_current_bucket=include_current_bucket)
    )
    return report_list

  def get_metrics(
      self, uid: str, include_current_bucket: bool = True
  ) -> StatsdData:
    """Gets the metrics (as dict) collected by the config with the given UID.

    The metrics will be in a StatsdData object, which also contains other
    metadata associated with the collection.

    Note: This operation will clear the collected data from device.

    Args:
      uid: The UID of the config to get metrics for.
      include_current_bucket: If true then includes the current bucket data.

    Returns:
      StatsdData representing the metrics collected so far.

    Raises:
      RuntimeError: if the given UID does not exist in this collector.
    """
    metrics_proto = self.get_metrics_report_proto(
        uid, include_current_bucket=include_current_bucket
    )
    data = json_format.MessageToDict(
        metrics_proto, preserving_proto_field_name=True
    )
    return StatsdData(
        uid, self._config_names[uid], data, self._device.device_info
    )

  def process_bt_connection_time_stats_report(self, data: dict[str, Any]):
    reports = data["reports"]
    if len(reports) == 0:
      self._device.log.debug('No stats report is collected')
      return
    report = reports[0]
    if len(report["metrics"]) == 0:
      self._device.log.debug('No metrics collected in stats report')
      return
    duration_data = report["metrics"][0]["duration_metrics"]
    if len(duration_data["data"]) == 0:
      self._device.log.debug('No duration data collected')
      return
    result = {}
    for metric in duration_data["data"]:
      total_duration_nanos = 0
      for bucket in metric["bucket_info"]:
        total_duration_nanos += int(bucket["duration_nanos"])
      bluetooth_profile = metric["dimension_leaf_values_in_what"][0]["value_int"]
      metric_key_items = [
          self.BLUETOOTH_CONNECTION_LATENCY_METRIC_KEY,
          "profile",
          str(bluetooth_profile),
          "ms"
      ]
      metric_key = "_".join(metric_key_items)
      if bluetooth_profile in self.BLUETOOTH_PROFILES_MAP:
        metric_key_items = [
            self.BLUETOOTH_CONNECTION_LATENCY_METRIC_KEY,
            self.BLUETOOTH_PROFILES_MAP[bluetooth_profile],
            "ms"
        ]
        metric_key = "_".join(metric_key_items)
      duration_ms = total_duration_nanos / 1000000
      self._device.log.debug('Processed metric on device %s with key: %s, value %d' % (self._device.serial, metric_key, duration_ms))
      result[metric_key] = duration_ms
    return result

  def process_bluetooth_event_metrics_stats_report(self, data: dict[str, Any]):
    reports = data["reports"]
    if len(reports) == 0:
      self._device.log.debug('No stats report is collected')
      return
    report = reports[0]
    if len(report["metrics"]) == 0:
      self._device.log.debug('No metrics collected in stats report')
      return
    event_metrics = report["metrics"][0]["event_metrics"]
    bt_profile_states = {}
    result = {}
    for metric in event_metrics["data"]:
      atom = metric["atom"] if "atom" in metric else metric["aggregated_atom_info"]["atom"]
      if "bluetooth_connection_state_changed" not in atom:
        self._device.log.debug('Atom does not have a bluetooth_connection_state_changed info. Skipping reporting')
        return
      bluetooth_connection_state_changed = atom["bluetooth_connection_state_changed"]
      bt_state = self.BLUETOOTH_CONNECTION_STATE_MAP[bluetooth_connection_state_changed["state"]]
      bt_profile = bluetooth_connection_state_changed["bt_profile"]
      self._device.log.debug('Processing connection state changed atom on device %s for profile number %d' % (self._device.serial, bt_profile))
      if bt_profile in self.BLUETOOTH_PROFILES_MAP:
        bt_profile_name = self.BLUETOOTH_PROFILES_MAP[bt_profile]
        states = bt_profile_states[bt_profile_name] if bt_profile_name in bt_profile_states else []
        states.append(bt_state)
        bt_profile_states[bt_profile_name] = states
        self._device.log.debug('Processed connection state changed atom on device %s profile %s value %d' % (self._device.serial, bt_profile_name, bt_state))
    for key in bt_profile_states:
      states = bt_profile_states[key]
      metric_key_items = [
          key,
          "connection_state_changed"
      ]
      metric_key = "_".join(metric_key_items)
      self._device.log.debug('Adding metric on device %s with key %s and values %s' % (self._device.serial, metric_key, str(states)))
      result[metric_key] = states
    return result

  def stop(self) -> None:
    """Stops the service and cleans up all resources.

    If config_persist is enabled, skips the cleanup process.
    """
    if self._collector_config.configs_persist:
      return
    self.remove_all_configs()