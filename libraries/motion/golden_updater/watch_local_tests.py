#!/usr/bin/env python3

#
# Copyright 2024, The Android Open Source Project
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

import socketserver
import os
import sys
import tempfile
import webbrowser
from impl.golden_watchers.golden_watcher_factory import GoldenWatcherFactory
from impl.golden_watchers.golden_watcher_types import GoldenWatcherTypes
from impl.adb_client import AdbClient
from impl.watch_web_app_request_handler import WatchWebAppRequestHandler
from impl.argument_parser import ArgumentParser
from impl.token_generator import TokenGenerator
from impl.adb_serial_finder import ADBSerialFinder

def main():

    args = ArgumentParser.get_args()

    if args.android_build_top is None or not os.path.exists(args.android_build_top):
        print("ANDROID_BUILD_TOP not set. Have you sourced envsetup.sh?")
        sys.exit(1)

    android_build_top = args.android_build_top

    with tempfile.TemporaryDirectory() as tmpdir:
        adb_client = None

        if args.atest:
            print("ATEST is running.")
            golden_watcher = GoldenWatcherFactory.create_watcher(
                GoldenWatcherTypes.ATEST,tmpdir
            )

        elif args.robolectricTest:
            print("Running for devicess sysui studio test")
            golden_watcher = GoldenWatcherFactory.create_watcher(
                GoldenWatcherTypes.ROBOLECTRIC, tmpdir
            )
        elif args.none:
            print("Running thin client")
            golden_watcher = GoldenWatcherFactory.create_watcher(
                GoldenWatcherTypes.NONE, tmpdir
            )
        else:
            serial = args.serial or ADBSerialFinder.get_serial()
            if not serial:
                sys.exit(1)
            adb_client = AdbClient(serial)
            if not adb_client.run_as_root():
                sys.exit(1)
            golden_watcher = GoldenWatcherFactory.create_watcher(
                GoldenWatcherTypes.ADB, tmpdir, adb_client
            )

        this_server_address = f"http://localhost:{args.port}"

        secret_token = TokenGenerator.get_token()
        WatchWebAppRequestHandler.secret_token = secret_token
        WatchWebAppRequestHandler.android_build_top = android_build_top
        WatchWebAppRequestHandler.golden_watcher = golden_watcher
        WatchWebAppRequestHandler.this_server_address = this_server_address
        WatchWebAppRequestHandler.adb_client = adb_client
        (WatchWebAppRequestHandler
         .golden_watcher_cache[golden_watcher.type.value]) = golden_watcher

        with socketserver.TCPServer(
            ("localhost", args.port), WatchWebAppRequestHandler, golden_watcher
        ) as httpd:
            uiAddress = f"{args.client_url}?token={secret_token}&port={args.port}"
            print(f"Open UI at {uiAddress}")
            webbrowser.open(uiAddress)
            try:
                httpd.serve_forever()
            except KeyboardInterrupt:
                httpd.shutdown()
                print("Shutting down")



if __name__ == "__main__":
    main()