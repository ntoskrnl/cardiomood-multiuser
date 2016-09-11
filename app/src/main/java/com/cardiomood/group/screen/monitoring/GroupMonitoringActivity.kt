package com.cardiomood.group.screen.monitoring

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import com.cardiomood.group.R
import com.cardiomood.group.mvp.BaseActivity
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.KodeinInjector
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import com.github.salomonbrys.kodein.singleton
import com.tbruyelle.rxpermissions.RxPermissions

class GroupMonitoringActivity : BaseActivity() {

    companion object {
        const val REQUEST_ENABLE_BT = 42
    }

    private val injector = KodeinInjector()
    private val presenter by injector.instance<GroupMonitoringPresenter>()
    private val view by injector.instance<GroupMonitoringView>()

    private lateinit var bluetoothAdapter: BluetoothAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_monitoring)

        injector.inject(diConfig())

        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE is not supported", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
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
    }

    override fun onResume() {
        super.onResume()

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }

        presenter.attachView(view)
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