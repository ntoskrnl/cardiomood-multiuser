package com.cardiomood.multiuser.screen.monitoring

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.cardiomood.multiuser.R
import com.cardiomood.multiuser.mvp.BaseActivity
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.KodeinInjector
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import com.github.salomonbrys.kodein.singleton
import com.tbruyelle.rxpermissions.RxPermissions
import ru.test.multydevicetest.DeviceService

class GroupMonitoringActivity : BaseActivity() {

    companion object {
        const val REQUEST_ENABLE_BT = 42
    }

    private val injector = KodeinInjector()
    private val presenter by injector.instance<GroupMonitoringPresenter>()
    private val view by injector.instance<GroupMonitoringView>()

    private lateinit var bluetoothAdapter: BluetoothAdapter
    var bluetoothLeService: DeviceService? = null

    private val scanHandler = Handler()
    private var isScanning = false

    // Code to manage Service lifecycle.
    private val serviceConnection = object : ServiceConnection {

        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
//            Log.d(TAG, "connected to service")
            bluetoothLeService = (service as DeviceService.LocalBinder).service


//            mLeDeviceListAdapter.clear()
//
//            for (address in mBluetoothLeService.getAllAddresses()) {
//                mLeDeviceListAdapter.addDevice(address)
//            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
//            Log.d(TAG, "disconnected from service")
            bluetoothLeService = null
        }
    }

    private val leScanCallback = BluetoothAdapter.LeScanCallback { device, rssi, scanRecord ->
        runOnUiThread {
            bluetoothLeService?.addDevice(device)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_monitoring)

        injector.inject(diConfig())

        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE is not supported", Toast.LENGTH_SHORT).show()
            finish()
        }

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        if (bluetoothManager.adapter != null) {
            bluetoothAdapter = bluetoothManager.adapter
        } else {
            Toast.makeText(this, "BLUETOOTH is not supported", Toast.LENGTH_SHORT).show()
            finish()
        }

        if (Build.VERSION.SDK_INT >= 23) {
            RxPermissions.getInstance(applicationContext)
                    .request(Manifest.permission.ACCESS_COARSE_LOCATION)
                    .filter { it == false }
                    .subscribe { granted ->
                        // todo: RxLifecycle
                        finish()
                    }
        }

        val gattServiceIntent = Intent(this, DeviceService::class.java)
        bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        doScan(true)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.group_monitoring, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.stop -> {
                view.clearPairingRequests.call(Unit)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        presenter.attachView(view)

        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            Toast.makeText(this, "BLUETOOTH must be turned on", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onPause() {
        presenter.detachView()

        super.onPause()
    }

    override fun onDestroy() {
        unbindService(serviceConnection)
        bluetoothLeService = null
        super.onDestroy()
    }

    fun doScan(enable: Boolean) {
        if (enable) {
            if (isScanning)
                return
            // Stops scanning after a pre-defined scan period.
            scanHandler.postDelayed(
                    {
                        isScanning = false
                        bluetoothAdapter.stopLeScan(leScanCallback)
                        runOnUiThread {
//                    if (scanMenu != null) {
//                        scanMenu.findItem(R.id.menu_stop).setVisible(false)
//                        scanMenu.findItem(R.id.menu_scan).setVisible(true)
//                    }
                        }
                    },
                    10000
            )

            isScanning = true
            bluetoothAdapter.startLeScan(leScanCallback)
        } else {
            isScanning = false
            bluetoothAdapter.stopLeScan(leScanCallback)
        }
        invalidateOptionsMenu()
    }


    override fun onBackPressed() {
        view.backPresses.call(Unit)
        if (false) {
            // avoiding warning
            super.onBackPressed()
        }
    }

    private fun diConfig() = Kodein {
        extend(appKodein())

        bind<GroupMonitoringPresenter>() with singleton {
            getOrCreatePresenter("group_monitoring") { GroupMonitoringPresenter(instance()) }
        }

        bind<GroupMonitoringView>() with singleton {
            GroupMonitoringViewImpl(window.decorView, this@GroupMonitoringActivity)
        }
    }
}