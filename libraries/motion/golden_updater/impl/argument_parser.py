import argparse
import os
from impl.port_finder import PortFinder

class ArgumentParser:

    @staticmethod
    def get_args():
        parser = argparse.ArgumentParser(
            "Watches a connected device for golden file updates."
        )

        parser.add_argument(
            "--port",
            default = PortFinder.find_free_port(),
            type=int,
            help="Port to run test at watcher web UI on.",
        )

        parser.add_argument(
            "--atest",
            default=False,
            help="Watches atest output",
        )

        parser.add_argument(
            "--robolectricTest",
            default=False,
            help="Watch for artifacs generated when a deviceless test is run via SysUi Studio",
        )

        parser.add_argument(
            "--serial",
            default=os.environ.get("ANDROID_SERIAL"),
            help="The ADB device serial to pull goldens from.",
        )

        parser.add_argument(
            "--android_build_top",
            default=os.environ.get("ANDROID_BUILD_TOP"),
            help="The root directory of the android checkout.",
        )

        parser.add_argument(
            "--client_url",
            default="http://motion.teams.x20web.corp.google.com/",
            help="The URL where the client app is deployed.",
        )

        return parser.parse_args()