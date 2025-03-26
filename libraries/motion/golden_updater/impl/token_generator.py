import secrets
import os
from impl.constants import GOLDEN_ACCESS_TOKEN_LOCATION

class TokenGenerator:

    def get_token() -> str:
        try:
            with open(GOLDEN_ACCESS_TOKEN_LOCATION, "r") as token_file:
                token = token_file.readline()
                return token
        except IOError:
            token = secrets.token_hex(32)
            os.makedirs(os.path.dirname(GOLDEN_ACCESS_TOKEN_LOCATION), exist_ok=True)
            try:
                with open(GOLDEN_ACCESS_TOKEN_LOCATION, "w") as token_file:
                    token_file.write(token)
                os.chmod(GOLDEN_ACCESS_TOKEN_LOCATION, 0o600)
            except IOError:
                print(
                    "Unable to save persistent token {} to {}".format(
                        token, GOLDEN_ACCESS_TOKEN_LOCATION
                    )
                )
            return token