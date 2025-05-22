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
from unittest.mock import patch, mock_open
import sys
import os
import subprocess
import json
import requests

sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))
from impl.fetch_presubmit_test_artifact import FetchPresubmitTestArtifacts
from test_assets.test_assets import FetchPresubmitTestArtifactTestAssets as ta
from test_assets.fake_subprocess import FakeSubprocessRun
from test_assets.fake_presubmit_apis import FakePresubmitArtifactAPIClient
from impl.constants import create_presubmit_artifacts_list_url
from impl.constants import create_get_download_url_for_presubmit_artifact
from impl.constants import ANDROID_BUILD_SCOPE

fetch_artifact_obj = None
headers = None
artifacts_list_api = None
artifacts_get_download_url_api = None

class FetchPresubmitTestArtifactTest(unittest.TestCase):

    @patch.object(FetchPresubmitTestArtifacts,
                  '_FetchPresubmitTestArtifacts__refresh_user_token')
    @patch.object(FetchPresubmitTestArtifacts,
                  '_FetchPresubmitTestArtifacts__create_download_directory')
    def setUp(self, mock_create_download_dir, mock_refresh_token):
        global fetch_artifact_obj, headers, artifacts_list_api, artifacts_get_download_url_api
        fetch_artifact_obj = FetchPresubmitTestArtifacts(
                                ta.invocation_id,
                                ta.artifacts_download_dir
                            )
        fetch_artifact_obj._token = ta.auth_token
        os.makedirs(ta.artifacts_download_dir, exist_ok=True)
        headers = {
            'Authorization': f'Bearer {fetch_artifact_obj._token}',
            'Accept': 'application/json',
        }
        artifacts_list_api = create_presubmit_artifacts_list_url(
                                fetch_artifact_obj._invocation_id,
                                100
                            )
        artifacts_get_download_url_api = create_get_download_url_for_presubmit_artifact(
                                            ta.test_asset_1_json,
                                            fetch_artifact_obj._invocation_id
                                        )

    def test_init(self):
        self.assertEqual(fetch_artifact_obj._invocation_id, ta.invocation_id)
        self.assertEqual(fetch_artifact_obj._artifacts_download_dir,
                         ta.artifacts_download_dir)
        self.assertEqual(len(fetch_artifact_obj._tests_with_artifacts_downloaded), 0)
        self.assertTrue(fetch_artifact_obj._token != None)
        self.assertTrue(os.path.isdir(ta.artifacts_download_dir)
                        and len(os.listdir(ta.artifacts_download_dir)) == 0)

    def test_get_auth_token(self):
        fake_subprocess_run = FakeSubprocessRun('oauth2_token: "auth_token"')
        token = (fetch_artifact_obj._FetchPresubmitTestArtifacts__get_oauth_token(
                     ta.email, ta.scope, fake_subprocess_run))
        self.assertEqual(token, ta.auth_token)
        command = [
                "stubby",
                "call",
                "blade:sso",
                "CorpLogin.Exchange",
                "--proto2",
                (f'target {{ name: "{ta.email}" }} target_credential'
                f'{{ type: OAUTH2_TOKEN oauth2_attributes {{ scope: "{ta.scope}" }} }}')
            ]
        self.assertEqual(
            fake_subprocess_run.calls,
            [
                (command)
            ]
        )

    def test_get_auth_token_got_malformed_token(self):
        fake_subprocess_run = FakeSubprocessRun('oauth2_token: malformed_token')
        with self.assertRaises(ValueError) as ex:
                fetch_artifact_obj._FetchPresubmitTestArtifacts__get_oauth_token(
                     ta.email, ta.scope, fake_subprocess_run)
        self.assertIn("Error: Could not extract token from output:\noauth2_token: malformed_token",
                       str(ex.exception))

    def test_get_auth_token_raises_process_error(self):
        fake_subprocess_run = FakeSubprocessRun(raise_exception=
            subprocess.CalledProcessError(
                returncode=1,
                cmd=["command"],
                output="permission denied",
                stderr="cannot access token: certificate expired"
            )
        )
        with self.assertRaises(subprocess.CalledProcessError) as ex:
                fetch_artifact_obj._FetchPresubmitTestArtifacts__get_oauth_token(
                     ta.email, ta.scope, fake_subprocess_run)
        self.assertIn("Command '['command']' returned non-zero exit status 1",
                      str(ex.exception))
        self.assertIn("cannot access token: certificate expired",
                      ex.exception.stderr.strip())
        self.assertIn("permission denied", ex.exception.output.strip())

    @patch('os.makedirs')
    @patch('shutil.rmtree')
    def test_create_output_directory(self, mock_rmtree, mock_makedirs):
        self.assertTrue(fetch_artifact_obj
                        ._FetchPresubmitTestArtifacts__create_download_directory())
        mock_makedirs.assert_called_once_with(fetch_artifact_obj
                                              ._artifacts_download_dir)
        mock_rmtree.assert_called_once_with(fetch_artifact_obj
                                              ._artifacts_download_dir)

    @patch('os.makedirs')
    @patch('shutil.rmtree')
    def test_create_output_directory_fails(self, mock_rmtree, mock_makedirs):
        mock_makedirs.side_effect = OSError("Permission denied")
        with self.assertRaises(OSError) as ex:
            fetch_artifact_obj._FetchPresubmitTestArtifacts__create_download_directory()
        self.assertIn("Permission denied", str(ex.exception))
        mock_rmtree.assert_called_once_with(fetch_artifact_obj
                                              ._artifacts_download_dir)

    def test_fetch_artifact_list_empty_response(self):
        fake_presubmit_api_client = FakePresubmitArtifactAPIClient(
            status_code=[200], data=[{}]
        )
        self.assertEqual(
             fetch_artifact_obj._FetchPresubmitTestArtifacts__fetch_artifacts_list(
                api_client = fake_presubmit_api_client
            ),
            []
        )
        self.assertEqual(
            fake_presubmit_api_client.calls,
            [
                {
                    "url":artifacts_list_api,
                    "headers":headers,
                    "allow_redirects":True
                }
            ]
        )


    def test_fetch_artifact_list_with_multi_page_response(self):
        fake_presubmit_api_client = FakePresubmitArtifactAPIClient(
            status_code=[200,200], data=ta.list_artifact_api_response
        )
        self.assertEqual(
             fetch_artifact_obj._FetchPresubmitTestArtifacts__fetch_artifacts_list(
                api_client = fake_presubmit_api_client
            ), [
                ta.test_asset_1_json, ta.test_asset_2_json, ta.test_asset_2_mp4
            ]
        )
        self.assertEqual(fake_presubmit_api_client.calls,
            [
                {
                    "url": artifacts_list_api,
                    "headers": headers,
                    "allow_redirects": True
                },
                {
                    "url": f"{artifacts_list_api}&pageToken={ta.page_two_token}",
                    "headers": headers,
                    "allow_redirects":True
                }
            ]
        )

    @patch.object(FetchPresubmitTestArtifacts,
                  '_FetchPresubmitTestArtifacts__refresh_user_token')
    def test_fetch_artifact_list_user_token_expires(self, mock_refresh_token):
        fake_presubmit_api_client = FakePresubmitArtifactAPIClient(
            status_code=[401,200], data=[None, {}]
        )
        self.assertEqual(
             fetch_artifact_obj._FetchPresubmitTestArtifacts__fetch_artifacts_list(
                api_client = fake_presubmit_api_client
            ), []
        )
        self.assertEqual(fake_presubmit_api_client.calls,
            [
                {
                    "url": artifacts_list_api,
                    "headers": headers,
                    "allow_redirects": True
                },
                {
                    "url": artifacts_list_api,
                    "headers": headers,
                    "allow_redirects": True
                }
            ]
        )
        mock_refresh_token.assert_called_once()

    def test_fetch_artifact_list_service_unavailable(self):
        fake_presubmit_api_client = FakePresubmitArtifactAPIClient(
            status_code=[503], error_message=["Service Unavailable"]
        )
        with self.assertRaises(requests.exceptions.RequestException) as ex:
            fetch_artifact_obj._FetchPresubmitTestArtifacts__fetch_artifacts_list(
                api_client = fake_presubmit_api_client
            )
        self.assertIn("Service Unavailable",
                       str(ex.exception))

    def test_fetch_artifact_list_data_not_parsable(self):
        fake_presubmit_api_client = FakePresubmitArtifactAPIClient(
            status_code=[200],
            data=["Not Json parsable str"]
        )
        with self.assertRaises(json.JSONDecodeError) as ex:
            fetch_artifact_obj._FetchPresubmitTestArtifacts__fetch_artifacts_list(
                api_client = fake_presubmit_api_client
            )
        self.assertIn("Failure to parse into json object", str(ex.exception))

    def test_fetch_artifact_list_raises_exception(self):
        fake_presubmit_api_client = FakePresubmitArtifactAPIClient(
            raise_exception=Exception("Unknown exception")
        )
        with self.assertRaises(Exception) as ex:
            fetch_artifact_obj._FetchPresubmitTestArtifacts__fetch_artifacts_list(
                api_client = fake_presubmit_api_client
            )
        self.assertIn("Unknown exception", str(ex.exception))

    def test_get_signed_download_url_empty_response(self):
        fake_presubmit_api_client = FakePresubmitArtifactAPIClient(
            status_code=[200], data=[{}]
        )
        self.assertEqual(
            fetch_artifact_obj._FetchPresubmitTestArtifacts__get_signed_download_url(
                resource_id = ta.test_asset_1_json,
                api_client = fake_presubmit_api_client
            ), None)
        self.assertEqual(
            fake_presubmit_api_client.calls,
            [
                {
                    "url": artifacts_get_download_url_api,
                    "headers": headers,
                    "allow_redirects":False
                }
            ]
        )

    @patch.object(FetchPresubmitTestArtifacts,
                  '_FetchPresubmitTestArtifacts__refresh_user_token')
    def test_get_signed_download_url_user_token_expires(self, mock_refresh_token):
        fake_presubmit_api_client = FakePresubmitArtifactAPIClient(
            status_code=[401,200],
            data=[None, {"signedUrl": ta.download_url}]
        )
        self.assertEqual(
            fetch_artifact_obj._FetchPresubmitTestArtifacts__get_signed_download_url(
                resource_id = ta.test_asset_1_json,
                api_client = fake_presubmit_api_client
            ), ta.download_url
        )
        self.assertEqual(fake_presubmit_api_client.calls,
            [
                {
                    "url": artifacts_get_download_url_api,
                    "headers": headers,
                    "allow_redirects": False
                },
                {
                    "url": artifacts_get_download_url_api,
                    "headers": headers,
                    "allow_redirects": False
                }
            ]
        )
        mock_refresh_token.assert_called_once()

    def test_get_signed_download_url_service_unavailable(self):
        fake_presubmit_api_client = FakePresubmitArtifactAPIClient(
            status_code=[503], error_message=["Service Unavailable"]
        )
        with self.assertRaises(requests.exceptions.RequestException) as ex:
            fetch_artifact_obj._FetchPresubmitTestArtifacts__get_signed_download_url(
                resource_id = ta.test_asset_1_json,
                api_client = fake_presubmit_api_client
            )
        self.assertIn("Service Unavailable", str(ex.exception))

    def test_get_signed_download_url_data_not_parsable(self):
        fake_presubmit_api_client = FakePresubmitArtifactAPIClient(
            status_code=[200],
            data=["Not Json parsable str"]
        )
        with self.assertRaises(json.JSONDecodeError) as ex:
            fetch_artifact_obj._FetchPresubmitTestArtifacts__get_signed_download_url(
                resource_id = ta.test_asset_1_json,
                api_client = fake_presubmit_api_client
            )
        self.assertIn("Failure to parse into json object", str(ex.exception))

    def test_get_signed_download_url_raises_exception(self):
        fake_presubmit_api_client = FakePresubmitArtifactAPIClient(
            raise_exception=Exception("Unknown exception")
        )
        with self.assertRaises(Exception) as ex:
            fetch_artifact_obj._FetchPresubmitTestArtifacts__get_signed_download_url(
                resource_id = ta.test_asset_1_json,
                api_client = fake_presubmit_api_client
            )
        self.assertIn("Unknown exception", str(ex.exception))

    @patch.object(FetchPresubmitTestArtifacts,
                  '_FetchPresubmitTestArtifacts__get_oauth_token')
    def test_refresh_user_token_no_auth_token_received(self, mock_get_auth):
        mock_get_auth.return_value = None
        with self.assertRaises(Exception) as ex:
            fetch_artifact_obj._FetchPresubmitTestArtifacts__refresh_user_token()
        self.assertIn("API token cannot be obtained", str(ex.exception))
        mock_get_auth.assert_called_once_with(
            f"{os.environ.get("USER")}@google.com", ANDROID_BUILD_SCOPE)

    @patch.object(FetchPresubmitTestArtifacts,
                  '_FetchPresubmitTestArtifacts__get_oauth_token')
    def test_refresh_user_token_auth_token_received(self, mock_get_auth):
        mock_get_auth.return_value = ta.auth_token
        fetch_artifact_obj._FetchPresubmitTestArtifacts__refresh_user_token()
        self.assertEqual(fetch_artifact_obj._token, ta.auth_token)
        mock_get_auth.assert_called_once_with(
            f"{os.environ.get("USER")}@google.com", ANDROID_BUILD_SCOPE)

    @patch.object(FetchPresubmitTestArtifacts,
                  '_FetchPresubmitTestArtifacts__fetch_artifacts_list')
    def test_list_presubmit_artifacts(self, mock_fetch_artifacts_list):
        mock_fetch_artifacts_list.return_value = [ta.test_asset_1_json,
                                                  ta.test_asset_2_json,
                                                  ta.test_asset_2_mp4]
        self.assertEqual(fetch_artifact_obj.list_presubmit_test_artifacts(),
                         [ta.test_name1, ta.test_name2])
        self.assertEqual(fetch_artifact_obj._artifacts_dict,
                         {
                             ta.test_name1: [ta.test_asset_1_json],
                             ta.test_name2: [ta.test_asset_2_json,
                                             ta.test_asset_2_mp4]
                         })

    @patch.object(FetchPresubmitTestArtifacts,
                  '_FetchPresubmitTestArtifacts__fetch_artifacts_list')
    def test_list_presubmit_artifacts_no_actuals_found(self,mock_fetch_artifacts_list):
        mock_fetch_artifacts_list.return_value = []
        self.assertEqual(fetch_artifact_obj.list_presubmit_test_artifacts(),[])
        self.assertEqual(fetch_artifact_obj._artifacts_dict,{})

    @patch.object(FetchPresubmitTestArtifacts,
                  '_FetchPresubmitTestArtifacts__fetch_artifacts_list')
    def test_list_presubmit_artifacts_exception_raised(self, mock_fetch_artifacts_list):
        mock_fetch_artifacts_list.side_effect = Exception("Failure while fetching artifacts")
        with self.assertRaises(Exception) as ex:
            fetch_artifact_obj.list_presubmit_test_artifacts()
        self.assertIn("Failure while fetching artifacts", str(ex.exception))

    @patch.object(FetchPresubmitTestArtifacts,
                  '_FetchPresubmitTestArtifacts__get_signed_download_url')
    @patch.object(FetchPresubmitTestArtifacts,
                  '_FetchPresubmitTestArtifacts__download_artifact')
    def test_download_presubmit_test_artifact_for_test_name(self, mock_downloader,
                                                             mock_get_signed_url):
        mock_get_signed_url.return_value = ta.download_url
        fetch_artifact_obj._artifacts_dict = {ta.test_name1: [ta.test_asset_1_json]}
        result = fetch_artifact_obj.download_presubmit_test_artifact_for_test_name(ta.test_name1)
        self.assertTrue(ta.test_name1 in fetch_artifact_obj._tests_with_artifacts_downloaded)
        mock_get_signed_url.assert_called_once_with(ta.test_asset_1_json)
        mock_downloader.assert_called_once_with(ta.download_url, ta.test_asset_1_json)
        self.assertEqual(result, [ta.test_asset_1_json])

    @patch.object(FetchPresubmitTestArtifacts,
                  '_FetchPresubmitTestArtifacts__get_signed_download_url')
    @patch.object(FetchPresubmitTestArtifacts,
                  '_FetchPresubmitTestArtifacts__download_artifact')
    def test_download_presubmit_test_artifact_called_with_same_test_name_multiple_times(
                                                            self, mock_downloader,
                                                            mock_get_signed_url
                                                        ):
        mock_get_signed_url.return_value = ta.download_url
        fetch_artifact_obj._artifacts_dict = {ta.test_name1: [ta.test_asset_1_json]}
        fetch_artifact_obj._tests_with_artifacts_downloaded.add(ta.test_name1)
        result = (fetch_artifact_obj
                    .download_presubmit_test_artifact_for_test_name(ta.test_name1))
        mock_get_signed_url.assert_not_called()
        mock_downloader.assert_not_called()
        self.assertEqual(result, [])

    @patch.object(FetchPresubmitTestArtifacts,
                  '_FetchPresubmitTestArtifacts__get_signed_download_url')
    @patch.object(FetchPresubmitTestArtifacts,
                  '_FetchPresubmitTestArtifacts__download_artifact')
    def test_download_presubmit_test_artifact_with_no_signed_url(
                                                            self, mock_downloader,
                                                            mock_get_signed_url
                                                        ):
        mock_get_signed_url.return_value = None
        fetch_artifact_obj._artifacts_dict = {ta.test_name1: [ta.test_asset_1_json]}
        with self.assertRaises(ValueError) as ex:
            fetch_artifact_obj.download_presubmit_test_artifact_for_test_name(ta.test_name1)
        self.assertIn("Error: Invalid signed download url!", str(ex.exception))
        mock_get_signed_url.assert_called_once_with(ta.test_asset_1_json)
        mock_downloader.assert_not_called()
        self.assertEqual(len(fetch_artifact_obj._tests_with_artifacts_downloaded), 0)

    @patch('os.access')
    @patch('builtins.open', new_callable=mock_open)
    def test_download_artifact(self, mock_file_open, mock_file_access):
        mock_file_access.return_value=True
        fake_api_client = FakePresubmitArtifactAPIClient(
            status_code=[200],
            data=[b"This is some test content for the artifact."]
        )
        fetch_artifact_obj._FetchPresubmitTestArtifacts__download_artifact(
            ta.download_url,
            ta.test_asset_1_json,
            fake_api_client
        )
        self.assertEqual(
            fake_api_client.calls,
            [{"url":ta.download_url, "stream":True}]
        )
        mock_file_access.assert_called_once_with(
            fetch_artifact_obj._artifacts_download_dir,
            os.W_OK
        )
        mock_file_open.assert_called_once_with(
            f'{fetch_artifact_obj._artifacts_download_dir}/{ta.test_asset_1_json}',
            'wb'
        )
        mock_file_handle = mock_file_open()
        written_content = b"".join(call.args[0]
                                for call in mock_file_handle.write.call_args_list
                            )
        self.assertEqual(written_content, b"This is some test content for the artifact.")

    @patch('os.access')
    def test_download_artifact_empty_url(self, mock_access):
        with self.assertRaises(ValueError) as ex:
            fetch_artifact_obj._FetchPresubmitTestArtifacts__download_artifact(
                "",
                ta.test_asset_1_json
            )
        self.assertIn("URL is empty", str(ex.exception))
        mock_access.assert_not_called()

    @patch('os.access')
    def test_download_artifact_download_directory_not_writable(self, mock_access):
        mock_access.return_value = False
        with self.assertRaises(OSError) as ex:
            fetch_artifact_obj._FetchPresubmitTestArtifacts__download_artifact(
                ta.download_url,
                ta.test_asset_1_json
            )
        self.assertIn("Download directory is not writable", str(ex.exception))
        mock_access.assert_called_once_with(
            fetch_artifact_obj._artifacts_download_dir, os.W_OK)

    @patch('os.access')
    def test_download_artifact_returned_404(self, mock_file_access):
        mock_file_access.return_value=True
        fake_api_client = FakePresubmitArtifactAPIClient(
            status_code=[404]
        )
        with self.assertRaises(requests.exceptions.RequestException) as ex:
            fetch_artifact_obj._FetchPresubmitTestArtifacts__download_artifact(
                ta.download_url,
                ta.test_asset_1_json,
                fake_api_client
            )
        self.assertEqual(
            fake_api_client.calls,
            [{"url":ta.download_url, "stream":True}]
        )
        mock_file_access.assert_called_once_with(
            fetch_artifact_obj._artifacts_download_dir,
            os.W_OK
        )

    @patch('os.access')
    @patch('builtins.open', new_callable=mock_open)
    def test_download_artifact_raises_os_error(self, mock_file_open, mock_file_access):
        mock_file_access.return_value=True
        fake_api_client = FakePresubmitArtifactAPIClient(
            status_code=[200],
            data=[b"This is some test content for the artifact."]
        )
        (mock_file_open.return_value.__enter__
        .return_value.write.side_effect) = OSError("Disk full")

        with self.assertRaises(OSError) as ex:
            fetch_artifact_obj._FetchPresubmitTestArtifacts__download_artifact(
                ta.download_url,
                ta.test_asset_1_json,
                fake_api_client
            )
        self.assertIn("Disk full", str(ex.exception))
        self.assertEqual(
            fake_api_client.calls,
            [{"url":ta.download_url, "stream":True}]
        )
        mock_file_access.assert_called_once_with(
            fetch_artifact_obj._artifacts_download_dir,
            os.W_OK
        )
        mock_file_open.assert_called_once_with(
            f'{fetch_artifact_obj._artifacts_download_dir}/{ta.test_asset_1_json}',
            'wb'
        )

    @patch('os.access')
    def test_download_artifact_raises_unknown_exception(self, mock_file_access):
        mock_file_access.return_value=True
        fake_api_client = FakePresubmitArtifactAPIClient(
            raise_exception=Exception("unknown exception")
        )
        with self.assertRaises(Exception) as ex:
            fetch_artifact_obj._FetchPresubmitTestArtifacts__download_artifact(
                ta.download_url,
                ta.test_asset_1_json,
                fake_api_client
            )
        self.assertIn("unknown exception", str(ex.exception))
        self.assertEqual(
            fake_api_client.calls,
            [{"url":ta.download_url, "stream":True}]
        )
        mock_file_access.assert_called_once_with(
            fetch_artifact_obj._artifacts_download_dir,
            os.W_OK
        )


if __name__ == '__main__':
  unittest.main()