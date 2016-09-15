package ru.test.multydevicetest

import android.app.ActivityManager
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import com.cardiomood.multiuser.R
import com.cardiomood.multiuser.api.Api
import com.cardiomood.multiuser.api.User
import com.cardiomood.multiuser.screen.monitoring.GroupMonitoringActivity
import com.github.salomonbrys.kodein.KodeinInjector
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import ru.test.multydevicetest.bluetooth.IDeviceEventListener
import ru.test.multydevicetest.bluetooth.SensorDevice
import ru.test.multydevicetest.devicemanagers.AbstractDeviceManager
import java.util.*

class DeviceService : Service(), IDeviceEventListener {


    protected var wakeLock: PowerManager.WakeLock? = null
    private var deviceManager: AbstractDeviceManager? = null
    private var dataCollector: DataCollector? = null
    private var longestLatency: Long = 0
    private var isStarted = false
    private var sharedPref: SharedPreferences? = null

    private val injector = KodeinInjector()
    private val api: Api by injector.instance()


    override fun onCreate() {
        injector.inject(appKodein())

        dataCollector = DataCollector(api)

        sharedPref = applicationContext.getSharedPreferences("prefs", Context.MODE_PRIVATE)

        val intentForeground = Intent(this, GroupMonitoringActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_RECEIVER_FOREGROUND)
        val pendIntent = PendingIntent.getActivity(this, 0, intentForeground, 0)
        val notification = Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setTicker("MultiUserTracker")
                .setContentTitle("CardioMood multi-user monitor")
                .setContentText("Touch to start overview")
                .setContentIntent(pendIntent)
                .setDefaults(Notification.DEFAULT_ALL)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setAutoCancel(false)
                .build()

        startForeground(ONGOING_NOTIFICATION_ID, notification)

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG)
        wakeLock!!.acquire()

        shouldRun = true
        AlarmReceiver.startAlarm(this)
        super.onCreate()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")

        if (shouldRun) {
            sharedPref!!.edit().putStringSet(ADDRESSES, deviceManager!!.allAddresses()).apply()
            doStop(false)
        } else {
            sharedPref!!.edit().putStringSet(ADDRESSES, HashSet<String>()).apply()
        }

        if (wakeLock != null && wakeLock!!.isHeld)
            wakeLock!!.release()
        super.onDestroy()
    }


    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStart")
        initialize()
        isStarted = true
        return Service.START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        Log.d(TAG, "onBind")
        if (!isStarted) {
            val startIntent = Intent(this, this.javaClass)
            startIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            startService(startIntent)
        }
        return mBinder
    }

    override fun onUnbind(intent: Intent): Boolean {
        Log.d(TAG, "onUnbind")
        return super.onUnbind(intent)
    }

    private val mBinder = LocalBinder()


    override fun onAction(address: String, action: String) {
        val intent = Intent(action)
        intent.putExtra(EXTRA_ADDRESS, address.toString())
        sendBroadcast(intent)

    }

    override fun onHeartRate(address: String, heartRate: Int, latency: Long) {
        //Intent intent = new Intent(SensorDevice.ACTION_DATA_AVAILABLE);
        //
        //intent.putExtra(EXTRA_ADDRESS, address);
        //intent.putExtra(EXTRA_HEART_RATE, String.valueOf(heartRate));
        //intent.putExtra(EXTRA_LATENCY, dutyCycle);
        //sendBroadcast(intent);

        doUpdateStats()
    }

    override fun onBatteryLevel(address: String, batteryLevel: Int) {
        val intent = Intent(SensorDevice.ACTION_BATTERY_LEVEL)

        intent.putExtra(EXTRA_ADDRESS, address)
        intent.putExtra(EXTRA_BATTERY_LEVEL, batteryLevel)
        sendBroadcast(intent)
    }

    @JvmOverloads fun doStop(isExpected: Boolean = true) {
        Log.d(TAG, "doStop, expected : " + isExpected)
        if (deviceManager != null) {
            deviceManager!!.doStop()
            deviceManager = null

        }

        if (isExpected) {
            shouldRun = false
            clearDevicePairing()
        }

        stopSelf()
    }

    val maxActiveDevices: Int
        get() {
            if (deviceManager == null) return 0
            return deviceManager!!.maxActiveDevices
        }

    fun maxActiveCountInc(): Int {
        if (deviceManager == null) return 0
        return deviceManager!!.maxActiveInc()
    }

    fun maxActiveCountDec(): Int {
        if (deviceManager == null) return 0
        return deviceManager!!.maxActiveDec()
    }

    fun doUpdateStats() {
        if (deviceManager == null) return
        val intent = Intent(INFO_STATISTICS)
        intent.putExtra(EXTRA_TOTAL, deviceManager!!.countAllDevices())
        intent.putExtra(EXTRA_MAX_ACTIVE, maxActiveDevices)
        intent.putExtra(EXTRA_ACTIVE, deviceManager!!.countActiveDevices())
        longestLatency = Math.max(deviceManager!!.maxLatency, longestLatency)
        intent.putExtra(EXTRA_MAX_LATENCY, longestLatency)
        //intent.putExtra(EXTRA_BLUETOOTH_STAT_EXTRA, BluetoothGattWrapper.getStat());
        sendBroadcast(intent)
    }

    fun addDevice(device: BluetoothDevice?): Boolean {
        return deviceManager != null && device != null && deviceManager!!.addDevice(device, this, this)
    }

    fun onBluetoothOff() {
        if (deviceManager == null) return
        deviceManager!!.closeAll()
    }

    fun onBluetoothOn() {
        if (deviceManager == null) return
        deviceManager!!.resetAll()
    }


    inner class LocalBinder : Binder() {
        val service: DeviceService
            get() = this@DeviceService
    }

    fun getDevice(address: String): SensorDevice? {
        if (deviceManager == null) return null
        return deviceManager!!.getDevice(address)
    }

    fun pairDeviceWithUser(address: String, user: User) {
        dataCollector!!.startRecording(address, user)
    }

    fun clearDevicePairing() {
        dataCollector!!.resetPairing()
    }

    fun getUser(address: String): User? {
        return dataCollector?.getUser(address)
    }

    private fun initialize(): Boolean {

        try {
            val devices = BluetoothAdapter.getDefaultAdapter().bondedDevices
            val removeBond = BluetoothDevice::class.java.getMethod("removeBond")
            for (device in devices) {
                removeBond.invoke(device)
            }
        } catch (e: Exception) {
            Log.w(TAG, " could not initialize devices", e)
            return false
        }


        if (this.deviceManager != null) deviceManager!!.doStop()
        try {
            Log.i(TAG, "using device manager : " + AbstractDeviceManager.deviceManagerClass.name)
            this.deviceManager = AbstractDeviceManager.deviceManagerClass.newInstance()
            this.deviceManager!!.setHeartRateListener(dataCollector)
        } catch (e: Exception) {
            Log.e(TAG, "could'not instantiate device manager of class " + AbstractDeviceManager.deviceManagerClass.name)
        }

        this.deviceManager!!.start()

        val savedAddresses = sharedPref!!.getStringSet(ADDRESSES, HashSet<String>())
        var device: BluetoothDevice?
        for (address in savedAddresses!!) {
            device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address)
            if (device != null)
                addDevice(device)
        }
        sharedPref!!.edit().putStringSet(ADDRESSES, HashSet<String>()).apply()

        return true

    }

    val allAddresses: Set<String>
        get() {
            if (deviceManager == null) return HashSet()
            return deviceManager!!.allAddresses()
        }

    companion object {

        private val TAG = DeviceService::class.java.simpleName

        val PREFIX = DeviceService::class.java.`package`.name + "."
        val EXTRA_HEART_RATE = PREFIX + "EXTRA_HEART_RATE"
        val EXTRA_ADDRESS = PREFIX + "EXTRA_ADDRESS"
        val EXTRA_LATENCY = PREFIX + "EXTRA_LATENCY"
        val INFO_STATISTICS = PREFIX + "INFO_STATISTICS"
        val EXTRA_TOTAL = PREFIX + "EXTRA_TOTAL"
        val EXTRA_ACTIVE = PREFIX + "EXTRA_ACTIVE"
        val EXTRA_MAX_ACTIVE = PREFIX + "EXTRA_MAX_ACTIVE"
        val EXTRA_MAX_LATENCY = PREFIX + "EXTRA_MAX_LATENCY"
        val EXTRA_BATTERY_LEVEL = PREFIX + "EXTRA_BATTERY_LEVEL"
        //public static final String EXTRA_BLUETOOTH_STAT_EXTRA = PREFIX + "EXTRA_BLUETOOTH_STAT_EXTRA";
        private val ADDRESSES = PREFIX + "ADDRESSES"

        val WAKELOCK_TAG = PREFIX + "WAKE_LOCK"

        private val ONGOING_NOTIFICATION_ID = 13

        private var shouldRun = false

        fun isRunning(context: Context?): Boolean {
            if (context == null) return false
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            for (serviceInfo in activityManager.getRunningServices(Integer.MAX_VALUE)) {
                if (DeviceService::class.java.name == serviceInfo.service.className) {
                    Log.i(TAG, "Service is running!")
                    return true
                }
            }
            return false
        }

        fun shouldRun(): Boolean {
            return shouldRun
        }
    }
}