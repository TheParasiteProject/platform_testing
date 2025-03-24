import hashlib
import datetime
import json

class CachedGolden:

    def __init__(self, remote_file, local_file):
        self.id = hashlib.md5(remote_file.encode("utf-8")).hexdigest()
        self.remote_file = remote_file
        self.local_file = local_file
        self.updated = False
        self.test_time = datetime.datetime.now().isoformat()
        # Checksum is the time the test data was loaded, forcing unique URLs
        # every time the golden is reloaded
        self.checksum = hashlib.md5(self.test_time.encode("utf-8")).hexdigest()

        motion_golden_data = None
        with open(local_file, "r") as json_file:
            motion_golden_data = json.load(json_file)
        metadata = motion_golden_data["//metadata"]

        self.result = metadata["result"]
        self.golden_repo_path = metadata["goldenRepoPath"]
        self.golden_identifier = metadata["goldenIdentifier"]
        self.test_class_name = metadata["testClassName"]
        self.test_method_name = metadata["testMethodName"]
        self.device_local_path = metadata["deviceLocalPath"]
        self.video_location = None
        if "videoLocation" in metadata:
            self.video_location = metadata["videoLocation"]

        with open(local_file, "w") as json_file:
            del motion_golden_data["//metadata"]
            json.dump(motion_golden_data, json_file, indent=2)