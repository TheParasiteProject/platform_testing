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
from unittest.mock import patch, Mock, call
import sys
import os

sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..')))
from impl.golden_watchers.robolectric_golden_watcher import RobolectricGoldenWatcher
from test_assets.test_assets import *
from test_assets.fake_cached_golden import FakeCachedGolden
from test_assets.fake_golden import FakeGolden

class RobolectricGoldenWatcherTest(unittest.TestCase):

  @patch.object(RobolectricGoldenWatcher, 'refresh_golden_files')
  def test_init(self, mock_refresh_golden_files):
    watcher = RobolectricGoldenWatcher(temp_dir, atest_latest_dir)
    mock_refresh_golden_files.assert_called_once()
    self.assertEqual(watcher.temp_dir, temp_dir)
    self.assertEqual(watcher.latest_dir, atest_latest_dir)
    self.assertEqual(watcher.cached_goldens, {})

  @patch('glob.iglob')
  @patch.object(RobolectricGoldenWatcher, 'copy_file')
  @patch('hashlib.md5')
  def test_refresh_golden_files(self, mock_hash_func, mock_copy_file, mock_iglob):
    mock_iglob.return_value = [fileNameG]
    mock_hash = Mock()
    mock_hash_func.return_value = mock_hash
    mock_hash.hexdigest.return_value = time_hash_value
    fake_cached_golden = FakeCachedGolden()
    golden=FakeGolden()
    fake_cached_golden.goldens.append(golden)

    #Not invoking refresh_golden_files explicitly as it is being called in the constructor
    watcher = RobolectricGoldenWatcher(temp_dir, atest_latest_dir, fake_cached_golden)

    mock_iglob.assert_called_once_with(f"{watcher.latest_dir}//**/*.actual.json", recursive=True)
    mock_hash_func.assert_called_once()
    mock_copy_file.assert_called_once_with(fileNameG, localFileG)
    self.assertEqual(
      fake_cached_golden.calls,
      [[fileNameG, localFileG]]
    )
    self.assertEqual(watcher.cached_goldens[fileNameG], golden)

  @patch('glob.iglob')
  @patch.object(RobolectricGoldenWatcher, 'copy_file')
  @patch('hashlib.md5')
  @patch('os.path.isfile')
  def test_refresh_golden_files_with_video_file(self, mock_is_file, mock_hash_func, mock_copy_file, mock_iglob):
    mock_is_file.return_value = True
    mock_iglob.return_value = [fileNameG]
    mock_hash = Mock()
    mock_hash_func.return_value = mock_hash
    mock_hash.hexdigest.return_value = time_hash_value
    fake_cached_golden = FakeCachedGolden()
    golden=FakeGolden(video_location3, test_class_name=test_class_name)
    fake_cached_golden.goldens.append(golden)

    #Not invoking refresh_golden_files explicitly as it is being called in the constructor
    watcher = RobolectricGoldenWatcher(temp_dir, atest_latest_dir, fake_cached_golden)

    mock_iglob.assert_called_once_with(f"{atest_latest_dir}//**/*.actual.json", recursive=True)
    mock_hash_func.assert_called_once()

    mock_copy_file.assert_has_calls(
      [
        call(fileNameG, localFileG),
        call(robo_video_file_path, f"{temp_dir}/{video_location3}")
      ]
    )
    self.assertEqual(
      fake_cached_golden.calls,
      [[fileNameG, localFileG]]
    )
    self.assertEqual(watcher.cached_goldens[fileNameG], golden)
    mock_is_file.assert_called_once_with(robo_video_file_path)


  @patch('shutil.copyfile')
  @patch.object(RobolectricGoldenWatcher, 'refresh_golden_files')
  def test_copy_file(self, _, mock_copy):
    sourceFile = 'sourceFile.json'
    targetFile = '/tmp/LATEST/targetFile.json'

    studioWatcher = RobolectricGoldenWatcher(temp_dir, atest_latest_dir)
    studioWatcher.copy_file(sourceFile, targetFile)

    self.assertTrue(os.path.isdir('/tmp/LATEST/'))
    mock_copy.assert_called_once_with(sourceFile, targetFile)

  @patch.object(RobolectricGoldenWatcher, 'refresh_golden_files')
  def test_clean(self, _):
    studioWatcher = RobolectricGoldenWatcher(temp_dir, atest_latest_dir)
    studioWatcher.cached_goldens = {"a": 1}
    studioWatcher.clean()
    self.assertEqual(studioWatcher.cached_goldens, {})

if __name__ == '__main__':
  unittest.main()