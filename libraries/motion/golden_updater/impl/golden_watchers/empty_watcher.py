from impl.golden_watchers.golden_watcher import GoldenWatcher
from impl.cached_golden import CachedGolden

class EmptyWatcher(GoldenWatcher):
    def __init__(self, cached_golden_service=CachedGolden):
        self.cached_golden_service = cached_golden_service
        self.cached_goldens={}

    def clean(self):
        pass

    def refresh_golden_files(self):
        pass