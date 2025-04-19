# Copyright 2025, The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

import unittest
from unittest.mock import patch
import sys
import os

sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..')))
from impl.golden_watchers.golden_watcher_factory import GoldenWatcherFactory
from impl.golden_watchers.golden_watcher_types import GoldenWatcherTypes
from test_assets.fake_adb_client import FakeADBClient

class FakeAtestGoldenWatcher:

    def __init__(self, temp_dir, atest_latest_dir):
        self.temp_dir = temp_dir
        self.atest_latest_dir = atest_latest_dir

class FakeRobolectricGoldenWatcher:

    def __init__(self, temp_dir, latest_dir):
        self.temp_dir = temp_dir
        self.latest_dir = latest_dir

class FakeGoldenFileWatcher:

    def __init__(self, temp_dir, adb_client):
        self.temp_dir = temp_dir
        self.adb_client = adb_client

class GoldenWatcherFactoryTest(unittest.TestCase):

    @patch('impl.golden_watchers.golden_watcher_factory.AtestGoldenWatcher', new=FakeAtestGoldenWatcher)
    def test_create_watcher_creates_AtestGoldenWatcher(self):
        golden_watcher = GoldenWatcherFactory.create_watcher(GoldenWatcherTypes.ATEST, "tmp")
        user = os.environ.get("USER")
        self.assertEqual(golden_watcher.temp_dir, "tmp")
        self.assertEqual(golden_watcher.atest_latest_dir, f"/tmp/atest_result_{user}/LATEST/")

    @patch('impl.golden_watchers.golden_watcher_factory.RobolectricGoldenWatcher', new=FakeRobolectricGoldenWatcher)
    def test_create_watcher_creates_RobolectricGoldenWatcher(self):
        golden_watcher = GoldenWatcherFactory.create_watcher(GoldenWatcherTypes.ROBOLECTRIC, "tmp")
        self.assertEqual(golden_watcher.temp_dir, "tmp")
        self.assertEqual(golden_watcher.latest_dir, f"/tmp/motion/")

    @patch('impl.golden_watchers.golden_watcher_factory.GoldenFileWatcher', new=FakeGoldenFileWatcher)
    def test_create_watcher_creates_GoldenFileWatcher(self):
        fake_adb_client = FakeADBClient()
        golden_watcher = GoldenWatcherFactory.create_watcher(GoldenWatcherTypes.FILE, "tmp", fake_adb_client)
        self.assertEqual(golden_watcher.temp_dir, "tmp")
        self.assertEqual(golden_watcher.adb_client, fake_adb_client)

    def test_create_watcher_fails_creating_GoldenFileWatcher(self):
        with self.assertRaises(ValueError) as e:
            GoldenWatcherFactory.create_watcher(GoldenWatcherTypes.FILE, "tmp")
        self.assertEqual(e.exception.args[0], "adb client not found")

    def test_create_watcher_fails_for_unknown_type(self):
        with self.assertRaises(ValueError) as e:
            GoldenWatcherFactory.create_watcher("Unknown", "tmp")
        self.assertEqual(e.exception.args[0], "Imporper Golden Watcher Type.")

if __name__ == '__main__':
  unittest.main()