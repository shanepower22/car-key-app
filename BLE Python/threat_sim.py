"""
Threat Simulation Script — CarKey Security Demo
Simulates attack scenarios against the VACU validation pipeline.
Each attack runs through the same code the real VACU uses, so rejections
are genuine and appear on the dashboard in real time.

Usage:
    .venv/Scripts/python threat_sim.py
"""

import time
import hmac
import hashlib
import base64
import uuid
from payload_parser import validate_payload, parse_payload, verify_signature, sign_payload, PAYLOAD_MAX_AGE_SECONDS
from cloud import log_event, get_active_key, VEHICLE_ID


def section(title: str):
    print(f"\n{'='*55}")
    print(f"  {title}")
    print(f"{'='*55}")


def run_through_vacu(raw: str, label: str):
    """Runs a raw payload string through the full VACU validation pipeline."""
    print(f"\n[ATTACK] {label}")
    print(f"  Payload: {raw}")

    cmd, error = validate_payload(raw)
    if error:
        parsed = parse_payload(raw)
        nonce = parsed["nonce"] if parsed else str(uuid.uuid4())
        log_event(
            user_id="attacker",
            action="UNKNOWN",
            result="FAILURE",
            nonce=nonce,
            failure_reason=error
        )
        print(f"  REJECTED by VACU: {error}")
        return

    parsed = parse_payload(raw)
    nonce = parsed["nonce"]

    active_key = get_active_key()
    if active_key is None:
        log_event(
            user_id="attacker",
            action=cmd,
            result="FAILURE",
            nonce=nonce,
            failure_reason="no active key"
        )
        print(f"  REJECTED by VACU: no active key for vehicle {VEHICLE_ID}")
        return

    hmac_secret = active_key.get("hmacSecret", "")
    sig_data = f"{parsed['command']}:{parsed['nonce']}:{parsed['timestamp']}"
    if not verify_signature(sig_data, parsed["signature"], hmac_secret):
        log_event(
            user_id="attacker",
            action=cmd,
            result="FAILURE",
            nonce=nonce,
            failure_reason="invalid signature"
        )
        print(f"  REJECTED by VACU: invalid signature")
        return

    print(f"  WARNING: payload accepted — check your test setup")


# ── Attack 1: Replay Attack ───────────────────────────────────────────────────

def simulate_replay_attack():
    section("ATTACK 1: Replay Attack")
    print("Scenario: attacker captures a valid payload and replays it after the")
    print(f"          {PAYLOAD_MAX_AGE_SECONDS}-second validity window has expired.\n")

    # build a payload with a timestamp that's already expired
    old_timestamp = int(time.time() * 1000) - ((PAYLOAD_MAX_AGE_SECONDS + 10) * 1000)
    nonce = uuid.uuid4().hex
    payload = f"unlock:{nonce}:{old_timestamp}:replayed_signature=="

    run_through_vacu(payload, "Replayed expired payload")


# ── Attack 2: Tampered Payload ────────────────────────────────────────────────

def simulate_tamper_attack():
    section("ATTACK 2: Payload Tampering")
    print("Scenario: attacker intercepts a valid 'lock' payload and flips the command")
    print("          to 'unlock'. The original HMAC signature no longer matches.\n")

    active_key = get_active_key()
    if not active_key:
        print("  No active key found — assign a key in the Manager App first")
        return

    secret = active_key.get("hmacSecret", "")
    nonce = uuid.uuid4().hex
    timestamp = int(time.time() * 1000)

    # build a legitimately signed "lock" payload
    original_data = f"lock:{nonce}:{timestamp}"
    sig = sign_payload(original_data, secret)
    print(f"  Original (signed):  lock:{nonce[:8]}...:{timestamp}:{sig[:12]}...")

    # tamper: flip command to "unlock", signature now invalid
    tampered = f"unlock:{nonce}:{timestamp}:{sig}"
    print(f"  Tampered (flipped): unlock:{nonce[:8]}...:{timestamp}:{sig[:12]}...")

    run_through_vacu(tampered, "Tampered payload — command changed, signature invalid")


# ── Attack 3: Malformed Payload ───────────────────────────────────────────────

def simulate_malformed_payload():
    section("ATTACK 3: Malformed Payload")
    print("Scenario: attacker sends a raw BLE command without the expected format,")
    print("          attempting to trigger a vehicle action directly.\n")

    run_through_vacu("unlock", "Raw command with no nonce/timestamp/signature")
    run_through_vacu("unlock:open:now", "3-part payload missing signature")
    run_through_vacu("", "Empty payload")


# ── Attack 4: Unknown Command ─────────────────────────────────────────────────

def simulate_unknown_command():
    section("ATTACK 4: Unknown Command Injection")
    print("Scenario: attacker attempts to inject an unsupported command.\n")

    nonce = uuid.uuid4().hex
    timestamp = int(time.time() * 1000)
    payload = f"start_engine:{nonce}:{timestamp}:somesig=="

    run_through_vacu(payload, "Injected unknown command 'start_engine'")


# ── Attack 5: Future Timestamp ────────────────────────────────────────────────

def simulate_future_timestamp():
    section("ATTACK 5: Clock Skew / Future Timestamp")
    print("Scenario: attacker crafts a payload with a far-future timestamp,")
    print("          attempting to pre-generate a payload for later use.\n")

    future_timestamp = int(time.time() * 1000) + (60 * 60 * 1000)  # 1 hour in future
    nonce = uuid.uuid4().hex
    payload = f"unlock:{nonce}:{future_timestamp}:somesig=="

    run_through_vacu(payload, "Payload with future timestamp (+1 hour)")


# ── Attack 6: Revoked Key ─────────────────────────────────────────────────────

def simulate_revoked_key():
    section("ATTACK 6: Revoked Key Usage")
    print("Scenario: a key has been revoked in the Manager App. The attacker")
    print("          still has the phone but the VACU checks Firestore and rejects.\n")
    print("  NOTE: For this demo, revoke the key in the Manager App first,")
    print("        then press Enter to send the command.\n")

    input("  Press Enter when key is revoked...")

    nonce = uuid.uuid4().hex
    timestamp = int(time.time() * 1000)
    payload = f"unlock:{nonce}:{timestamp}:valid_looking_sig=="

    # this will pass format/timestamp checks but fail the Firestore key check
    cmd, error = validate_payload(payload)
    if error:
        print(f"  Rejected at format stage: {error}")
        return

    active_key = get_active_key()
    if active_key is None:
        log_event(
            user_id="attacker",
            action="unlock",
            result="FAILURE",
            nonce=nonce,
            failure_reason="key revoked — no active key found"
        )
        print("  REJECTED: no active key found — revocation enforced by VACU")
    else:
        print("  Key is still active — revoke it in the Manager App first")


# ── Main ──────────────────────────────────────────────────────────────────────

ATTACKS = {
    "1": ("Replay Attack",          simulate_replay_attack),
    "2": ("Payload Tampering",      simulate_tamper_attack),
    "3": ("Malformed Payload",      simulate_malformed_payload),
    "4": ("Unknown Command",        simulate_unknown_command),
    "5": ("Future Timestamp",       simulate_future_timestamp),
    "6": ("Revoked Key",            simulate_revoked_key),
    "a": ("Run All (except 6)",     None),
}

if __name__ == "__main__":
    print("\nCarKey Threat Simulation")
    print("All rejections are logged to Firestore and appear on the dashboard.\n")

    for key, (name, _) in ATTACKS.items():
        print(f"  [{key}] {name}")

    choice = input("\nSelect attack: ").strip().lower()

    if choice == "a":
        for key in ["1", "2", "3", "4", "5"]:
            ATTACKS[key][1]()
            time.sleep(1)
    elif choice in ATTACKS and ATTACKS[choice][1]:
        ATTACKS[choice][1]()
    else:
        print("Invalid choice.")

    print("\nDone. Check the dashboard for logged failures.")
