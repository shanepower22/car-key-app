import streamlit as st
import pandas as pd
import plotly.express as px
from streamlit_autorefresh import st_autorefresh
import firebase_admin
from firebase_admin import credentials, firestore
from datetime import datetime, timezone

st.set_page_config(page_title="CarKey Dashboard", layout="wide")

# refresh every 10 seconds
st_autorefresh(interval=10_000, key="dashboard_refresh")


@st.cache_resource
def init_firebase():
    if not firebase_admin._apps:
        cred = credentials.Certificate("serviceAccountKey.json")
        firebase_admin.initialize_app(cred)
    return firestore.client()


db = init_firebase()


def fetch_collection(collection: str) -> list[dict]:
    docs = db.collection(collection).get()
    return [doc.to_dict() for doc in docs]


def parse_timestamp(t) -> datetime | None:
    if t is None:
        return None
    if isinstance(t, datetime):
        return t
    # Firestore Admin SDK returns DatetimeWithNanoseconds
    if hasattr(t, "seconds"):
        return datetime.fromtimestamp(t.seconds, tz=timezone.utc)
    return None


#  data 

users   = fetch_collection("users")
keys    = fetch_collection("digitalKeys")
vehicles = fetch_collection("vehicles")
events  = fetch_collection("vehicleEvents")

active_keys   = [k for k in keys   if k.get("status") == "ACTIVE"]
revoked_keys  = [k for k in keys   if k.get("status") == "REVOKED"]

# header 

st.title("CarKey Fleet Dashboard")
st.caption(f"Last updated: {datetime.now().strftime('%H:%M:%S')}")

# stat cards
c1, c2, c3, c4 = st.columns(4)
c1.metric("Users",        len(users))
c2.metric("Vehicles",     len(vehicles))
c3.metric("Active Keys",  len(active_keys))
c4.metric("Revoked Keys", len(revoked_keys))

st.divider()

# events chart 

if events:
    df = pd.DataFrame(events)

    df["time"] = df["timestamp"].apply(
        lambda t: parse_timestamp(t).strftime("%Y-%m-%d") if parse_timestamp(t) else None
    )
    df = df.dropna(subset=["time"])

    col_left, col_right = st.columns(2)

    with col_left:
        st.subheader("Events per Day")
        daily = df.groupby("time").size().reset_index(name="count")
        daily = daily.sort_values("time").tail(14)
        fig = px.bar(daily, x="time", y="count", labels={"time": "Date", "count": "Events"})
        fig.update_layout(margin=dict(t=20, b=20))
        st.plotly_chart(fig, use_container_width=True)

    with col_right:
        st.subheader("Lock vs Unlock")
        action_counts = df["action"].value_counts().reset_index()
        action_counts.columns = ["action", "count"]
        fig2 = px.pie(action_counts, names="action", values="count", hole=0.4)
        fig2.update_layout(margin=dict(t=20, b=20))
        st.plotly_chart(fig2, use_container_width=True)

else:
    st.info("No events logged yet.")

st.divider()

#  recent events table 

st.subheader("Recent Events")

if events:
    df_display = pd.DataFrame(events)[["timestamp", "userId", "vehicleId", "action", "result"]]
    df_display["timestamp"] = df_display["timestamp"].apply(
        lambda t: parse_timestamp(t).strftime("%Y-%m-%d %H:%M:%S") if parse_timestamp(t) else "Unknown"
    )
    df_display = df_display.sort_values("timestamp", ascending=False).head(20)
    df_display.columns = ["Time", "User ID", "Vehicle ID", "Action", "Result"]
    st.dataframe(df_display, use_container_width=True, hide_index=True)
else:
    st.info("No events to display.")
