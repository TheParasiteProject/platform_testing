import http.server
import urllib.parse
import json
import mimetypes
import shutil
from os import path
import pathlib
import os
from impl.constants import GOLDEN_ACCESS_TOKEN_HEADER
from impl.zip_to_video_converter import ZipToVideoConverter

class WatchWebAppRequestHandler(http.server.BaseHTTPRequestHandler):
    secret_token = None
    golden_watcher = None
    android_build_top = None
    this_server_address = None

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
        elif parsed.path.startswith("/golden/"):
            requested_file_start_index = parsed.path.find("/", len("/golden/") + 1)
            requested_file = parsed.path[requested_file_start_index + 1 :]
            self.serve_file(WatchWebAppRequestHandler.golden_watcher.temp_dir, requested_file)
            return
        elif parsed.path.startswith("/expected/"):
            golden_id = parsed.path[len("/expected/") :]

            goldens = WatchWebAppRequestHandler.golden_watcher.cached_goldens.values()
            for golden in goldens:
                if golden.id != golden_id:
                    continue

                self.serve_file(
                    WatchWebAppRequestHandler.android_build_top, golden.golden_repo_path, "application/json"
                )
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
        else:
            self.send_error(404)

    def do_PUT(self):
        if not self.verify_access_token():
            return

        parsed = urllib.parse.urlparse(self.path)
        query_params = urllib.parse.parse_qs(parsed.query)

        if parsed.path == "/service/update":
            self.service_update_golden(query_params["id"][0])
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
                    if pathlib.Path(video_path).is_file() :
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

            golden_data = {}
            golden_data["id"] = golden.id
            golden_data["result"] = golden.result
            golden_data["label"] = golden.golden_identifier
            golden_data["goldenRepoPath"] = golden.golden_repo_path
            golden_data["updated"] = golden.updated
            golden_data["testClassName"] = golden.test_class_name
            golden_data["testMethodName"] = golden.test_method_name
            golden_data["testTime"] = golden.test_time

            golden_data["actualUrl"] = (
                f"{WatchWebAppRequestHandler.this_server_address}/golden/{golden.checksum}/{golden.local_file[len(WatchWebAppRequestHandler.golden_watcher.temp_dir) + 1 :]}"
            )
            expected_file = path.join(WatchWebAppRequestHandler.android_build_top, golden.golden_repo_path)
            if os.path.exists(expected_file):
                golden_data["expectedUrl"] = (
                    f"{WatchWebAppRequestHandler.this_server_address}/expected/{golden.id}"
                )

            golden_data["videoUrl"] = (
                f"{WatchWebAppRequestHandler.this_server_address}/golden/{golden.checksum}/{golden.video_location}"
            )

            goldens_list.append(golden_data)

        self.send_json(goldens_list)

    def service_refresh_goldens(self, clear):
        if clear:
            WatchWebAppRequestHandler.golden_watcher.clean()
        WatchWebAppRequestHandler.golden_watcher.refresh_golden_files()
        self.service_list_goldens()

    def service_update_golden(self, id):
        goldens = WatchWebAppRequestHandler.golden_watcher.cached_goldens.values()
        for golden in goldens:
            if golden.id != id:
                print("skip", golden.id)
                continue

            dst = path.join(WatchWebAppRequestHandler.android_build_top, golden.golden_repo_path)
            if not path.exists(path.dirname(dst)):
                os.makedirs(path.dirname(dst))

            shutil.copyfile(golden.local_file, dst)

            golden.updated = True
            self.send_json({"result": "OK"})
            return

        self.send_error(400)

    def send_json(self, data):
        # Replace this with code that generates your JSON data
        data_encoded = json.dumps(data).encode("utf-8")
        self.send_response(200)
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