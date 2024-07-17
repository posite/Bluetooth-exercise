package com.posite.bluetoothex

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.BLUETOOTH
import android.Manifest.permission.BLUETOOTH_ADMIN
import android.Manifest.permission.BLUETOOTH_CONNECT
import android.Manifest.permission.BLUETOOTH_SCAN
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.posite.bluetoothex.bluetooth.BluetoothLE
import com.posite.bluetoothex.bluetooth.BluetoothLeService
import com.posite.bluetoothex.ui.theme.BluetoothExTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val ble by lazy { BluetoothLE(this) }
    private val devices = mutableListOf<BluetoothDevice>()
    private var bluetoothService: BluetoothLeService? = null

    // Code to manage Service lifecycle.
    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(
            componentName: ComponentName,
            service: IBinder
        ) {
            bluetoothService = (service as BluetoothLeService.LocalBinder).getService()
            Log.e("BLE service connect", bluetoothService.toString())
            bluetoothService?.let { bluetooth ->
                if (!bluetooth.initialize()) {
                    Log.e("BLE service connect", "Unable to initialize Bluetooth")
                    finish()
                }
                // perform device connection
                Log.d("BLE service connect", "Connecting to device")
                devices.forEach { device ->
                    bluetooth.connect(device.address)
                }
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            bluetoothService = null
        }
    }

    private fun observe() {
        CoroutineScope(Dispatchers.IO).launch {
            ble.foundDevices.collect {
                devices.addAll(it)
                Log.d("devices", devices.toString())
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        observe()
        val gattServiceIntent =
            Intent(this@MainActivity, BluetoothLeService::class.java)
        bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
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
                            ble.scanLeDevice()
                        }

                    }
                }
            }
        }
    }

    private val gattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothLeService.ACTION_GATT_CONNECTED -> {
                    Log.d("BLE", "Connected")
                }

                BluetoothLeService.ACTION_GATT_DISCONNECTED -> {
                    Log.d("BLE", "Disconnected")
                }

                BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED -> {
                    Log.d("BLE", "Services discovered")
                }
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onResume() {
        super.onResume()
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter(), RECEIVER_EXPORTED)
        if (bluetoothService != null) {
            val result = bluetoothService!!.connect(bluetoothService!!.deviceAddress)
            Log.d("", "Connect request result=$result")
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(gattUpdateReceiver)
    }

    private fun makeGattUpdateIntentFilter(): IntentFilter? {
        return IntentFilter().apply {
            addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
            addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
        }
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
