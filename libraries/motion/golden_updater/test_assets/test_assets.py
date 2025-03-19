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

import os

temp_dir = '/tmp/tmp_dir'
atest_latest_dir = '/tmp/atest_result_user/LATEST'
time_hash_value = "time_hash_value"

filenameA = 'dir_name/test_method_1.actual.json_4383267726505225616.txt.gz'
filenameB = 'dir_name/test_method_2.actual_10536896158799342698.json'
filenameC = 'dir_name/test_method_3.actual_118505410949600545.json.gz'
filenameD = 'dir_name/test_method_3.actual_12613191689435798576.mp4'
filenameE = 'dir_name/test_method_2.actual_1988198704080929506.mp4.gz'
filenameF = 'dir_name/test_method_1.actual.mp4_1617964025478041468.txt.gz'
fileNameG = 'dir_name/test_method_1.actual.json'
localFileA = f"{temp_dir}/test_method_1_4383267726505225616.actual.json"
localFileB = f"{temp_dir}/test_method_2_10536896158799342698.actual.json"
localFileC = f"{temp_dir}/test_method_3_118505410949600545.actual.json"
localFileD = f"{temp_dir}/dir_name/test_method_3.actual.mp4"
localFileE = f"{temp_dir}/dir_name/test_method_2.actual.mp4"
localFileF = f"{temp_dir}/dir_name/test_method_1.actual.mp4"
localFileG = f'{temp_dir}/copy_test_method_1.actual_time_hash_value.json'
video_location1 = "dir_name/test_method_1.actual.mp4"
video_location2 = "dir_name/test_method_2.actual.mp4"
video_location3 = "dir_name/test_method_3.actual.mp4"
device_local_path = "data/user/0/platform.test.motion.compose.tests/files/goldens"
MOCK_TOKEN_LOCATION = os.path.expanduser("~/.config/motion-golden/.mock_token")

remote_file_content = """{
  "frame_ids": [
    "before",
    0,
    16,
    32,
    64,
    96,
    144,
    "after"
  ],
  "features": [
    {
      "name": "alpha",
      "type": "float",
      "data_points": [
        0,
        0,
        0,
        0.12842241,
        0.55449593,
        0.81507283,
        0.9581131,
        1
      ]
    }
  ],
  "//metadata": {
    "goldenRepoPath": "dir_name/goldens/test_method.json",
    "goldenIdentifier": "test_method",
    "testClassName": "test_class_name",
    "testMethodName": "test_method",
    "deviceLocalPath": "/data/user/0/platform.test.motion.compose.tests/files/goldens",
    "result": "PASSED",
    "videoLocation": "test_class_name/test_method.actual.mp4"
  }
}"""

local_file_content = {
    "frame_ids": [
    "before",
    0,
    16,
    32,
    64,
    96,
    144,
    "after"
  ],
  "features": [
    {
      "name": "alpha",
      "type": "float",
      "data_points": [
        0,
        0,
        0,
        0.12842241,
        0.55449593,
        0.81507283,
        0.9581131,
        1
      ]
    }
  ]
}

def returnNoneIfOverFlow(list: list, index):
    return list[index] if len(list) > index else None