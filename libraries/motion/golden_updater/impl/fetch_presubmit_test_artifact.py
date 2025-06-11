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
import re
import requests
import json
import os
from urllib.parse import urlparse
import threading
import shutil
from impl.constants import create_presubmit_artifacts_list_url
from impl.constants import create_get_download_url_for_presubmit_artifact
from impl.constants import ANDROID_BUILD_SCOPE

class FetchPresubmitTestArtifacts:

    def __init__(self, invocation_id, artifacts_download_dir):
        self._invocation_id = invocation_id
        self._tests_with_artifacts_downloaded = set()
        self.__refresh_user_token()
        self._artifacts_download_dir = artifacts_download_dir
        self.__create_download_directory()

    def __get_oauth_token(self, email, scope, subprocess_run = subprocess.run):
        """
        Retrieves an OAuth2 token for the specified user and scope using stubby.

        Args:
            email: The email address of the user (e.g., "username@google.com").
            scope: The desired OAuth2 scope
            (e.g., "https://www.googleapis.com/auth/androidbuild.internal").

        Returns:
            The OAuth2 token string, or None if an error occurred.
        """

        command = [
            "stubby",
            "call",
            "blade:sso",
            "CorpLogin.Exchange",
            "--proto2",
            (f'target {{ name: "{email}" }} target_credential'
                f'{{ type: OAUTH2_TOKEN oauth2_attributes {{ scope: "{scope}" }} }}')
        ]

        try:
            result = subprocess_run(command, capture_output=True,
                                    text=True, check=True)
            output = result.stdout

            # Extracting the token using a regular expression
            match = re.search(r'oauth2_token: "(.*)"', output)
            if match:
                return match.group(1)
            else:
                raise ValueError(f"Error: Could not extract token from output:\n{output}")
        except subprocess.CalledProcessError as exception:
            print(f"Please ensure GCERT is done! Error running stubby command: {exception}. \
                  Output: {exception.stderr}")
            raise exception
        except Exception as exception:
            print(f"An unexpected error occurred: {exception}")
            raise exception

    def __fetch_artifacts_list(self, max_results=100, api_client = requests):
        """
        Fetches test artifacts for a given invocation ID
            using the AnTS API.

        Args:
            max_results: Maximum number of results requested in the response.
                If max_results >= total number of results available,
                                        it returns all the results.
                If max_results < total number of results available,
                    it returns {max_results} number of results in first page
                    and then allows pagination.

        Returns:
            A list of test artifact names (str) containing '.actual'
                or [] if no such names found.
            Raises exception if error occurred.
        """

        api = create_presubmit_artifacts_list_url(self._invocation_id, max_results)
        nextPageToken = ""
        url=api
        test_artifacts = []

        retried = False
        while nextPageToken is not None:
            headers = {
                'Authorization': f'Bearer {self._token}',
                'Accept': 'application/json',
            }
            try:
                response = api_client.get(url, headers=headers, allow_redirects=True)
                response.raise_for_status()
                data = response.json()
                if data and data.get('test_artifacts'):
                    [test_artifacts.append(artifact.get("name"))
                        for artifact in data.get('test_artifacts')
                            if ".actual" in artifact.get("name")]
                    nextPageToken = data.get('nextPageToken')
                    url=f"{api}&pageToken={nextPageToken}"
                else:
                    print(f"No test artifacts found for invocation {self._invocation_id}")
                    nextPageToken = None
            except requests.exceptions.RequestException as exception:
                print(f"An error occurred during the fetch artifact request: {exception}")
                if exception.response.status_code == 401 and not retried:
                    self.__refresh_user_token()
                    retried = True
                    continue
                raise exception
            except json.JSONDecodeError as exception:
                print(f"Error decoding JSON response during the fetch artifact "
                      f"request: {exception}")
                print(f"Response content of the fetch artifact request: "
                      f"{response.content}")
                raise exception
            except Exception as exception:
                print(f"An unexpected error occurred during the fetch artifact "
                      f"request: {exception}")
                raise exception
        return test_artifacts

    def __get_signed_download_url(self, resource_id, api_client = requests):
        """
        Constructs the HTTPS download URL for the test artifact.

        Args:
            resource_id: The name/id of the artifact to download.

        Returns:
            The HTTPS download URL, or None if the path is invalid.
        """

        url = create_get_download_url_for_presubmit_artifact(resource_id,
                                                             self._invocation_id)
        attempts = 1
        while attempts <= 2 :
            headers = {
                'Authorization': f'Bearer {self._token}',
                'Accept': 'application/json',
            }
            try:
                response = api_client.get(url, headers=headers, allow_redirects=False)
                response.raise_for_status()
                data = response.json()
                if data and data.get('signedUrl'):
                    return data['signedUrl']
                else:
                    print(f"No signedUrl found for resource: {resource_id} "
                          f"invocation:{self._invocation_id}")
                    return None
            except requests.exceptions.RequestException as exception:
                print(f"An error occurred during the signed url request: {exception}")
                if exception.response.status_code == 401 and attempts <= 1:
                    self.__refresh_user_token()
                    attempts += 1
                    continue
                raise exception
            except json.JSONDecodeError as exception:
                print(f"Error decoding JSON response of the signed url: {exception}")
                print(f"Response content the signed url: {response.content}")
                raise exception
            except Exception as exception:
                print(f"An unexpected error occurred during the signed url "
                      f"request: {exception}")
                raise exception


    def __download_artifact(self, url_string, artifact_name, api_client=requests):
        """
        Downloads an artifact using a signed URL.

        Args:
            url_string: The URL of the artifact to download.
            artifact_name: The name of the artifact to save.
        """
        if not url_string:
            raise ValueError("URL is empty")

        if not os.access(self._artifacts_download_dir, os.W_OK):
            raise OSError("Download directory is not writable")

        try:
            file_path = os.path.join(self._artifacts_download_dir, artifact_name)
            response = api_client.get(url_string, stream=True)
            response.raise_for_status()
            block_size = 1024

            with open(file_path, 'wb') as file:
                for chunk in response.iter_content(block_size):
                    file.write(chunk)
            print(f"Download complete: {file_path}.")
        except requests.exceptions.RequestException as exception:
            print(f"Error downloading artifact: {exception}")
            raise exception
        except OSError as exception:
            print(f"Error creating the output directory: {exception}")
            raise exception
        except Exception as exception:
            print(f"An unexpected error occurred: {exception}")
            raise exception


    def __create_download_directory(self):
        """ Creates the directory to download the artifacts. If already exists,
                it deletes and recreates it.

        Returns:
            True if the directory was created or already exists,
                raises exception otherwise.
        """
        try:
            if os.path.exists(self._artifacts_download_dir):
                shutil.rmtree(self._artifacts_download_dir)
            os.makedirs(self._artifacts_download_dir)
            return True
        except OSError as exception:
            print(f"Error creating/deleting directory "
                  f"{self._artifacts_download_dir}: {exception}")
            raise exception

    def __refresh_user_token(self):
        '''Fetches and sets new token for the user for the given scope.

        Raises exception if token cannot be obtained.
        '''
        user = os.environ.get("USER")
        user_email = f"{user}@google.com"
        print(f"Running for user: {user_email}")
        self._token = self.__get_oauth_token(user_email, ANDROID_BUILD_SCOPE)
        if not self._token:
            raise Exception("API token cannot be obtained")

    def list_presubmit_test_artifacts(self):
        '''Fetches test artifacts list whose name contains '.actual'
            and creates a mapping between test names and list of test artifacts.

        Returns:
            A list of test names for whose artifacts have '.actual' in their names.

        '''
        artifacts = self.__fetch_artifacts_list()
        self._artifacts_dict = {}

        if not artifacts:
            print("No Artifacts found for this invocation ID.")
            return []

        for artifact in artifacts:
            test_name = artifact[0:artifact.find(".actual")].replace("/","_")
            if not test_name in self._artifacts_dict:
                self._artifacts_dict[test_name] = []
            self._artifacts_dict[test_name].append(artifact)
        return list(self._artifacts_dict.keys())

    def download_presubmit_test_artifact_for_test_name(self, test_name):
        ''' Downloads and save artifacts for given test name.

        Args:
            test_name: test name of the test for which artifacts need to be downloaded.

        Throws:
            ValueError if signed download url is not received.
        '''
        if test_name in self._tests_with_artifacts_downloaded:
            return []

        for resource_id in self._artifacts_dict.get(test_name):
            url_to_download = self.__get_signed_download_url(resource_id)

            if not url_to_download:
                raise ValueError("Error: Invalid signed download url!")
            else:
                download_thread = threading.Thread(target=self.__download_artifact,
                    args=(url_to_download, resource_id))
                download_thread.start()
                download_thread.join()

        self._tests_with_artifacts_downloaded.add(test_name)
        return self._artifacts_dict[test_name]

