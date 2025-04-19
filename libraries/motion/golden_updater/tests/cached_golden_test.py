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
from unittest.mock import patch, call, mock_open
import sys
import os
import json
import hashlib

sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))
from impl.cached_golden import CachedGolden
from test_assets.test_assets import *

class CachedGoldenTest(unittest.TestCase):

  @patch("json.dump")
  @patch("json.load")
  @patch("builtins.open", new_callable=mock_open)
  def test_init(self, mock_open_func, mock_json_load, mock_json_dump):
    remote_file = "remote_file"
    local_file = "local_file"

    mock_file = mock_open_func.return_value
    mock_json_load.return_value = json.loads(remote_file_content)

    cached_golden = CachedGolden(remote_file, local_file)
    mock_json_load.assert_called_once()
    mock_json_dump.assert_called_once_with(local_file_content, mock_file, indent=2)
    mock_open_func.assert_has_calls(
      [
        call(local_file, "r"),
        call(local_file, "w")
      ],
      any_order = True
    )

    metadata = json.loads(remote_file_content)["//metadata"]
    self.assertEqual(
      [cached_golden.id, cached_golden.remote_file, cached_golden.local_file,
       cached_golden.updated, cached_golden.result, cached_golden.golden_repo_path,
       cached_golden.golden_identifier, cached_golden.test_class_name, cached_golden.test_method_name,
       cached_golden.device_local_path, cached_golden.video_location],

      [hashlib.md5(remote_file.encode("utf-8")).hexdigest(), remote_file, local_file,
       False, metadata["result"], metadata["goldenRepoPath"], metadata["goldenIdentifier"],
       metadata["testClassName"], metadata["testMethodName"], metadata["deviceLocalPath"],
       metadata["videoLocation"]]
    )

if __name__ == '__main__':
  unittest.main()