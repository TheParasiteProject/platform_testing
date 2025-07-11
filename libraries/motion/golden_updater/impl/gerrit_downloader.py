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

import subprocess
from urllib.parse import urlparse, unquote
import base64
import uuid
import json
from enum import Enum
import datetime
from impl.enums import DataSource

class GerritDownloader:
    def __init__(self, subprocess_run_func = subprocess.run):
        self.subprocess_run = subprocess_run_func
        pass

    def download(self, left, right):
        print("Attempting downloads")
        name = self.__getTestName(left) or self.__getTestName(right)
        left_data = self.__download(left)
        right_data = self.__download(right)
        left_json_obj = {}
        right_json_obj = {}

        if left_data:
            try:
                left_json_obj = json.loads(left_data)
            except json.JSONDecodeError as e:
                print(f"Warning: Could not parse left_data string as JSON. Error: {e}")

        if right_data:
            try:
                right_json_obj = json.loads(right_data)
            except json.JSONDecodeError as e:
                print(f"Warning: Could not parse right_data string as JSON. Error: {e}")
        print("All done. Sending data to UI")
        return self.__createMotionGolden(name, left_json_obj, right_json_obj)


    def __download(self, link):
        if not link:
            return
        content_str = "/content"
        download_str = "/download"
        content_link = content_str.join(link.rsplit(download_str, 1))
        cmd_gob_curl = ["gob-curl", content_link]
        try:
            curl_result = self.subprocess_run(
                cmd_gob_curl,
                capture_output=True,
                check=True,
                text=False
            )
            base64_encoded_data_bytes = curl_result.stdout
            print("gob-curl command executed successfully.")
            try:
                return base64.b64decode(base64_encoded_data_bytes)
            except base64.binascii.Error as b64_error:
                print(f"ERROR: Base64 decoding failed: {b64_error}")
                return None
        except Exception as e:
            print(f"An unexpected error occurred during subprocess execution: {e}")
            print("Please ensure gcert is completed")
            return None

    def __createMotionGolden(self, name, leftJson, rightJson):
        golden_list = []
        golden_data = {}
        golden_data["id"] = str(uuid.uuid4())
        golden_data["result"] = "NONE"
        golden_data["label"] = name
        golden_data["goldenRepoPath"] = ""
        golden_data["updated"] = False
        golden_data["testClassName"] = name
        golden_data["testMethodName"] = name
        golden_data["testTime"] = datetime.datetime.now().isoformat()
        golden_data["actualData"] = rightJson
        golden_data["expectedData"] = leftJson
        golden_data["dataSource"] = DataSource.GERRIT.value
        golden_list.append(golden_data)
        return golden_list

    def __getTestName(self, url):
        if not url:
            return None

        try:
            parsedUrl = urlparse(url)
            path =  parsedUrl.path
            normalizedPath = path.strip('/')
            pathSegments = normalizedPath.split('/')
            try:
                fileMarkerIndex = pathSegments.index("files")
            except Exception:
                return ""

            if fileMarkerIndex + 1 < len(pathSegments):
                return unquote(pathSegments[fileMarkerIndex + 1])
        except Exception:
            return ""
