import socket

class PortFinder:

    @staticmethod
    def find_free_port():
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
            s.bind(("", 0))  # Bind to a random free port provided by the OS
            return s.getsockname()[1]  # Get the port number