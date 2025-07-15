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

class WatchWebAppRequestHandler(http.server.BaseHTTPRequestHandler):
    secret_token = None
    golden_watcher = None
    temp_dir = None
    android_build_top = None
    this_server_address = None
    presubmit_fetch_client = None
    adb_serial_finder = ADBSerialFinder()
    golden_watcher_cache = {}

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

        if parsed.path == "/service/list":
            self.service_list_goldens()
            return
        elif parsed.path == "/service/testModes/list":
            self.get_available_modes()
            return
        elif parsed.path.startswith("/golden/"):
            requested_file_start_index = parsed.path.find("/", len("/golden/") + 1)
            requested_file = parsed.path[requested_file_start_index + 1 :]
            self.serve_file(
                WatchWebAppRequestHandler.golden_watcher.temp_dir,
                requested_file
            )
            return
        elif parsed.path.startswith("/expected/"):
            golden_id = parsed.path[len("/expected/") :]

            goldens = WatchWebAppRequestHandler.golden_watcher.cached_goldens.values()
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
            self.send_json(res)
            return

        self.send_error(404)

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
        else:
            self.send_error(404)

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
            self.send_error(404)

    def serve_file(self, root_directory, file_relative_to_root, mime_type=None):
        resolved_path = path.abspath(path.join(root_directory, file_relative_to_root))

        if path.commonprefix(
            [resolved_path, root_directory]
        ) == root_directory and path.isfile(resolved_path):
            self.send_response(200)
            self.send_header(
                "Content-type", mime_type or mimetypes.guess_type(resolved_path)[0]
            )
            self.add_standard_headers()

            if resolved_path.endswith("screenshots.zip"):

                if ZipToVideoConverter.process_single_zip(pathlib.Path(resolved_path)):
                    video_path = resolved_path.replace("zip","mp4")
                    if pathlib.Path(video_path).is_file():
                        self.send_header(
                            "Content-type", "video/mp4"
                        )
                        self.end_headers()
                        with open(video_path, "rb") as f:
                            self.wfile.write(f.read())

            else :
                self.end_headers()
                with open(resolved_path, "rb") as f:
                    self.wfile.write(f.read())

        else:
            self.send_error(404)

    def service_list_goldens(self):
        if not self.verify_access_token():
            return

        goldens_list = []

        for golden in WatchWebAppRequestHandler.golden_watcher.cached_goldens.values():
            goldens_list.append(self.create_golden_data(golden))

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
            f"{golden.local_file[len(WatchWebAppRequestHandler.
                                     golden_watcher.temp_dir) + 1 :]}"
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
            WatchWebAppRequestHandler.golden_watcher = (
                GoldenWatcherFactory.create_watcher(
                    GoldenWatcherTypes.PRESUBMIT,
                    WatchWebAppRequestHandler.golden_watcher.temp_dir
                )
            )
            WatchWebAppRequestHandler.presubmit_fetch_client = (
                FetchPresubmitTestArtifacts(invocation_id,
                                            WatchWebAppRequestHandler
                                            .golden_watcher
                                            .artifacts_download_dir)
            )
            artifacts_list = (WatchWebAppRequestHandler.presubmit_fetch_client.
                              list_presubmit_test_artifacts())
            self.send_json(artifacts_list)
        except requests.exceptions.RequestException as exception:
            if exception.response.status_code == 500:
                self.send_error(503, str(exception))
            else:
                self.send_error(exception.response.status_code, str(exception))
        except Exception as exception:
            self.send_error(500, str(exception))

    def service_fetch_artifacts(self, test_name):
        try:
            artifacts = (WatchWebAppRequestHandler.presubmit_fetch_client
                            .download_presubmit_test_artifact_for_test_name(test_name))
            if artifacts:
                (WatchWebAppRequestHandler.golden_watcher
                .refresh_golden_files(artifacts, test_name))
            for golden in (WatchWebAppRequestHandler.golden_watcher
                           .cached_goldens.values()):
                if golden.golden_name == test_name:
                    self.send_json(self.create_golden_data(golden))
                    break
        except requests.exceptions.RequestException as exception:
            if exception.response.status_code == 500:
                self.send_error(503, str(exception))
            else:
                self.send_error(exception.response.status_code, str(exception))
        except Exception as exception:
            self.send_error(500, str(exception))

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

        if mode in WatchWebAppRequestHandler.golden_watcher_cache:
            (WatchWebAppRequestHandler
            .golden_watcher) = WatchWebAppRequestHandler.golden_watcher_cache[mode]

        else:
            try:
                match(mode):
                    case GoldenWatcherTypes.ROBOLECTRIC.value:
                        (WatchWebAppRequestHandler
                        .golden_watcher) = GoldenWatcherFactory.create_watcher(
                                            GoldenWatcherTypes.ROBOLECTRIC,
                                            os.path.join(WatchWebAppRequestHandler
                                                         .temp_dir,mode)
                                        )

                    case GoldenWatcherTypes.ATEST.value:
                        (WatchWebAppRequestHandler
                        .golden_watcher) = GoldenWatcherFactory.create_watcher(
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

                        (WatchWebAppRequestHandler
                        .golden_watcher) = GoldenWatcherFactory.create_watcher(
                                            GoldenWatcherTypes.ADB,
                                            os.path.join(WatchWebAppRequestHandler
                                                         .temp_dir,mode),
                                            adb_client
                                        )

                (WatchWebAppRequestHandler
                .golden_watcher_cache[mode]) = WatchWebAppRequestHandler.golden_watcher
            except Exception as ex:
                print(f"Failure occurred: {ex}")
                self.send_error(503)
                return

        self.service_list_goldens()

    def service_refresh_goldens(self, clear):
        if clear:
            WatchWebAppRequestHandler.golden_watcher.clean()
        WatchWebAppRequestHandler.golden_watcher.refresh_golden_files()
        self.service_list_goldens()

    def service_update_golden(self, update_golden_id_set):
        '''
        Find goldens with IDs in update_golden_id_set and updates expected values.
        '''
        if len(update_golden_id_set) == 0:
            self.send_json({}, 400)
        result = {}
        success_count = 0

        goldens = WatchWebAppRequestHandler.golden_watcher.cached_goldens.values()
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