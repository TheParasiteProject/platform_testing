from abc import ABC, abstractmethod

class GoldenWatcher(ABC):

    @abstractmethod
    def clean(self):
        pass

    @abstractmethod
    def refresh_golden_files(self):
        pass

