package ie.setu.carkey.service

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.ParcelUuid
import androidx.annotation.RequiresPermission
import ie.setu.carkey.security.SecurityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.UUID

// App acts as BLE peripheral, VACU connects as central
object BleManager {

    private val SERVICE_UUID    = UUID.fromString("12345678-1234-5678-1234-56789abcdef0")
    private val CHAR_UUID       = UUID.fromString("12345678-1234-5678-1234-56789abcdef1")
    private val DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var gattServer: BluetoothGattServer? = null
    private var characteristic: BluetoothGattCharacteristic? = null
    private var connectedDevice: BluetoothDevice? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    fun initialize(context: Context) {
        bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        Timber.i("BleManager initialised")
    }

    @RequiresPermission(allOf = [
        android.Manifest.permission.BLUETOOTH_CONNECT,
        android.Manifest.permission.BLUETOOTH_ADVERTISE
    ])
    fun start(context: Context) {
        setupGattServer(context)
        startAdvertising()
    }

    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    private fun setupGattServer(context: Context) {
        gattServer = bluetoothManager.openGattServer(context, gattServerCallback)

        characteristic = BluetoothGattCharacteristic(
            CHAR_UUID,
            BluetoothGattCharacteristic.PROPERTY_NOTIFY or
                    BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        ).apply {
            addDescriptor(
                BluetoothGattDescriptor(
                    DESCRIPTOR_UUID,
                    BluetoothGattDescriptor.PERMISSION_READ or
                            BluetoothGattDescriptor.PERMISSION_WRITE
                )
            )
        }

        val service = BluetoothGattService(
            SERVICE_UUID,
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        ).apply { addCharacteristic(characteristic) }

        gattServer?.addService(service)
        Timber.i("GATT server ready — service UUID: $SERVICE_UUID")
    }

    @RequiresPermission(android.Manifest.permission.BLUETOOTH_ADVERTISE)
    fun startAdvertising() {
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .setTimeout(0)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .addServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()

        bluetoothAdapter.bluetoothLeAdvertiser
            ?.startAdvertising(settings, data, advertiseCallback)
            ?: Timber.e("BLE advertising not supported on this device")
    }

    @RequiresPermission(android.Manifest.permission.BLUETOOTH_ADVERTISE)
    fun stopAdvertising() {
        bluetoothAdapter.bluetoothLeAdvertiser
            ?.stopAdvertising(advertiseCallback)
        Timber.i("BLE advertising stopped")
    }

    // payload format: {command}:{nonce}:{timestamp}
    @SuppressLint("MissingPermission") // permission checked before start() is called
    fun sendCommand(command: String) {
        val device = connectedDevice ?: run {
            Timber.w("sendCommand: no device connected")
            return
        }
        val char = characteristic ?: run {
            Timber.w("sendCommand: GATT not initialised")
            return
        }
        val payload = SecurityManager.buildPayload(command)
        char.value = payload.toByteArray(Charsets.UTF_8)
        gattServer?.notifyCharacteristicChanged(device, char, false)
        Timber.i("Command sent -> $payload")
    }

    private val gattServerCallback = object : BluetoothGattServerCallback() {

        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(
            device: BluetoothDevice, status: Int, newState: Int
        ) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectedDevice = device
                    _isConnected.value = true
                    Timber.i("VACU connected: ${device.address}")
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    if (connectedDevice?.address == device.address) {
                        connectedDevice = null
                        _isConnected.value = false
                    }
                    Timber.i("VACU disconnected: ${device.address}")
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onDescriptorWriteRequest(
            device: BluetoothDevice, requestId: Int,
            descriptor: BluetoothGattDescriptor,
            preparedWrite: Boolean, responseNeeded: Boolean,
            offset: Int, value: ByteArray?
        ) {
            if (descriptor.uuid == DESCRIPTOR_UUID) {
                descriptor.value = value
                if (responseNeeded) {
                    gattServer?.sendResponse(
                        device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null
                    )
                }
                Timber.i("Notification subscription updated for ${device.address}")
            }
        }
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Timber.i("BLE advertising started — device name: ${bluetoothAdapter.name}")
        }
        override fun onStartFailure(errorCode: Int) {
            Timber.e("BLE advertising failed — error code: $errorCode")
        }
    }
}
