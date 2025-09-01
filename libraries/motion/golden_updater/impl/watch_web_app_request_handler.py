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

import http.server
import urllib.parse
import json
import mimetypes
import shutil
from os import path
import pathlib
import os
import requests
from impl.constants import GOLDEN_ACCESS_TOKEN_HEADER
from impl.zip_to_video_converter import ZipToVideoConverter
from impl.gerrit_downloader import GerritDownloader
from impl.fetch_presubmit_test_artifact import FetchPresubmitTestArtifacts
from impl.golden_watchers.golden_watcher_factory import GoldenWatcherFactory
from impl.golden_watchers.golden_watcher_types import GoldenWatcherTypes
from impl.adb_serial_finder import ADBSerialFinder
from impl.adb_client import AdbClient
from impl.test_entity import TestEntity

class WatchWebAppRequestHandler(http.server.BaseHTTPRequestHandler):
    secret_token = None
    test_entity: TestEntity = None
    temp_dir = None
    android_build_top = None
    this_server_address = None
    adb_serial_finder = ADBSerialFinder()
    test_entity_cache = {}

    def __init__(self, *args, **kwargs):
        self.root_directory = path.abspath(path.dirname(__file__))
        super().__init__(*args, **kwargs)

    def verify_access_token(self):
        token = self.headers.get(GOLDEN_ACCESS_TOKEN_HEADER)
        if not token or token != WatchWebAppRequestHandler.secret_token:
            self.send_response(403, "Bad authorization token!")
            return False

        return True

    def do_OPTIONS(self):
        self.send_response(200)
        self.send_header("Allow", "GET,POST,PUT")
        self.add_standard_headers()
        self.end_headers()
        self.wfile.write(b"GET,POST,PUT")

    def do_GET(self):

        parsed = urllib.parse.urlparse(self.path)

        if parsed.path == "/service/testModes/list":
            self.get_available_modes()
            return
        elif parsed.path.startswith("/golden/"):
            requested_file_start_index = parsed.path.find("/", len("/golden/") + 1)
            requested_file = parsed.path[requested_file_start_index + 1 :]
            self.serve_file(
                WatchWebAppRequestHandler.test_entity.golden_watcher.temp_dir,
                requested_file
            )
            return
        elif parsed.path.startswith("/expected/"):
            golden_id = parsed.path[len("/expected/") :]

            goldens = WatchWebAppRequestHandler.test_entity.golden_watcher.cached_goldens.values()
            for golden in goldens:
                if golden.id != golden_id:
                    continue

                self.serve_file(
                    WatchWebAppRequestHandler.android_build_top,
                    golden.golden_repo_path, "application/json"
                )
                return
        elif parsed.path.startswith("/getGerrit"):
            query_params = urllib.parse.parse_qs(parsed.query)
            # query_params.get() returns a list. Picking out the first element from it
            leftLinkValues = query_params.get('leftLink')
            rightLinkValues = query_params.get('rightLink')
            leftLink = leftLinkValues[0] if leftLinkValues else None
            rightLink = rightLinkValues[0] if rightLinkValues else None
            gerrit_downloader = GerritDownloader()
            res = gerrit_downloader.download(left=leftLink, right=rightLink)
            testEntity = TestEntity(goldens_list=res)
            (WatchWebAppRequestHandler
            .test_entity_cache[GoldenWatcherTypes.GERRIT.value]) = testEntity
            print("All done. Sending data to UI")
            self.send_json(res)
            return

        self.send_error_with_message(404, message=f"Invalid GET API: {parsed.path}")

    def do_POST(self):
        if not self.verify_access_token():
            return

        content_type = self.headers.get("Content-Type")

        # refuse to receive non-json content
        if content_type != "application/json":
            self.send_response(400)
            return

        length = int(self.headers.get("Content-Length"))
        message = json.loads(self.rfile.read(length))

        parsed = urllib.parse.urlparse(self.path)
        if parsed.path == "/service/refresh":
            self.service_refresh_goldens(message["clear"])
        elif parsed.path == '/service/presubmit_artifact/list':
            self.service_presubmit_artifact_list(message["invocation_id"])
        elif parsed.path == '/service/fetch_artifact':
            self.service_fetch_artifacts(message["resource_id"])
        elif parsed.path == "/service/mode":
            self.switch_mode(message["mode"])
        elif parsed.path == "/getMultipleGoldensFromGerrit":
            self.fetch_gerrit_artifacts(message["linkPairs"])
        else:
            self.send_error_with_message(404, message=f"Invalid POST API: {parsed.path}")

    def do_PUT(self):
        if not self.verify_access_token():
            return

        parsed = urllib.parse.urlparse(self.path)

        if parsed.path == "/service/update":
            query_params = urllib.parse.parse_qs(parsed.query)
            self.service_update_golden({query_params["id"][0]})
        elif parsed.path == '/service/updateSelectedGoldensIds':
            length = int(self.headers.get("Content-Length"))
            message = json.loads(self.rfile.read(length))
            self.service_update_golden(set(message["selectedGoldenIds"]))
        else:
            self.send_error_with_message(404, message=f"Invalid PUT API: {parsed.path}")

    def serve_file(self, root_directory, file_relative_to_root, mime_type=None):
        resolved_path = path.abspath(path.join(root_directory, file_relative_to_root))

        if path.commonprefix(
            [resolved_path, root_directory]
        ) == root_directory and path.isfile(resolved_path):

            if resolved_path.endswith("screenshots.zip"):

                if ZipToVideoConverter.process_single_zip(pathlib.Path(resolved_path)):
                    video_path = resolved_path.replace("zip","mp4")
                    if pathlib.Path(video_path).is_file():
                        self.send_response(200)
                        self.send_header(
                            "Content-type", mime_type or mimetypes.guess_type(resolved_path)[0]
                        )
                        self.add_standard_headers()
                        self.send_header(
                            "Content-type", "video/mp4"
                        )
                        self.end_headers()
                        with open(video_path, "rb") as f:
                            self.wfile.write(f.read())
                else:
                    # when unzip wasn't successful
                    self.send_error_with_message(
                        503, message="Zip to Video convertor failed !")

            else :
                self.send_response(200)
                self.send_header(
                    "Content-type", mime_type or mimetypes.guess_type(resolved_path)[0]
                )
                self.add_standard_headers()
                self.end_headers()
                with open(resolved_path, "rb") as f:
                    self.wfile.write(f.read())

        else:
            self.send_error_with_message(404, message=f"File not found: {resolved_path}")

    def send_error_with_message(self, code, message):
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.add_standard_headers()
        self.end_headers()
        payload = {"message": message}
        self.wfile.write(json.dumps(payload).encode("utf-8"))

    def fetch_gerrit_artifacts(self, linkPairs):
        gerrit_downloader = GerritDownloader()
        golden_list = gerrit_downloader.downloadMultipleJsons(linkPairs)
        self.send_json(golden_list)

    def service_list_goldens(self):
        if not self.verify_access_token():
            return

        goldens_list = []

        for golden in WatchWebAppRequestHandler.test_entity.golden_watcher.cached_goldens.values():
            goldens_list.append(self.create_golden_data(golden))

        #updating the goldens list
        WatchWebAppRequestHandler.test_entity.goldens_list = goldens_list
        self.send_json(goldens_list)

    def create_golden_data(self, golden):
        golden_data = {}
        golden_data["id"] = golden.id
        golden_data["result"] = golden.result
        golden_data["label"] = golden.golden_identifier
        golden_data["goldenRepoPath"] = golden.golden_repo_path
        golden_data["updated"] = golden.updated
        golden_data["testClassName"] = golden.test_class_name
        golden_data["testMethodName"] = golden.test_method_name
        golden_data["testTime"] = golden.test_time
        golden_data["goldenName"] = golden.golden_name

        golden_data["actualUrl"] = (
            f"{WatchWebAppRequestHandler.this_server_address}/golden/"
            f"{golden.checksum}/"
            f"{golden.local_file[len(WatchWebAppRequestHandler.test_entity
                                     .golden_watcher.temp_dir) + 1 :]}"
        )
        expected_file = path.join(
                            WatchWebAppRequestHandler.android_build_top,
                            golden.golden_repo_path
                        )

        if os.path.exists(expected_file):
            golden_data["expectedUrl"] = (
                f"{WatchWebAppRequestHandler.this_server_address}/expected/{golden.id}"
            )

        if golden.video_location:
            golden_data["videoUrl"] = (
                f"{WatchWebAppRequestHandler.this_server_address}/golden/"
                f"{golden.checksum}/{golden.video_location}"
            )

        return golden_data

    def service_presubmit_artifact_list(self, invocation_id):
        try:
            golden_watcher = (
                GoldenWatcherFactory.create_watcher(
                    GoldenWatcherTypes.PRESUBMIT,
                    WatchWebAppRequestHandler.temp_dir
                )
            )
            presubmit_fetch_client = (
                FetchPresubmitTestArtifacts(invocation_id,
                                            golden_watcher
                                            .artifacts_download_dir)
            )
            WatchWebAppRequestHandler.test_entity = TestEntity(
                golden_watcher=golden_watcher,
                download_client=presubmit_fetch_client
            )
            WatchWebAppRequestHandler.test_entity_cache[
                GoldenWatcherTypes.PRESUBMIT.value] = WatchWebAppRequestHandler.test_entity

            self.serve_presubmit_data(presubmit_fetch_client
                                                     .list_presubmit_test_artifacts())
        except requests.exceptions.RequestException as exception:
            if exception.response.status_code == 500:
                self.send_error_with_message(503, str(exception))
            else:
                self.send_error_with_message(exception.response.status_code, str(exception))
        except Exception as exception:
            self.send_error_with_message(500, str(exception))

    def serve_presubmit_data(self, test_list):
        presubmit_data = []
        for test in test_list:
            presubmit_data_json = {}
            presubmit_data_json["testname"] = test
            presubmit_data.append(presubmit_data_json)
        #updating the goldens list
        WatchWebAppRequestHandler.test_entity.goldens_list = presubmit_data
        self.send_json(presubmit_data)

    def service_fetch_artifacts(self, test_name):
        try:
            artifacts = (WatchWebAppRequestHandler.test_entity.download_client
                            .download_presubmit_test_artifact_for_test_name(test_name))
            if artifacts:
                (WatchWebAppRequestHandler.test_entity.golden_watcher
                .refresh_golden_files(artifacts, test_name))
            for golden in (WatchWebAppRequestHandler.test_entity.golden_watcher
                           .cached_goldens.values()):
                if golden.golden_name == test_name:
                    self.send_json(self.create_golden_data(golden))
                    break
        except requests.exceptions.RequestException as exception:
            if exception.response.status_code == 500:
                self.send_error_with_message(503, str(exception))
            else:
                self.send_error_with_message(exception.response.status_code, str(exception))
        except Exception as exception:
            self.send_error_with_message(500, str(exception))

    def get_available_modes(self):
        '''
        Collects all adb devices available and send them along with modes like
        robolectric and atest as available test mode options.
        '''
        available_modes = []
        WatchWebAppRequestHandler.adb_serial_finder.update_model_serial_map()
        if WatchWebAppRequestHandler.adb_serial_finder.model_serial_map:
            available_modes = list(WatchWebAppRequestHandler.adb_serial_finder
                                   .model_serial_map.keys())
        available_modes.append(GoldenWatcherTypes.ATEST.value)
        available_modes.append(GoldenWatcherTypes.ROBOLECTRIC.value)

        print(f"available modes: {available_modes}")
        self.send_json(available_modes)

    def switch_mode(self, mode: GoldenWatcherTypes):
        print(f'Switched to: {mode}')

        #If found in cache, served from cache.
        #If files changed then need to run refresh.

        if mode in WatchWebAppRequestHandler.test_entity_cache:
            (WatchWebAppRequestHandler
            .test_entity) = WatchWebAppRequestHandler.test_entity_cache[mode]
            self.send_json(WatchWebAppRequestHandler.test_entity.goldens_list)

        else:
            try:
                golden_watcher = None
                match(mode):
                    case GoldenWatcherTypes.ROBOLECTRIC.value:
                        golden_watcher = GoldenWatcherFactory.create_watcher(
                                            GoldenWatcherTypes.ROBOLECTRIC,
                                            os.path.join(WatchWebAppRequestHandler
                                                         .temp_dir,mode)
                                        )

                    case GoldenWatcherTypes.ATEST.value:
                        golden_watcher = GoldenWatcherFactory.create_watcher(
                                            GoldenWatcherTypes.ATEST,
                                            os.path.join(WatchWebAppRequestHandler
                                                         .temp_dir,mode)
                                        )

                    case _:
                        '''
                            If not matched with above two test modes,
                            it must be an ADB device connected.
                            If not raise exception.

                            Else, create adb client and move on.
                        '''
                        if mode not in (WatchWebAppRequestHandler
                                        .adb_serial_finder.model_serial_map):
                            raise ValueError("Mode not supported")
                        serial = (WatchWebAppRequestHandler.adb_serial_finder
                                  .model_serial_map.get(mode))
                        adb_client = AdbClient(serial)
                        if not adb_client.run_as_root():
                            raise Exception("Cannot run ADB as root.")

                        golden_watcher = GoldenWatcherFactory.create_watcher(
                                            GoldenWatcherTypes.ADB,
                                            os.path.join(WatchWebAppRequestHandler
                                                         .temp_dir,mode),
                                            adb_client
                                        )
                (WatchWebAppRequestHandler
                .test_entity) = TestEntity(golden_watcher=golden_watcher)
                (WatchWebAppRequestHandler
                .test_entity_cache[mode]) = WatchWebAppRequestHandler.test_entity
                self.service_list_goldens()
            except Exception as ex:
                print(f"Failure occurred: {ex}")
                self.send_error_with_message(503, f"Failure occurred: {ex}")
                return

    def service_refresh_goldens(self, clear):
        if not WatchWebAppRequestHandler.test_entity.golden_watcher:
            # no test mode selected
            self.send_error_with_message(400, "No test mode selected !")
            return None

        if clear:
            WatchWebAppRequestHandler.test_entity.golden_watcher.clean()
        WatchWebAppRequestHandler.test_entity.golden_watcher.refresh_golden_files()
        self.service_list_goldens()

    def service_update_golden(self, update_golden_id_set):
        '''
        Find goldens with IDs in update_golden_id_set and updates expected values.
        '''
        if len(update_golden_id_set) == 0:
            self.send_json({}, 400)
        result = {}
        success_count = 0

        goldens = WatchWebAppRequestHandler.test_entity.golden_watcher.cached_goldens.values()
        for golden in goldens:
            if golden.id not in update_golden_id_set:
                print("skip", golden.id)
                continue
            try:
                dst = path.join(WatchWebAppRequestHandler.android_build_top,
                                golden.golden_repo_path)
                if not path.exists(path.dirname(dst)):
                    os.makedirs(path.dirname(dst))

                shutil.copyfile(golden.local_file, dst)

                golden.updated = True
                result[golden.id] = "Updated"
                success_count += 1
            except Exception as e:
                result[golden.id] = f"Failed with exception: {e}"

        if success_count == len(update_golden_id_set):
            self.send_json(result)
        elif success_count == 0:
            self.send_json(result, 400)
        else:
            self.send_json(result, 207)

    def send_json(self, data, status_code=200):
        # Replace this with code that generates your JSON data
        data_encoded = json.dumps(data).encode("utf-8")
        self.send_response(status_code)
        self.send_header("Content-type", "application/json")
        self.add_standard_headers()
        self.end_headers()
        self.wfile.write(data_encoded)

    def add_standard_headers(self):
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "POST, PUT, GET, OPTIONS")
        self.send_header(
            "Access-Control-Allow-Headers",
            GOLDEN_ACCESS_TOKEN_HEADER
            + ", Content-Type, Content-Length, Range, Accept-ranges",
        )
        # Accept-ranges: bytes is needed for chrome to allow seeking the
        # video. At this time, won't handle ranges on subsequent gets,
        # but that is likely OK given the size of these videos and that
        # its local only.
        self.send_header("Accept-ranges", "bytes")