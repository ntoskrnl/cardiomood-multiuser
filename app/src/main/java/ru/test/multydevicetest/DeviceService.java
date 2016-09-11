package ru.test.multydevicetest;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import ru.test.multydevicetest.bluetooth.IDeviceEventListener;
import ru.test.multydevicetest.bluetooth.SensorDevice;
import ru.test.multydevicetest.deviceManagers.AbstractDeviceManager;
import ru.test.multydevicetest.ui.OverviewActivity;

public class DeviceService extends Service implements IDeviceEventListener {

    private final static String TAG = DeviceService.class.getSimpleName();

    public static final String SERVER_ADDRESS = "http://192.168.2.96:8881/Log/Write";

    public static final String PREFIX = DeviceService.class.getPackage().getName() + ".";
    public final static String EXTRA_HEART_RATE = PREFIX + "EXTRA_HEART_RATE";
    public final static String EXTRA_ADDRESS = PREFIX + "EXTRA_ADDRESS";
    public static final String EXTRA_LATENCY = PREFIX + "EXTRA_LATENCY";
    public static final String INFO_STATISTICS = PREFIX + "INFO_STATISTICS";
    public static final String EXTRA_TOTAL = PREFIX + "EXTRA_TOTAL";
    public static final String EXTRA_ACTIVE = PREFIX + "EXTRA_ACTIVE";
    public static final String EXTRA_MAX_ACTIVE = PREFIX + "EXTRA_MAX_ACTIVE";
    public static final String EXTRA_MAX_LATENCY = PREFIX + "EXTRA_MAX_LATENCY";
    public static final String EXTRA_BATTERY_LEVEL = PREFIX + "EXTRA_BATTERY_LEVEL";
    //public static final String EXTRA_BLUETOOTH_STAT_EXTRA = PREFIX + "EXTRA_BLUETOOTH_STAT_EXTRA";
    private static final String ADDRESSES = PREFIX +"ADDRESSES";

    public static final String WAKELOCK_TAG = PREFIX +"WAKE_LOCK";

    private static final int ONGOING_NOTIFICATION_ID = 13;

    private static boolean shouldRun =false;


    protected PowerManager.WakeLock wakeLock;
    private AbstractDeviceManager deviceManager;
    private DataManager dataManager;
    private long longestLatency = 0;
    private boolean isStarted = false;
    private SharedPreferences sharedPref;


    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        dataManager = new DataManager(SERVER_ADDRESS);

        sharedPref = getApplicationContext().getSharedPreferences(
                "prefs",MODE_PRIVATE);


        Intent intentForeground = new Intent(this, OverviewActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_RECEIVER_FOREGROUND);

        PendingIntent pendIntent = PendingIntent.getActivity(this, 0,
                intentForeground, 0);


        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setTicker("MultiDeviceTest")
                .setContentTitle("Multiple Bluetooth Device test")
                .setContentText("Touch to start overview")
                .setContentIntent(pendIntent)
                .setDefaults(Notification.DEFAULT_ALL).setOngoing(true)
                .setOnlyAlertOnce(true).setAutoCancel(false).build();


        startForeground(ONGOING_NOTIFICATION_ID, notification);


        PowerManager powerManager = (PowerManager)getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.PARTIAL_WAKE_LOCK,WAKELOCK_TAG);
        wakeLock.acquire();

        shouldRun = true;
        AlarmReceiver.startAlarm(this);
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");

        if(shouldRun)
        {
            sharedPref.edit().putStringSet(ADDRESSES,deviceManager.allAddresses()).apply();
            doStop(false);
        } else
        {
            sharedPref.edit().putStringSet(ADDRESSES,new HashSet<String>()).apply();
        }

        if(wakeLock != null && wakeLock.isHeld())
            wakeLock.release();
        super.onDestroy();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStart");
        initialize();
        isStarted = true;
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        if (!isStarted) {
            Intent startIntent = new Intent(this, this.getClass());
            startIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            startService(startIntent);
        }
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind");
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();


    @Override
    public void onAction(String address, String action) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_ADDRESS, String.valueOf(address));
        sendBroadcast(intent);

    }

    @Override
    public void onHeartRate(final String address, final int heartRate, final long latency) {
        //Intent intent = new Intent(SensorDevice.ACTION_DATA_AVAILABLE);
        //
        //intent.putExtra(EXTRA_ADDRESS, address);
        //intent.putExtra(EXTRA_HEART_RATE, String.valueOf(heartRate));
        //intent.putExtra(EXTRA_LATENCY, dutyCycle);
        //sendBroadcast(intent);

        if (dataManager != null)
            dataManager.addRecord(address, Calendar.getInstance().getTime(), heartRate);

        doUpdateStats();

    }

    @Override
    public void onBatteryLevel(final String address, final int batteryLevel) {
        Intent intent = new Intent(SensorDevice.ACTION_BATTERY_LEVEL);

        intent.putExtra(EXTRA_ADDRESS, address);
        intent.putExtra(EXTRA_BATTERY_LEVEL, batteryLevel);
        sendBroadcast(intent);
    }

    public void doStop()
    {
        doStop(true);
    }
    public void doStop( boolean isExpected) {
        Log.d(TAG, "doStop, expected : " + isExpected);
        if (deviceManager != null) {
            deviceManager.doStop();
            deviceManager = null;

        }

        if (dataManager != null) {
            dataManager.dispose();
            dataManager = null;
        }
        if(isExpected)
            shouldRun = false;


        stopSelf();
    }

    public int getMaxActiveDevices() {
        if(deviceManager == null) return 0;
        return deviceManager.getMaxActiveDevices();
    }

    public int maxActiveCountInc() {
        if(deviceManager == null) return 0;
        return deviceManager.maxActiveInc();
    }

    public int maxActiveCountDec() {
        if(deviceManager == null) return 0;
        return deviceManager.maxActiveDec();
    }

    public void doUpdateStats() {
        if(deviceManager == null) return;
        Intent intent = new Intent(INFO_STATISTICS);
        intent.putExtra(EXTRA_TOTAL, deviceManager.countAllDevices());
        intent.putExtra(EXTRA_MAX_ACTIVE, getMaxActiveDevices());
        intent.putExtra(EXTRA_ACTIVE, deviceManager.countActiveDevices());
        long latency = deviceManager.getMaxLatency();
        latency = (longestLatency > latency) ? longestLatency : (longestLatency = latency);
        intent.putExtra(EXTRA_MAX_LATENCY, latency);
        //intent.putExtra(EXTRA_BLUETOOTH_STAT_EXTRA, BluetoothGattWrapper.getStat());
        sendBroadcast(intent);
    }

    public boolean addDevice(BluetoothDevice device) {
        return deviceManager != null && device != null && deviceManager.addDevice(device, this, this);
    }

    public void onBluetoothOff() {
        if(deviceManager == null) return;
        deviceManager.closeAll();
    }

    public void onBluetoothOn() {
        if(deviceManager == null) return;
        deviceManager.resetAll();
    }

    public static boolean isRunning(Context context) {
        if(context == null) return false;
        ActivityManager activityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        for(ActivityManager.RunningServiceInfo serviceInfo : activityManager.getRunningServices(Integer.MAX_VALUE))
        {
            if(DeviceService.class.getName().equals(serviceInfo.service.getClassName()))
            {
                Log.i(TAG, "Service is running!");
                return true;
            }
        }
        return false;
    }

    public static boolean shouldRun() {
        return shouldRun;
    }


    public class LocalBinder extends Binder {
        public DeviceService getService() {
            return DeviceService.this;
        }
    }

    public SensorDevice getDevice(String address) {
        if(deviceManager == null) return null;
        return deviceManager.getDevice(address);
    }

    private boolean initialize() {

        try {
            Set<BluetoothDevice> devices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
            Method removeBond = BluetoothDevice.class
                    .getMethod("removeBond", (Class[]) null);
            for (BluetoothDevice device : devices) {

                removeBond.invoke(device, (Object[]) null);
            }
        } catch (Exception e) {
            Log.w(TAG, " could not initialize devices", e);
            return false;
        }



        if (this.deviceManager != null) deviceManager.doStop();
        try {
            Log.i(TAG, "using device manager : " + AbstractDeviceManager.deviceManagerClass.getName());
            this.deviceManager = AbstractDeviceManager.deviceManagerClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            Log.e(TAG, "could'not instantiate device manager of class " + AbstractDeviceManager.deviceManagerClass.getName());
        }
        this.deviceManager.start();

        Set<String> savedAddresses = sharedPref.getStringSet(ADDRESSES,new HashSet<String>());
        BluetoothDevice device;
        for(String address : savedAddresses)
        {
            device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address);
            if(device != null)
                addDevice(device);
        }
        sharedPref.edit().putStringSet(ADDRESSES,new HashSet<String>()).apply();

        return true;

    }

    public Set<String> getAllAddresses() {
        if(deviceManager == null) return new HashSet<>();
        return deviceManager.allAddresses();
    }
}
