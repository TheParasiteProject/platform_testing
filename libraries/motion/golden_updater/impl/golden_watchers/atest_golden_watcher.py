import glob
import re
import os
import gzip
import shutil
from impl.cached_golden import CachedGolden
from impl.golden_watchers.golden_watcher import GoldenWatcher

class AtestGoldenWatcher(GoldenWatcher):

    def __init__(self, temp_dir, atest_latest_dir):
        self.temp_dir = temp_dir
        self.atest_latest_dir = atest_latest_dir

        # name -> CachedGolden
        self.cached_goldens = {}
        self.refresh_golden_files()

    def clean(self):
        self.cached_goldens = {}

    def refresh_golden_files(self):

        # Atest writes the files with a wide variety of filenames. Examples
        # log/stub/local_atest/inv_8184127433410125702/light_portrait_pagingRight.actual.json_4383267726505225616.txt.gz
        # log/invocation_3042186109657619915/inv_5155363728971335727/recordMotion_captureCrossfade.actual_10536896158799342698.json
        # log/stub/local_atest/inv_6860054371355660320/light_portrait_noOverscrollRight.actual_118505410949600545.json.gz

        # log/stub/local_atest/inv_6860054371355660320/light_portrait_noOverscrollRight.actual_12613191689435798576.mp4
        # log/invocation_3042186109657619915/inv_5155363728971335727/recordMotion_captureCrossfade.actual_1988198704080929506.mp4
        # log/stub/local_atest/inv_8184127433410125702/light_portrait_pagingRight.actual.mp4_1617964025478041468.txt.gz

        pattern_type = (
            r".*/(?P<name>.*)\.actual((\.(?P<ext1>[a-zA-Z0-9]+)_(?P<hash1>\d+)\.txt)|(_(?P<hash2>\d+)\.(?P<ext2>[a-zA-Z0-9]+)))(?P<compressed>\.gz)?"
        )

        # Output from on-device runs
        # Modifying the search regex to handle files not ending with json as given above.
        for filename in glob.iglob(
            f"{self.atest_latest_dir}//**/*.actual*json*", recursive=True
        ):

            match = re.search(pattern_type, filename)

            if not match:
                continue

            golden_name = match.group("name")
            ext = match.group("ext1") or match.group("ext2")
            hash = match.group("hash1") or match.group("hash2")
            is_compressed = match.group("compressed") == ".gz"

            local_file = os.path.join(self.temp_dir, f"{golden_name}_{hash}.actual.json")
            self.copy_file(filename, local_file, is_compressed)
            golden = CachedGolden(filename, local_file)

            if golden.video_location:
                for video_filename in glob.iglob(
                    f"{self.atest_latest_dir}/**/{golden_name}.actual*.mp4*",
                    recursive=True,
                ):

                    local_video_file = os.path.join(
                        self.temp_dir, golden.video_location
                    )
                    video_is_compressed = video_filename.endswith(".gz")
                    self.copy_file(
                        video_filename, local_video_file, video_is_compressed
                    )

                    break

            self.cached_goldens[filename] = golden

    def copy_file(self, source, target, is_compressed):
        os.makedirs(os.path.dirname(target), exist_ok=True)

        if is_compressed:
            with gzip.open(source, "rb") as f_in:
                with open(target, "wb") as f_out:
                    shutil.copyfileobj(f_in, f_out)
        else:
            shutil.copyfile(source, target)