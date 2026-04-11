package ie.setu.carkey.integration

import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ie.setu.carkey.service.BleManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for BleManager — V-Model Phase 6 (Integration Test)
 *
 * Verifies that BleManager initialises correctly against real Android
 * Bluetooth system services on a physical device or emulator.
 *
 * NOTE: Full GATT advertising/connection tests require a physical device
 * with Bluetooth enabled and a paired VACU. These tests cover the
 * initialisation and state-machine boundaries that can be verified
 * without a second device.
 */
@RunWith(AndroidJUnit4::class)
class BleIntegrationTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        BleManager.initialize(context)
    }

    @Test
    fun useAppContext() {
        assertEquals("ie.setu.carkey", context.packageName)
    }

    @Test
    fun bleManagerInitialises_bluetoothSystemServiceAvailable() {
        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        assertNotNull("BluetoothManager system service must be available", btManager)
    }

    @Test
    fun isConnected_initialStateIsFalse() {
        // Before any VACU connects the flow must report disconnected
        assertFalse(BleManager.isConnected.value)
    }

    @Test
    fun bluetoothAdapter_isNotNull() {
        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        assertNotNull("BluetoothAdapter must not be null on a BLE-capable device", btManager.adapter)
    }
}
