import time
import hmac as _hmac
import hashlib
import base64

# maximum age of a valid payload in seconds
# payloads older than this are rejected as potential replays
PAYLOAD_MAX_AGE_SECONDS = 30
CLOCK_SKEW_TOLERANCE_MS = 10_000  # allow 10s difference between phone and laptop clocks

VALID_COMMANDS = {"unlock", "lock"}

# nonce cache: maps nonce string -> timestamp_ms when first seen
# entries are evicted once they fall outside the replay window
_seen_nonces: dict[str, int] = {}


def _evict_expired_nonces() -> None:
    """Remove nonces that are now outside the replay window."""
    cutoff_ms = (time.time() - PAYLOAD_MAX_AGE_SECONDS) * 1000
    expired = [n for n, t in _seen_nonces.items() if t < cutoff_ms]
    for n in expired:
        del _seen_nonces[n]


def is_unique_nonce(nonce: str, timestamp_ms: int) -> bool:
    """
    Returns True if this nonce has not been seen before within the replay window.
    Registers the nonce on first use so subsequent identical payloads are rejected.
    """
    _evict_expired_nonces()
    if nonce in _seen_nonces:
        return False
    _seen_nonces[nonce] = timestamp_ms
    return True


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


def sign_payload(data: str, secret: str) -> str:
    """Sign {command}:{nonce}:{timestamp} with the given HMAC secret."""
    mac = _hmac.new(secret.encode(), data.encode(), hashlib.sha256)
    return base64.b64encode(mac.digest()).decode()


def verify_signature(data: str, signature: str, secret: str) -> bool:
    """Returns True if the signature matches the expected HMAC for data."""
    expected = sign_payload(data, secret)
    return _hmac.compare_digest(expected, signature)


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
    if not is_unique_nonce(parsed["nonce"], parsed["timestamp"]):
        return None, "replayed nonce"
    return parsed["command"], None
