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
from unittest.mock import patch, MagicMock, call
import json
import base64
import subprocess
from impl.gerrit_downloader import GerritDownloader

class TestGerritDownloader(unittest.TestCase):

    def setUp(self):
        self.mock_subprocess_run = MagicMock()
        self.downloader = GerritDownloader(subprocess_run_func=self.mock_subprocess_run)
        self.fake_left_url = "http://gerrit/changes/123/revisions/1/files/my-test-file.json/download"
        self.fake_right_url = "http://gerrit/changes/123/revisions/2/files/my-test-file.json/download"
        self.left_json = {"key": "value", "source": "left"}
        self.right_json = {"key": "value", "source": "right"}
        self.left_b64 = base64.b64encode(json.dumps(self.left_json).encode('utf-8'))
        self.right_b64 = base64.b64encode(json.dumps(self.right_json).encode('utf-8'))

    @patch('__main__.uuid.uuid4')
    def test_download_success(self, mock_uuid):
        fake_uuid = "1234-5678-90ab-cdef"
        mock_uuid.return_value = fake_uuid
        self.mock_subprocess_run.side_effect = [
            MagicMock(stdout=self.left_b64),
            MagicMock(stdout=self.right_b64)
        ]
        result = self.downloader.download(self.fake_left_url, self.fake_right_url)
        expected_left_content_url = self.fake_left_url.replace('/download', '/content')
        expected_right_content_url = self.fake_right_url.replace('/download', '/content')
        calls = [
            call(['gob-curl', expected_left_content_url], capture_output=True, check=True, text=False),
            call(['gob-curl', expected_right_content_url], capture_output=True, check=True, text=False)
        ]
        self.mock_subprocess_run.assert_has_calls(calls)
        self.assertEqual(len(result), 1)
        golden_data = result[0]
        self.assertEqual(golden_data['id'], fake_uuid)
        self.assertEqual(golden_data['testClassName'], 'my-test-file.json')
        self.assertEqual(golden_data['expectedData'], self.left_json)
        self.assertEqual(golden_data['actualData'], self.right_json)
        self.assertTrue(golden_data['isLocalData'])

    def test_download_subprocess_exception(self):
        self.mock_subprocess_run.side_effect = subprocess.CalledProcessError(1, 'gob-curl')
        result = self.downloader.download(self.fake_left_url, self.fake_right_url)
        self.assertEqual(result[0]['expectedData'], {})
        self.assertEqual(result[0]['actualData'], {})

    def test_download_json_decode_error(self):
        invalid_json_b64 = base64.b64encode(b"this is not json")
        self.mock_subprocess_run.return_value = MagicMock(stdout=invalid_json_b64)
        result = self.downloader.download(self.fake_left_url, self.fake_right_url)
        self.assertEqual(result[0]['expectedData'], {})
        self.assertEqual(result[0]['actualData'], {})
        self.assertEqual(self.mock_subprocess_run.call_count, 2)

    def test_download_with_one_none_link(self):
        self.mock_subprocess_run.return_value = MagicMock(stdout=self.right_b64)
        result = self.downloader.download(None, self.fake_right_url)
        self.mock_subprocess_run.assert_called_once()
        self.assertEqual(result[0]['expectedData'], {})
        self.assertEqual(result[0]['actualData'], self.right_json)
        self.assertEqual(result[0]['testClassName'], 'my-test-file.json')

    def test_download_base64_decode_error(self):
        invalid_b64_stdout = b"this is not valid base64 string$$$"
        self.mock_subprocess_run.return_value = MagicMock(stdout=invalid_b64_stdout)
        result = self.downloader.download(self.fake_left_url, self.fake_right_url)
        self.assertEqual(result[0]['expectedData'], {})
        self.assertEqual(result[0]['actualData'], {})

if __name__ == '__main__':
    unittest.main(argv=['first-arg-is-ignored'], exit=False)