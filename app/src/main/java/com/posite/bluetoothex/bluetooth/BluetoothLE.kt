package com.posite.bluetoothex.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class BluetoothLE(private val context: Context) {
    private val bluetooth = context.getSystemService(Context.BLUETOOTH_SERVICE)
            as? BluetoothManager
        ?: throw Exception("Bluetooth is not supported by this device")
    private val SCAN_PERIOD: Long = 10000
    private val bluetoothLeScanner: BluetoothLeScanner = bluetooth.adapter.bluetoothLeScanner
    private var scanning = false
    private val handler = Handler(Looper.getMainLooper())
    val foundDevices = MutableStateFlow<Set<BluetoothDevice>>(emptySet())

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
                    }

                    //Log.d(BluetoothLeService.BLETAG, "Found devices: ${foundDevices.value}")
                }
            }
        }
    }

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

}