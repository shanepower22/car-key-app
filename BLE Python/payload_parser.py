import time

# maximum age of a valid payload in seconds
# payloads older than this are rejected as potential replays
PAYLOAD_MAX_AGE_SECONDS = 30
CLOCK_SKEW_TOLERANCE_MS = 10_000  # allow 10s difference between phone and laptop clocks

VALID_COMMANDS = {"unlock", "lock"}


def parse_payload(raw: str):
    """
    Parse a signed BLE command payload.
    Expected format: {command}:{nonce}:{timestamp_ms}:{signature}

    Returns a dict with keys command/nonce/timestamp/signature,
    or None if the format is invalid.
    """
    parts = raw.strip().split(":")
    if len(parts) != 4:
        return None
    command, nonce, timestamp_str, signature = parts
    try:
        timestamp = int(timestamp_str)
    except ValueError:
        return None
    return {
        "command": command,
        "nonce": nonce,
        "timestamp": timestamp,
        "signature": signature
    }


def is_valid_command(command: str) -> bool:
    return command in VALID_COMMANDS


def is_fresh(timestamp_ms: int, max_age_seconds: int = PAYLOAD_MAX_AGE_SECONDS) -> bool:
    """
    Returns True if the payload timestamp is within the acceptable age window.
    Rejects payloads that are too old (replay) or timestamped in the future (clock skew attack).
    """
    now_ms = time.time() * 1000
    age_ms = now_ms - timestamp_ms
    return -CLOCK_SKEW_TOLERANCE_MS <= age_ms <= (max_age_seconds * 1000)


def validate_payload(raw: str):
    """
    Full validation pipeline. Returns (command, error_message).
    On success: (command_string, None)
    On failure: (None, reason_string)
    """
    parsed = parse_payload(raw)
    if parsed is None:
        return None, "malformed payload"
    if not is_valid_command(parsed["command"]):
        return None, f"unknown command: {parsed['command']}"
    if not is_fresh(parsed["timestamp"]):
        return None, "payload expired or timestamp out of range"
    return parsed["command"], None
