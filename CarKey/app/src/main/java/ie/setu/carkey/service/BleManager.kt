package ie.setu.carkey.service

import timber.log.Timber

// Service UUID: 12345678-1234-5678-1234-56789abcdef0
// Characteristic UUID: 12345678-1234-5678-1234-56789abcdef1
object BleManager {

    var isConnected: Boolean = false
        private set

    fun sendCommand(command: String) {
        // TODO Phase 3: sign command with nonce + timestamp, send via BLE GATT notify
        Timber.i("BleManager.sendCommand($command) — not yet implemented")
    }

    fun startAdvertising() {
        // TODO Phase 3: start BLE LE advertising as peripheral
        Timber.i("BleManager.startAdvertising() — not yet implemented")
    }

    fun stopAdvertising() {
        Timber.i("BleManager.stopAdvertising() — not yet implemented")
    }
}
