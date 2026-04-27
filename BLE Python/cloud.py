import uuid
import time
import firebase_admin
from firebase_admin import credentials, firestore

# path to service account key - same one used by the dashboard
SERVICE_ACCOUNT_PATH = "../dashboard/serviceAccountKey.json"

# vehicle this VACU instance is associated with
VEHICLE_ID = "vehicle_1"


def init_firebase():
    if not firebase_admin._apps:
        cred = credentials.Certificate(SERVICE_ACCOUNT_PATH)
        firebase_admin.initialize_app(cred)
    return firestore.client()


db = init_firebase()


_key_cache: dict = {"key": None, "fetched_at": 0.0}
KEY_CACHE_TTL = 10  # seconds — revocation takes effect within this window

_public_key_cache: dict = {}  # user_id -> (public_key, fetched_at)
PUBLIC_KEY_CACHE_TTL = 30


def get_user_public_key(user_id: str):
    """
    Returns the registered ECDSA public key (base64 X.509 SPKI) for a user, or None.
    Cached for PUBLIC_KEY_CACHE_TTL seconds.
    """
    cached = _public_key_cache.get(user_id)
    if cached and time.time() - cached[1] < PUBLIC_KEY_CACHE_TTL:
        return cached[0]

    doc = db.collection("users").document(user_id).get()
    if not doc.exists:
        return None
    public_key = doc.to_dict().get("publicKey")
    if not public_key:
        return None
    _public_key_cache[user_id] = (public_key, time.time())
    return public_key


def get_active_key(vehicle_id: str = VEHICLE_ID):
    """
    Returns the active DigitalKey document for this vehicle, or None.
    Cached for KEY_CACHE_TTL seconds to avoid Firestore quota exhaustion.
    """
    from google.cloud.firestore_v1.base_query import FieldFilter

    if time.time() - _key_cache["fetched_at"] < KEY_CACHE_TTL:
        return _key_cache["key"]

    docs = (
        db.collection("digitalKeys")
        .where(filter=FieldFilter("vehicleId", "==", vehicle_id))
        .where(filter=FieldFilter("status", "==", "ACTIVE"))
        .limit(1)
        .get()
    )
    result = docs[0].to_dict() if docs else None
    _key_cache["key"] = result
    _key_cache["fetched_at"] = time.time()
    return result


def log_event(
    user_id: str,
    action: str,
    result: str,
    nonce: str,
    vehicle_id: str = VEHICLE_ID,
    failure_reason: str = None
):
    event = {
        "logId":     str(uuid.uuid4()),
        "timestamp": firestore.SERVER_TIMESTAMP,
        "userId":    user_id,
        "vehicleId": vehicle_id,
        "action":    action.upper(),
        "result":    result.upper(),
        "nonce":     nonce,
        "source":    "VACU"
    }
    if failure_reason:
        event["failureReason"] = failure_reason
    db.collection("vehicleEvents").add(event)
    print(f"Event logged: {action.upper()} {result.upper()} for vehicle {vehicle_id}"
          + (f" — {failure_reason}" if failure_reason else ""))
