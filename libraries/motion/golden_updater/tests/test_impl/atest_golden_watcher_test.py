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
from unittest.mock import call, patch
import sys
import os

sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..')))
from test_assets.test_assets import *
from test_assets.fake_cached_golden import FakeCachedGolden
from test_assets.fake_golden import FakeGolden
from impl.golden_watchers.atest_golden_watcher import AtestGoldenWatcher

class AtestGoldenWatcherTest(unittest.TestCase):

  @patch.object(AtestGoldenWatcher, 'refresh_golden_files')
  def test_init(self, _):
    atestWatcher = AtestGoldenWatcher(temp_dir, atest_latest_dir)
    self.assertEqual(atestWatcher.temp_dir, temp_dir)
    self.assertEqual(atestWatcher.atest_latest_dir, atest_latest_dir)
    self.assertEqual(atestWatcher.cached_goldens, {})

  @patch.object(AtestGoldenWatcher, 'refresh_golden_files')
  def test_clean(self, _):
    atestWatcher = AtestGoldenWatcher(temp_dir, atest_latest_dir)
    atestWatcher.cached_goldens = {"a": 1}
    atestWatcher.clean()
    self.assertEqual(atestWatcher.cached_goldens, {})

  @patch('glob.iglob')
  @patch.object(AtestGoldenWatcher, 'copy_file')
  def test_refresh_golden_files_without_videos(self, mock_copy_file, mock_iglob):
    mock_iglob.return_value = [filenameA, filenameB, filenameC]

    fake_cached_golden = FakeCachedGolden()
    golden = FakeGolden()
    fake_cached_golden.goldens.append(golden)
    fake_cached_golden.goldens.append(golden)
    fake_cached_golden.goldens.append(golden)

    atestWatcher = AtestGoldenWatcher(temp_dir, atest_latest_dir, fake_cached_golden)

    mock_iglob.assert_called_once_with(f"{atestWatcher.atest_latest_dir}//**/*.actual*json*", recursive=True)

    calls = [
      call(filenameA, localFileA, True),
      call(filenameB, localFileB, False),
      call(filenameC, localFileC, True)
    ]
    mock_copy_file.assert_has_calls(calls)

    self.assertEqual(
      fake_cached_golden.calls,
      [
        [filenameA, localFileA],
        [filenameB, localFileB],
        [filenameC, localFileC]
      ]
    )

    expected_cached_goldens = {
      filenameA: golden,
      filenameB: golden,
      filenameC: golden
    }
    self.assertEqual(len(fake_cached_golden.calls), 3)
    self.assertEqual(mock_copy_file.call_count, 3)
    self.assertEqual(atestWatcher.cached_goldens, expected_cached_goldens)

  @patch('glob.iglob')
  @patch.object(AtestGoldenWatcher, 'copy_file')
  def test_refresh_golden_files_with_videos(self, mock_copy_file, mock_iglob):
    mock_iglob.side_effect = [[filenameA, filenameB, filenameC], [filenameF], [filenameE], [filenameD]]
    golden1 = FakeGolden(video_location1)
    golden2 = FakeGolden(video_location2)
    golden3 = FakeGolden(video_location3)

    fake_cached_golden = FakeCachedGolden()
    fake_cached_golden.goldens.append(golden1)
    fake_cached_golden.goldens.append(golden2)
    fake_cached_golden.goldens.append(golden3)

    atestWatcher = AtestGoldenWatcher(temp_dir, atest_latest_dir, fake_cached_golden)

    calls = [
      call(f"{atestWatcher.atest_latest_dir}//**/*.actual*json*", recursive=True),
      call(f"{atestWatcher.atest_latest_dir}/**/test_method_1.actual*.mp4*", recursive=True),
      call(f"{atestWatcher.atest_latest_dir}/**/test_method_2.actual*.mp4*", recursive=True),
      call(f"{atestWatcher.atest_latest_dir}/**/test_method_3.actual*.mp4*", recursive=True)
    ]
    mock_iglob.assert_has_calls(calls)

    calls = [
      call(filenameA, localFileA, True),
      call(filenameF, localFileF, True),
      call(filenameB, localFileB, False),
      call(filenameE, localFileE, True),
      call(filenameC, localFileC, True),
      call(filenameD, localFileD, False)
    ]
    mock_copy_file.assert_has_calls(calls)

    self.assertEqual(
      fake_cached_golden.calls,
      [
        [filenameA, localFileA],
        [filenameB, localFileB],
        [filenameC, localFileC]
      ]
    )

    expected_cached_goldens = {
      filenameA: golden1,
      filenameB: golden2,
      filenameC: golden3
    }

    self.assertEqual(mock_iglob.call_count, 4)
    self.assertEqual(len(fake_cached_golden.calls), 3)
    self.assertEqual(mock_copy_file.call_count, 6)
    self.assertEqual(atestWatcher.cached_goldens, expected_cached_goldens)

if __name__ == '__main__':
  unittest.main()