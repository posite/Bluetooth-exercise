package com.posite.bluetoothex.bluetooth

import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log


class BluetoothLeService : Service() {
    private var bluetoothAdapter: BluetoothAdapter? = null
    private val binder = LocalBinder()
    private var bluetoothGatt: BluetoothGatt? = null
    private var connectionState = STATE_DISCONNECTED
    var deviceAddress = ""

    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            Log.d("bluetoothle service", "status: $status,   state: $newState")
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("bluetoothle service", "Connected to GATT server.")
                broadcastUpdate(ACTION_GATT_CONNECTED)
                connectionState = STATE_CONNECTED
                bluetoothGatt?.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("bluetoothle service", "Disconnected from GATT server.")
                broadcastUpdate(ACTION_GATT_DISCONNECTED)
                connectionState = STATE_DISCONNECTED
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
            } else {
                Log.w("bluetoothle service", "onServicesDiscovered received: $status")
            }
        }

    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    inner class LocalBinder : Binder() {
        fun getService(): BluetoothLeService {
            return this@BluetoothLeService
        }
    }

    fun getSupportedGattServices(): List<BluetoothGattService?>? {
        return bluetoothGatt?.services
    }

    fun initialize(ble: BluetoothLE): Boolean {
        bluetoothAdapter = ble.getAdapter()
        if (bluetoothAdapter == null) {
            Log.e(BLETAG, "Unable to obtain a BluetoothAdapter.")
            return false
        }
        return true
    }

    @SuppressLint("MissingPermission")
    fun connect(address: String): Boolean {
        bluetoothAdapter?.let { adapter ->
            try {
                val device = adapter.getRemoteDevice(address)
                deviceAddress = device.address
                // connect to the GATT server on the device
                bluetoothGatt = device.connectGatt(this, false, bluetoothGattCallback)
                return true
            } catch (exception: IllegalArgumentException) {
                Log.w(BLETAG, "Device not found with provided address.  Unable to connect.")
                return false
            }
        } ?: run {
            Log.w(BLETAG, "BluetoothAdapter not initialized")
            return false
        }
    }


    private fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        sendBroadcast(intent)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        close()
        return super.onUnbind(intent)
    }


    @SuppressLint("MissingPermission")
    private fun close() {
        bluetoothGatt?.let { gatt ->
            gatt.close()
            bluetoothGatt = null
        }
    }

    companion object {
        const val BLETAG = "BluetoothLeService"
        const val ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED"
        const val ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED"
        const val ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED"

        private const val STATE_DISCONNECTED = 0
        private const val STATE_CONNECTED = 2
    }
}