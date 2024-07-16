package com.posite.bluetoothex

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.BLUETOOTH
import android.Manifest.permission.BLUETOOTH_ADMIN
import android.Manifest.permission.BLUETOOTH_CONNECT
import android.Manifest.permission.BLUETOOTH_SCAN
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.posite.bluetoothex.ui.theme.BluetoothExTheme

class MainActivity : ComponentActivity() {
    private val bluetooth by lazy {
        this@MainActivity.getSystemService(Context.BLUETOOTH_SERVICE)
                as? BluetoothManager
            ?: throw Exception("Bluetooth is not supported by this device")
    }

    private val scanner: BluetoothLeScanner
        get() = bluetooth.adapter.bluetoothLeScanner

    private var selectedDevice: BluetoothDevice? = null
    private var gatt: BluetoothGatt? = null
    private var services: List<BluetoothGattService> = emptyList()

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.let {
                if (it.device.name == "Buds Pro") {
                    Log.d("Bluetooth_", it.device.name)
                    selectedDevice = it.device
                    scanner.stopScan(this)
                    connect()
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
        }
    }

    private val callback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (status != BluetoothGatt.GATT_SUCCESS) {
                return
            }
            Log.d("Bluetooth_", "Connected to device")
            Toast.makeText(this@MainActivity, "Connected to device", Toast.LENGTH_SHORT).show()
            if (newState == BluetoothGatt.STATE_CONNECTING || newState == BluetoothGatt.STATE_DISCONNECTED || newState == BluetoothGatt.GATT_SUCCESS) {
                Log.d("Bluetooth_", "Connected to device")
                Toast.makeText(this@MainActivity, "Connected to device", Toast.LENGTH_SHORT).show()
                discoverServices()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            services = gatt.services
            Log.d("Bluetooth_", "Discovered services: ${services.size}")
            Toast.makeText(
                this@MainActivity,
                "Discovered services: ${services.size}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BluetoothExTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (!haveAllPermissions(this@MainActivity)) {
                            GrantPermissionsButton {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Bluetooth Permissions granted",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            startScanning()
                        }

                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startScanning() {
        scanner.startScan(scanCallback)
    }

    @RequiresPermission(BLUETOOTH_CONNECT)
    fun connect() {
        gatt = selectedDevice!!.connectGatt(this@MainActivity, true, callback)
    }

    @RequiresPermission(BLUETOOTH_CONNECT)
    fun discoverServices() {
        gatt!!.discoverServices()
    }
}


private val ALL_BLE_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    arrayOf(
        BLUETOOTH_CONNECT,
        BLUETOOTH_SCAN
    )
} else {
    arrayOf(
        BLUETOOTH_ADMIN,
        BLUETOOTH,
        ACCESS_FINE_LOCATION
    )
}

private fun haveAllPermissions(context: Context) =
    ALL_BLE_PERMISSIONS
        .all { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }


@Composable
fun GrantPermissionsButton(onPermissionGranted: () -> Unit) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.all { it }) {
            // User has granted all permissions
            onPermissionGranted()
        } else {
            // TODO: handle potential rejection in the usual way
        }
    }

    // User presses this button to request permissions
    Button(
        onClick = { launcher.launch(ALL_BLE_PERMISSIONS) }) {
        Text("Grant Permission")
    }
}
