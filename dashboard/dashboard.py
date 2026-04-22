import streamlit as st
import pandas as pd
import plotly.express as px
from streamlit_autorefresh import st_autorefresh
import firebase_admin
from firebase_admin import credentials, firestore
from datetime import datetime, timezone, timedelta

st.set_page_config(page_title="GoKey Dashboard", layout="wide")

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
    if hasattr(t, "seconds"):
        return datetime.fromtimestamp(t.seconds, tz=timezone.utc)
    return None


# data

users    = fetch_collection("users")
keys     = fetch_collection("digitalKeys")
vehicles = fetch_collection("vehicles")
events   = fetch_collection("vehicleEvents")

active_keys  = [k for k in keys if k.get("status") == "ACTIVE"]
revoked_keys = [k for k in keys if k.get("status") == "REVOKED"]

successes      = [e for e in events if e.get("result") == "SUCCESS"]
failures       = [e for e in events if e.get("result") == "FAILURE"]
vacu_failures  = [e for e in failures if e.get("source") == "VACU"]

now = datetime.now(tz=timezone.utc)
recent_failures = [
    e for e in failures
    if parse_timestamp(e.get("timestamp")) and
       (now - parse_timestamp(e.get("timestamp"))) < timedelta(hours=1)
]

# header

st.title("GoKey Fleet Dashboard")
st.caption(f"Last updated: {datetime.now().strftime('%H:%M:%S')}")

# security alert banner

if recent_failures:
    st.error(f"⚠ {len(recent_failures)} failed access attempt(s) in the last hour")

# stat cards — row 1: fleet

st.subheader("Fleet")
c1, c2, c3, c4 = st.columns(4)
c1.metric("Users",        len(users))
c2.metric("Vehicles",     len(vehicles))
c3.metric("Active Keys",  len(active_keys))
c4.metric("Revoked Keys", len(revoked_keys))

# stat cards — row 2: events

st.subheader("Access Activity")
e1, e2, e3, e4 = st.columns(4)
e1.metric("Total Events",    len(events))
e2.metric("Successful",      len(successes))
e3.metric("Failed",          len(failures),       delta=f"-{len(recent_failures)} last hour" if recent_failures else None, delta_color="inverse")
e4.metric("VACU Rejections", len(vacu_failures))

st.divider()

if events:
    df = pd.DataFrame(events)

    df["datetime"] = pd.to_datetime(df["timestamp"].apply(parse_timestamp), utc=True, errors="coerce")
    df["date"]     = df["datetime"].dt.strftime("%Y-%m-%d")
    df = df.dropna(subset=["date"])

    col1, col2 = st.columns(2)

    with col1:
        st.subheader("Events per Day")
        daily = df.groupby(["date", "result"]).size().reset_index(name="count")
        daily = daily[daily["date"].isin(sorted(df["date"].unique())[-14:])]
        fig = px.bar(
            daily, x="date", y="count", color="result",
            color_discrete_map={"SUCCESS": "#4CAF50", "FAILURE": "#F44336"},
            labels={"date": "Date", "count": "Events", "result": "Result"}
        )
        fig.update_layout(margin=dict(t=20, b=20))
        st.plotly_chart(fig, use_container_width=True)

    with col2:
        st.subheader("Lock vs Unlock")
        action_counts = df["action"].value_counts().reset_index()
        action_counts.columns = ["action", "count"]
        fig2 = px.pie(action_counts, names="action", values="count", hole=0.4)
        fig2.update_layout(margin=dict(t=20, b=20))
        st.plotly_chart(fig2, use_container_width=True)

    col3, col4 = st.columns(2)

    with col3:
        st.subheader("Activity by Vehicle")
        if "vehicleId" in df.columns:
            vehicle_counts = df.groupby(["vehicleId", "result"]).size().reset_index(name="count")
            fig3 = px.bar(
                vehicle_counts, x="vehicleId", y="count", color="result",
                color_discrete_map={"SUCCESS": "#4CAF50", "FAILURE": "#F44336"},
                labels={"vehicleId": "Vehicle", "count": "Events", "result": "Result"}
            )
            fig3.update_layout(margin=dict(t=20, b=20))
            st.plotly_chart(fig3, use_container_width=True)

    with col4:
        st.subheader("Event Source")
        df["source"] = df.get("source", pd.Series(["App"] * len(df))).fillna("App")
        source_counts = df["source"].value_counts().reset_index()
        source_counts.columns = ["source", "count"]
        fig4 = px.pie(source_counts, names="source", values="count", hole=0.4)
        fig4.update_layout(margin=dict(t=20, b=20))
        st.plotly_chart(fig4, use_container_width=True)

else:
    st.info("No events logged yet.")

st.divider()

# security monitor

st.subheader("Security Monitor")

if failures:
    df_fail = pd.DataFrame(failures)
    df_fail["datetime"] = pd.to_datetime(df_fail["timestamp"].apply(parse_timestamp), utc=True, errors="coerce")
    df_fail["time_str"] = df_fail["datetime"].dt.strftime("%H:%M:%S").fillna("Unknown")
    df_fail["failureReason"] = df_fail.get("failureReason", pd.Series(["unknown"] * len(df_fail))).fillna("unknown")

    reason_counts = df_fail["failureReason"].value_counts().reset_index()
    reason_counts.columns = ["Reason", "Count"]

    sec1, sec2 = st.columns([1, 2])

    with sec1:
        st.markdown("**Rejection Reasons**")
        st.dataframe(reason_counts, use_container_width=True, hide_index=True)

    with sec2:
        st.markdown("**Failed Attempts Timeline**")
        df_fail_ts = df_fail.dropna(subset=["datetime"])
        if not df_fail_ts.empty:
            df_fail_ts["hour"] = df_fail_ts["datetime"].dt.strftime("%Y-%m-%d %H:00")
            df_fail_ts = df_fail_ts.dropna(subset=["hour"])
            hourly = df_fail_ts.groupby(["hour", "failureReason"]).size().reset_index(name="count")
            fig_sec = px.bar(
                hourly, x="hour", y="count", color="failureReason",
                labels={"hour": "Time", "count": "Failures", "failureReason": "Reason"}
            )
            fig_sec.update_layout(margin=dict(t=20, b=20))
            st.plotly_chart(fig_sec, use_container_width=True)
else:
    st.success("No failed access attempts recorded.")

st.divider()

# recent events table

st.subheader("Recent Events")

if events:
    df_display = pd.DataFrame(events)
    cols = ["timestamp", "userId", "vehicleId", "action", "result", "source"]
    for c in cols:
        if c not in df_display.columns:
            df_display[c] = "—"

    df_display["timestamp"] = pd.to_datetime(
        df_display["timestamp"].apply(parse_timestamp), utc=True, errors="coerce"
    ).dt.strftime("%Y-%m-%d %H:%M:%S").fillna("Unknown")
    df_display["source"] = df_display["source"].fillna("App")
    df_display = df_display[cols].sort_values("timestamp", ascending=False).head(20)
    df_display.columns = ["Time", "User ID", "Vehicle ID", "Action", "Result", "Source"]

    def highlight_failures(row):
        if row["Result"] == "FAILURE":
            return ["background-color: #3d1a1a"] * len(row)
        return [""] * len(row)

    st.dataframe(
        df_display.style.apply(highlight_failures, axis=1),
        use_container_width=True,
        hide_index=True
    )
else:
    st.info("No events to display.")
