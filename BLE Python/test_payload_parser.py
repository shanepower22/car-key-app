import time
import pytest
from payload_parser import parse_payload, is_fresh, is_valid_command, validate_payload


# parse_payload tests

def test_parse_valid_payload():
    result = parse_payload("unlock:abc123:1712345678901:somesig==")
    assert result is not None
    assert result["command"] == "unlock"
    assert result["nonce"] == "abc123"
    assert result["timestamp"] == 1712345678901
    assert result["signature"] == "somesig=="

def test_parse_returns_none_for_too_few_parts():
    assert parse_payload("unlock:abc123:1712345678901") is None

def test_parse_returns_none_for_too_many_parts():
    assert parse_payload("unlock:abc:123:sig:extra") is None

def test_parse_returns_none_for_non_numeric_timestamp():
    assert parse_payload("unlock:abc123:notanumber:sig") is None

def test_parse_returns_none_for_empty_string():
    assert parse_payload("") is None


# is_fresh tests

def test_is_fresh_current_timestamp():
    now_ms = int(time.time() * 1000)
    assert is_fresh(now_ms) is True

def test_is_fresh_rejects_old_timestamp():
    old_ms = int(time.time() * 1000) - (60 * 1000)  # 60 seconds ago
    assert is_fresh(old_ms) is False

def test_is_fresh_rejects_future_timestamp():
    future_ms = int(time.time() * 1000) + (10 * 1000)  # 10 seconds ahead
    assert is_fresh(future_ms) is False


# is_valid_command tests

def test_valid_command_unlock():
    assert is_valid_command("unlock") is True

def test_valid_command_lock():
    assert is_valid_command("lock") is True

def test_invalid_command_unknown():
    assert is_valid_command("start") is False

def test_invalid_command_empty():
    assert is_valid_command("") is False


# validate_payload tests

def test_validate_rejects_malformed():
    cmd, err = validate_payload("unlock:abc")
    assert cmd is None
    assert "malformed" in err

def test_validate_rejects_unknown_command():
    now_ms = int(time.time() * 1000)
    cmd, err = validate_payload(f"start:abc123:{now_ms}:sig==")
    assert cmd is None
    assert "unknown command" in err

def test_validate_rejects_expired_payload():
    old_ms = int(time.time() * 1000) - (60 * 1000)
    cmd, err = validate_payload(f"unlock:abc123:{old_ms}:sig==")
    assert cmd is None
    assert "expired" in err

def test_validate_returns_command_on_success():
    now_ms = int(time.time() * 1000)
    cmd, err = validate_payload(f"unlock:abc123:{now_ms}:sig==")
    assert cmd == "unlock"
    assert err is None
