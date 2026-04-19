import uuid
import firebase_admin
from firebase_admin import credentials, firestore

# path to service account key - same one used by the dashboard
SERVICE_ACCOUNT_PATH = "../dashboard/serviceAccountKey.json"

# vehicle this VACU instance is associated with
VEHICLE_ID = "vehicle_001"


def init_firebase():
    if not firebase_admin._apps:
        cred = credentials.Certificate(SERVICE_ACCOUNT_PATH)
        firebase_admin.initialize_app(cred)
    return firestore.client()


db = init_firebase()


def get_active_key(vehicle_id: str = VEHICLE_ID):
    """
    Returns the active DigitalKey document for this vehicle, or None.
    Used to verify a key exists and is not revoked before acting on a command.
    """
    docs = (
        db.collection("digitalKeys")
        .where("vehicleId", "==", vehicle_id)
        .where("status", "==", "ACTIVE")
        .limit(1)
        .get()
    )
    return docs[0].to_dict() if docs else None


def log_event(user_id: str, action: str, result: str, nonce: str, vehicle_id: str = VEHICLE_ID):
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
    db.collection("vehicleEvents").add(event)
    print(f"Event logged: {action.upper()} {result.upper()} for vehicle {vehicle_id}")
