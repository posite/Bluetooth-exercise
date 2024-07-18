package com.posite.bluetoothex.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

class BluetoothLE(private val context: Context) {
    private val bluetooth = context.getSystemService(Context.BLUETOOTH_SERVICE)
            as? BluetoothManager
        ?: throw Exception("Bluetooth is not supported by this device")
    private val SCAN_PERIOD: Long = 10000
    private val bluetoothLeScanner: BluetoothLeScanner = bluetooth.adapter.bluetoothLeScanner
    private var scanning = false
    private val handler = Handler(Looper.getMainLooper())
    private var bluetoothGatt: BluetoothGatt? = null
    val foundDevices = MutableStateFlow<Set<BluetoothDevice>>(emptySet())
    private var services: List<BluetoothGattService> = emptyList()


    // Device scan callback.
    private val leScanCallback: ScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            result.device?.let {
                if (!foundDevices.value.contains(result.device)) {
                    if (result.device.name != null && result.device.name.contains("Buds Pro")) {
                        foundDevices.update { devices -> devices + result.device }
                        Log.d(BluetoothLeService.BLETAG, "Device found: ${it.name}")
                        connect(result.device.address)
                    }

                    //Log.d(BluetoothLeService.BLETAG, "Found devices: ${foundDevices.value}")
                }
            }
        }
    }

    fun getAdapter() = bluetooth.adapter

    @SuppressLint("MissingPermission")
    fun scanLeDevice() {
        if (!scanning) { // Stops scanning after a pre-defined scan period.
            handler.postDelayed({
                scanning = false
                bluetoothLeScanner.stopScan(leScanCallback)
            }, SCAN_PERIOD)
            scanning = true
            bluetoothLeScanner.startScan(leScanCallback)
            //Log.d("scanResult", "Found devices: ${foundDevices.value}")

        } else {
            scanning = false
            bluetoothLeScanner.stopScan(leScanCallback)
            //Log.d("scanResult", "Found devices: ${foundDevices.value}")
        }
    }

    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            Log.d("bluetoothle service", "status: $status,   state: $newState")
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("bluetoothle service", "Connected to GATT server.")
                bluetoothGatt?.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("bluetoothle service", "Disconnected from GATT server.")

            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            Log.d("bluetoothle service", "device: ${gatt.device.name}")
            services = gatt.services
            Log.d("bluetoothle service", "Services discovered: $services")
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)
            Log.v("bluetooth", String(characteristic.value))
        }

    }

    @SuppressLint("MissingPermission")
    fun connect(address: String): Boolean {
        bluetooth.adapter.let { adapter ->
            try {
                val device = adapter.getRemoteDevice(address)
                // connect to the GATT server on the device
                bluetoothGatt = device.connectGatt(context, false, bluetoothGattCallback)
                return true
            } catch (exception: IllegalArgumentException) {
                Log.w(
                    BluetoothLeService.BLETAG,
                    "Device not found with provided address.  Unable to connect."
                )
                return false
            }
        }
    }


    @SuppressLint("MissingPermission")
    fun readCharacteristic(serviceUUID: UUID, characteristicUUID: UUID) {
        val service = bluetoothGatt?.getService(serviceUUID)
        val characteristic = service?.getCharacteristic(characteristicUUID)

        if (characteristic != null) {
            val success = bluetoothGatt?.readCharacteristic(characteristic)
            Log.v("bluetooth", "Read status: $success")
        }
    }

}