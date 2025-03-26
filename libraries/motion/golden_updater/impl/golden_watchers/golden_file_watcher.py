import os
import hashlib
from impl.cached_golden import CachedGolden
from impl.golden_watchers.golden_watcher import GoldenWatcher

class GoldenFileWatcher(GoldenWatcher):

    def __init__(self, temp_dir, adb_client):
        self.temp_dir = temp_dir
        self.adb_client = adb_client

        # name -> CachedGolden
        self.cached_goldens = {}
        self.refresh_golden_files()

    def clean(self):
        self.cached_goldens = {}

    def refresh_golden_files(self):
        command = f"find /data/user/0/ -type f -name *.actual.json"
        updated_goldens = self.run_adb_command(["shell", command]).splitlines()
        print(f"Updating goldens - found {len(updated_goldens)} files")

        for golden_remote_file in updated_goldens:
            local_file = self.adb_pull(golden_remote_file)

            golden = CachedGolden(golden_remote_file, local_file)
            if golden.video_location:
                self.adb_pull_image(golden.device_local_path, golden.video_location)

            self.cached_goldens[golden_remote_file] = golden

    def adb_pull(self, remote_file):
        baseName = os.path.basename(remote_file)
        filename, ext = os.path.splitext(baseName)
        remoteFilenameHash = hashlib.md5(remote_file.encode("utf-8")).hexdigest()
        local_file = os.path.join(self.temp_dir, f'{filename}_{remoteFilenameHash}{ext}')
        self.run_adb_command(["pull", remote_file, local_file])
        self.run_adb_command(["shell", "rm", remote_file])
        return local_file

    def adb_pull_image(self, remote_dir, remote_file):
        remote_path = os.path.join(remote_dir, remote_file)
        local_path = os.path.join(self.temp_dir, remote_file)
        os.makedirs(os.path.dirname(local_path), exist_ok=True)
        self.run_adb_command(["pull", remote_path, local_path])
        self.run_adb_command(["shell", "rm", remote_path])
        return local_path

    def run_adb_command(self, args):
        return self.adb_client.run_adb_command(args)