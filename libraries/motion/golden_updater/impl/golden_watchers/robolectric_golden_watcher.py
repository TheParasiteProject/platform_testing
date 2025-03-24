import glob
import os
import hashlib
import datetime
import shutil
from impl.cached_golden import CachedGolden
from impl.golden_watchers.golden_watcher import GoldenWatcher

class RobolectricGoldenWatcher(GoldenWatcher):

    def __init__(self, temp_dir, latest_dir):
        self.temp_dir = temp_dir
        self.latest_dir = latest_dir
        self.cached_goldens = {}
        self.refresh_golden_files()

    def refresh_golden_files(self):
        for filename in glob.iglob(
            f"{self.latest_dir}//**/*.actual.json", recursive=True
        ):
            baseName = os.path.basename(filename)
            baseFilename, ext = os.path.splitext(baseName)
            timeHash = hashlib.md5(datetime.datetime.now().isoformat().encode("utf-8")).hexdigest()
            local_file = os.path.join(self.temp_dir, f'copy_{baseFilename}_{timeHash}{ext}')
            self.copy_file(filename, local_file)
            golden = CachedGolden(filename, local_file)
            self.cached_goldens[filename] = golden

    def copy_file(self, source, target):
        os.makedirs(os.path.dirname(target), exist_ok=True)
        shutil.copyfile(source, target)

    def clean(self):
        self.cached_goldens = {}