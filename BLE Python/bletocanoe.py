import asyncio
import time
import os
import threading
import queue

import win32com.client
import win32com.client.connect

from bleak import BleakClient, BleakScanner
from payload_parser import validate_payload, parse_payload
from cloud import get_active_key, log_event


#  

# =========================
# CANoe COM helpers
# =========================

def DoEvents():
    win32com.client.connect.pythoncom.PumpWaitingMessages()
    time.sleep(0.05)


def DoEventsUntil(cond):
    while not cond():
        DoEvents()


class CanoeMeasurementEvents(object):
    def OnStart(self):
        CanoeSync.Started = True
        CanoeSync.Stopped = False
        print("CANoe measurement started")

    def OnStop(self):
        CanoeSync.Started = False
        CanoeSync.Stopped = True
        print("CANoe measurement stopped")


class CanoeSync(object):
    Started = False
    Stopped = False

    def __init__(self):
        self.App = win32com.client.DispatchEx("CANoe.Application")
        self.Meas = self.App.Measurement
        win32com.client.WithEvents(self.Meas, CanoeMeasurementEvents)

    def Load(self, cfgPath):
        cfg = os.path.abspath(cfgPath)
        print("Loading CANoe config:", cfg)
        self.App.Open(cfg)

    def Start(self):
        if not self.Meas.Running:
            self.Meas.Start()
            DoEventsUntil(lambda: CanoeSync.Started)

    def Stop(self):
        if self.Meas.Running:
            self.Meas.Stop()
            DoEventsUntil(lambda: CanoeSync.Stopped)

    def SetSysvarValue(self, namespace, name, value):
        self.App.System.Namespaces.Item(namespace).Variables.Item(name).Value = value
        print(f"CANoe sysvar {namespace}.{name} set to {value}")


# =========================
# BLE configuration
# =========================

BLE_DEVICE_NAME = "CarKeyPhone"  
BLE_CHARACTERISTIC_UUID = "12345678-1234-5678-1234-56789abcdef1"

CANOE_CFG_PATH = r"ConfigWithArrays.cfg"

canoe = CanoeSync()

# Queue for sysvar updates to be processed on the main thread 
sysvar_queue = queue.Queue()


# =========================
# BLE logic 
# =========================

async def handle_ble():
    while True:  # Outer loop for reconnection
        print("Scanning for BLE device...")

        target = None

        while not target:
            print("Scanning for BLE device...")
            devices = await BleakScanner.discover(timeout=5)

            for d in devices:
                print(f"Found device: {d.name} [{d.address}]")
                if d.name == BLE_DEVICE_NAME or d.name == "CarKey":
                    target = d
                    break

            if not target:
                print("Device not found, retrying...\n")
                await asyncio.sleep(2)

        print("Connecting to BLE device:", target.name)

        try:
            async with BleakClient(target.address) as client:
                print("BLE connected")

                def notification_handler(_, data: bytearray):
                    try:
                        raw = data.decode().strip()
                        print("BLE payload received:", raw)

                        # step 1: validate payload format and timestamp
                        cmd, error = validate_payload(raw)
                        if error:
                            print(f"Payload rejected: {error}")
                            parsed = parse_payload(raw)
                            nonce = parsed["nonce"] if parsed else "unknown"
                            log_event(user_id="unknown", action="UNKNOWN", result="FAILURE", nonce=nonce)
                            return

                        parsed = parse_payload(raw)
                        nonce = parsed["nonce"] if parsed else "unknown"

                        # step 2: validate key is still active in Firestore
                        active_key = get_active_key()
                        if active_key is None:
                            print("Command rejected: no active key found for this vehicle")
                            log_event(user_id="unknown", action=cmd, result="FAILURE", nonce=nonce)
                            return

                        user_id = active_key.get("userId", "unknown")

                        # step 3: forward to CANoe and log success
                        if cmd == "unlock":
                            sysvar_queue.put(("Vehicle", "DoorLock", 0))
                        elif cmd == "lock":
                            sysvar_queue.put(("Vehicle", "DoorLock", 1))

                        log_event(user_id=user_id, action=cmd, result="SUCCESS", nonce=nonce)

                    except Exception as e:
                        print(f"Error handling BLE data: {e}")

                await client.start_notify(BLE_CHARACTERISTIC_UUID, notification_handler)

                print("Listening for BLE lock/unlock commands...")
                while True:
                    await asyncio.sleep(1)
        except Exception as e:
            print(f"BLE connection error: {e}")
            print("Reconnecting in 3 seconds...\n")
            await asyncio.sleep(3)
            target = None  # Force re-scan


def start_ble_thread():
    loop = asyncio.new_event_loop()
    asyncio.set_event_loop(loop)
    loop.run_until_complete(handle_ble())


# Created from sample code from the official Vector website: https://support.vector.com/kb?sys_kb_id=3d1cc76c87f08d50a0460fe40cbb358e&id=kb_article_view&sysparm_rank=9&sysparm_tsqueryId=fd608bbb3b2532d84fcd6c3eb3e45a40


# =========================
# Main
# =========================

if __name__ == "__main__":

    # Start CANoe
    canoe.Load(CANOE_CFG_PATH)
    canoe.Start()

    # Start BLE in background thread
    ble_thread = threading.Thread(
        target=start_ble_thread,
        daemon=True
    )
    ble_thread.start()

   

    print("BLE thread running")

    try:
        while True:
            # Process any pending sysvar updates on the main thread 
            try:
                while not sysvar_queue.empty():
                    namespace, name, value = sysvar_queue.get_nowait()
                    try:
                        canoe.SetSysvarValue(namespace, name, value)
                    except Exception as ex:
                        print(f"Failed to set sysvar {namespace}.{name}:", ex)
            except Exception:
                pass

            DoEvents()   # keep CANoe COM responsive
    except KeyboardInterrupt:
        print("Stopping system...")
    finally:
        canoe.Stop()
