import subprocess

class AdbClient:
    def __init__(self, adb_serial):
        self.adb_serial = adb_serial

    def run_as_root(self):
        root_result = self.run_adb_command(["root"])
        if "restarting adbd as root" in root_result:
            self.wait_for_device()
            return True
        if "adbd is already running as root" in root_result:
            return True

        print(f"run_as_root returned [{root_result}]")

        return False

    def wait_for_device(self):
        self.run_adb_command(["wait-for-device"])

    def run_adb_command(self, args):
        command = ["adb"]
        command += ["-s", self.adb_serial]
        command += args
        return subprocess.run(command, check=True, capture_output=True).stdout.decode(
            "utf-8"
        )