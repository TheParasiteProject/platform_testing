import os
from impl.golden_watchers.golden_watcher_types import GoldenWatcherTypes
from impl.golden_watchers.atest_golden_watcher import AtestGoldenWatcher
from impl.golden_watchers.robolectric_golden_watcher import RobolectricGoldenWatcher
from impl.golden_watchers.golden_file_watcher import GoldenFileWatcher

class GoldenWatcherFactory:

    @staticmethod
    def create_watcher(type: GoldenWatcherTypes, tmpdir, adb_client = None):

        match type:
            case GoldenWatcherTypes.ATEST:
                user = os.environ.get("USER")
                return AtestGoldenWatcher(
                    tmpdir, f"/tmp/atest_result_{user}/LATEST/"
                )

            case GoldenWatcherTypes.ROBOLECTRIC:
                return RobolectricGoldenWatcher(
                    tmpdir, f"/tmp/motion/"
                )

            case GoldenWatcherTypes.FILE:
                if not adb_client:
                    raise ValueError("adb client not found")

                return GoldenFileWatcher(tmpdir, adb_client)

            case _:
                print("No such Golden Watcher exists.")
                raise ValueError("Imporper Golden Watcher Type.")



