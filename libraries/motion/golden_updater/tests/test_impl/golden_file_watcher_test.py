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
from unittest.mock import patch, call
import sys
import os
import hashlib

sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..')))
from impl.golden_watchers.golden_file_watcher import GoldenFileWatcher
from test_assets.test_assets import *
from test_assets.fake_cached_golden import FakeCachedGolden
from test_assets.fake_golden import FakeGolden
from test_assets.fake_adb_client import FakeADBClient

class GoldenFileWatcherTest(unittest.TestCase):

  @patch.object(GoldenFileWatcher, 'refresh_golden_files')
  def test_init(self, mock_refresh_golden_files):
    fake_adb_client = FakeADBClient()
    watcher = GoldenFileWatcher(temp_dir, fake_adb_client)

    mock_refresh_golden_files.assert_called_once()
    self.assertEqual(watcher.temp_dir, temp_dir)
    self.assertEqual(watcher.adb_client, fake_adb_client)
    self.assertEqual(watcher.cached_goldens, {})

  @patch.object(GoldenFileWatcher, 'refresh_golden_files')
  def test_clean(self, _):
    fake_adb_client = FakeADBClient()
    goldenWatcher = GoldenFileWatcher(temp_dir, fake_adb_client)
    goldenWatcher.cached_goldens = {"a": 1}

    goldenWatcher.clean()

    self.assertEqual(goldenWatcher.cached_goldens, {})

  @patch.object(GoldenFileWatcher, 'adb_pull')
  @patch.object(GoldenFileWatcher, 'adb_pull_image')
  def test_refresh_golden_files(self, mock_pull_image, mock_pull):
    fake_adb_client = FakeADBClient()
    fake_adb_client.run_adb_command_result.append(f'{filenameA}\n{filenameB}')

    mock_pull.side_effect = [localFileA, localFileB]
    fake_cached_golden = FakeCachedGolden()

    golden1=FakeGolden(video_location1, device_local_path)
    golden2=FakeGolden()
    fake_cached_golden.goldens.append(golden1)
    fake_cached_golden.goldens.append(golden2)

    #Not invoking refresh_golden_files explicitly as it is being called in the constructor
    goldenWatcher = GoldenFileWatcher(temp_dir, fake_adb_client, fake_cached_golden)

    self.assertEqual(fake_adb_client.run_adb_command_calls, [["shell", f"find /data/user/0/ -type f -name *.actual.json"]])
    mock_pull.assert_has_calls(
      [
        call(filenameA),
        call(filenameB)
      ]
    )
    self.assertEqual(
        fake_cached_golden.calls,
        [
            [filenameA, localFileA],
            [filenameB, localFileB]
        ]
    )
    mock_pull_image.assert_called_once_with(device_local_path, video_location1)

    self.assertEqual(goldenWatcher.cached_goldens, {
      filenameA: golden1,
      filenameB: golden2
    })

  @patch.object(GoldenFileWatcher, 'adb_pull')
  @patch.object(GoldenFileWatcher, 'adb_pull_image')
  def test_refresh_golden_files_no_files_found(self, mock_pull_image, mock_pull):
    fake_adb_client = FakeADBClient()
    fake_adb_client.run_adb_command_result.append('')
    fake_cached_golden = FakeCachedGolden()

    #Not invoking refresh_golden_files explicitly as it is being called in the constructor
    goldenWatcher = GoldenFileWatcher(temp_dir, fake_adb_client, fake_cached_golden)

    self.assertEqual(fake_adb_client.run_adb_command_calls, [["shell", f"find /data/user/0/ -type f -name *.actual.json"]])
    mock_pull.assert_not_called()
    self.assertEqual(fake_cached_golden.calls, [])
    mock_pull_image.assert_not_called()
    self.assertEqual(goldenWatcher.cached_goldens, {})

  @patch.object(GoldenFileWatcher, 'refresh_golden_files')
  def test_adb_pull(self, _):
    remote_file = "remote_file.json"
    remote_filename_hash = hashlib.md5(remote_file.encode("utf-8")).hexdigest()
    local_file = f'/tmp/tmp_dir/remote_file_{remote_filename_hash}.json'
    fake_adb_client = FakeADBClient()
    fake_cached_golden = FakeCachedGolden()

    goldenWatcher = GoldenFileWatcher(temp_dir, fake_adb_client, fake_cached_golden)
    result = goldenWatcher.adb_pull(remote_file)

    self.assertEqual(
        fake_adb_client.run_adb_command_calls,
        [
            ["pull", remote_file, local_file],
            ["shell", "rm", remote_file]
        ]
    )
    self.assertEqual(result, local_file)

  @patch.object(GoldenFileWatcher, 'refresh_golden_files')
  def test_adb_pull_image(self, _):
    fake_adb_client = FakeADBClient()
    fake_cached_golden = FakeCachedGolden()
    goldenWatcher = GoldenFileWatcher(temp_dir, fake_adb_client, fake_cached_golden)
    result = goldenWatcher.adb_pull_image(device_local_path, video_location1)

    remote_path = f'{device_local_path}/{video_location1}'
    local_path = f'/tmp/tmp_dir/{video_location1}'

    self.assertTrue(os.path.isdir(os.path.dirname(local_path)))
    self.assertEqual(
        fake_adb_client.run_adb_command_calls,
      [
        ["pull", remote_path, local_path],
        ["shell", "rm", remote_path]
      ]
    )
    self.assertEqual(result, local_path)

if __name__ == '__main__':
  unittest.main()