package ru.test.multydevicetest.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.util.Log;

import com.cardiomood.multiuser.R;

import java.lang.reflect.Constructor;
import java.util.Calendar;
import java.util.UUID;

/**
 * Created by Bes on 18.08.2016.
 */
public abstract class SensorDevice {

    private static final Class<? extends SensorDevice> deviceClass = SensorDeviceRx.class;
    //private static final Class<? extends SensorDevice> deviceClass = SensorDeviceClassic.class;

    private static final Constructor<? extends SensorDevice> deviceConstructor;
    static {
        Constructor<? extends SensorDevice> deviceConstructorTmp;
        try {
            deviceConstructorTmp = deviceClass.getDeclaredConstructor(Context.class,BluetoothDevice.class, IDeviceEventListener.class);
            deviceConstructorTmp.setAccessible(true);
        } catch (NoSuchMethodException e) {
            deviceConstructorTmp = null;
        }
        deviceConstructor = deviceConstructorTmp;
    }

    public static final String ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public static final String ACTION_GATT_DISCONNECTED ="com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public static final String ACTION_GATT_SERVICES_UNSUPPORTED = "com.example.bluetooth.le.ACTION_GATT_SERVICES_UNSUPPORTED";
    public static final String ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public static final String ACTION_BATTERY_LEVEL = "com.example.bluetooth.le.ACTION_BATTERY_LEVEL";
    public static final UUID BATTERY_CHARACTERISTIC = UUID.fromString(H7GattAttributes.BATTERY_CHARACTERISTIC);
    public static final UUID HEART_RATE_CHARACTERISTIC = UUID.fromString(H7GattAttributes.HEART_RATE_CHARACTERISTIC);
    public static final UUID BATTERY_SERVICE = UUID.fromString(H7GattAttributes.BATTERY_SERVICE);
    public static final UUID HEART_RATE_SERVICE = UUID.fromString(H7GattAttributes.HEART_RATE_SERVICE);

    private static final long DATA_ON_TIMEOUT = 2500;

    public class Status {

        protected final Object syncStateObject = new Object();

        protected static final int UNSUPPORTED = -2;
        protected static final int ERROR = -1;
        protected static final int DISCONNECTED = 0;
        protected static final int CONNECTING = 1;
        protected static final int CONNECTED = 2;
        protected static final int DISCONNECTING = 3;
        protected static final int TRANSMITTING = 4;
        protected static final int CLOSING = 5;

        protected int status = DISCONNECTED;
        protected long whenStatusChangedMs = 0;

        protected void setStatus(int newStatus) {
            if (newStatus == status || status == UNSUPPORTED) return;

            Log.d(tag(), "Changing status from " + toString() + " to " + toString(newStatus));

            synchronized (syncStateObject) {
                whenStatusChangedMs = System.currentTimeMillis();
                status = newStatus;
            }

        }

        public boolean isInStatus(int checkStatus) {
            synchronized (syncStateObject) {
                return status == checkStatus;
            }
        }


        public boolean isInStatus(int... checkStatuses) {
            synchronized (syncStateObject) {
                for (int checkStatus : checkStatuses)
                    if (status == checkStatus) return true;
                return false;
            }
        }

        public long getTimeInStatus() {
            synchronized (syncStateObject) {
                if (whenStatusChangedMs == 0) return -1;
                return System.currentTimeMillis() - whenStatusChangedMs;
            }
        }

        @Override
        public String toString() {
            return toString(status);
        }

        public String toString(int status) {
            synchronized (syncStateObject) {
                switch (status) {
                    case UNSUPPORTED:
                        return ctx.getString(R.string.unsupported_status);
                    case ERROR:
                        return ctx.getString(R.string.error_status);
                    case DISCONNECTED:
                        return ctx.getString(R.string.disconnected_status);
                    case DISCONNECTING:
                        return ctx.getString(R.string.disconnecting_status);
                    case CONNECTING:
                        return ctx.getString(R.string.connecting_status);
                    case CONNECTED:
                        return ctx.getString(R.string.connected_status);
                    case TRANSMITTING:
                        return ctx.getString(R.string.transmitting_status);
                    case CLOSING:
                        return ctx.getString(R.string.closing_status);
                    default:
                        return ctx.getString(R.string.unknown_status);
                }
            }
        }
    }


    protected Status connectionState = new Status();

    protected BluetoothGattCharacteristic heartRateCharacteristic = null;
    protected BluetoothGattCharacteristic batteryLevelCharacteristic = null;

    protected String address;
    protected String name;

    protected long activationTime = System.currentTimeMillis();
    protected long lastChangeConnectionTime = 0;
    protected long lastDataTime = System.currentTimeMillis();
    protected long lastLatency = 0;
    protected int lastHeartRate = 0;
    protected boolean isConnected;
    protected int lastBatteryLevel = -1;
    protected DutyCycleTime lastDutyCycle = new DutyCycleTime();
    private DutyCycleTime curDutyCycle = new DutyCycleTime();

    protected HeartRateListener heartRateListener = null;

    protected boolean keepClosed = false;

    protected abstract String tag();

    public int deviceManagerId = 0;

    protected final Context ctx;
    protected final IDeviceEventListener listener;

    public static SensorDevice newInstance(Context ctx, BluetoothDevice device, IDeviceEventListener listener) {
        //return new SensorDeviceClassic(ctx,device,listener);
        //return new SensorDeviceRx(ctx,device,listener);
        try {
            return deviceConstructor.newInstance(ctx,device,listener);
        } catch (Exception e) {
            return null;
        }
    }

    protected SensorDevice(Context ctx, BluetoothDevice device, IDeviceEventListener listener)
    {
        this.ctx = ctx;
        this.address = device.getAddress();
        this.name = device.getName();
        this.listener = listener;

    }

    protected static long getTime() {
        //return System.currentTimeMillis();
        return Calendar.getInstance().getTimeInMillis();
    }

    protected void onDataReceived()
    {
        long newDataTime = getTime();
        //Log.d(tag(), " lastDataTime =  " +lastDataTime + ", newDataTime = " +newDataTime);
        lastLatency = lastDataTime > 0 ? newDataTime - lastDataTime : 0;
        lastDataTime = newDataTime;

        if (lastLatency > DATA_ON_TIMEOUT) {
            lastDutyCycle.on = curDutyCycle.on;
            lastDutyCycle.off = lastLatency;
            curDutyCycle.on = 0;
            //Log.d(tag(), "duty cycle finished after delay of " + lastLatency);
        } else {
            curDutyCycle.on += lastLatency;
            //Log.d(tag(), "On time = " + curDutyCycle.on + ", latency = " + lastLatency);
        }
    }

    public long getLatency()
    {
        return getTime() - (lastDataTime > 0 ? lastDataTime : activationTime);
    }

    public int getHeartRate(){
        return lastHeartRate;
    }

    public int getBatteryLevel()
    {
        return lastBatteryLevel;
    }

    public boolean isConnecting() {
        return connectionState.isInStatus(Status.CONNECTING);
    }


    public boolean isTransmitting() {
        return connectionState.isInStatus(Status.TRANSMITTING);
    }

    public boolean getLastConnectionState() {
        return isConnected;
    }

    public long getLastConnectionStatusTime() {
        if (lastChangeConnectionTime == 0) return 0;
        return getTime() - lastChangeConnectionTime;
    }

    public long getTimeInStatus()
    {
        return connectionState.getTimeInStatus();
    }

    public long getStatusTime()
    {
        synchronized (connectionState.syncStateObject) {
            return connectionState.whenStatusChangedMs;
        }
    }

    public String getStatus() {
        return connectionState.toString();

    }

    public boolean isConnected() {
        return connectionState.isInStatus(Status.CONNECTED);
    }

    public boolean isDisconnected() {
        return connectionState.isInStatus(Status.DISCONNECTED);
    }

    public long getActivationTime() {
        return activationTime;
    }

    public String getAddress() {
        return address;
    }

    public String getName() {
        return name;
    }

    public long getLastDataTime() {
        return lastDataTime > 0 ? lastDataTime : activationTime;
    }

    public void close(){close(false);}

    public void setHeartRateListener(HeartRateListener listener) {
        heartRateListener = listener;
    }

    public int getLastHeartRate() {
        return lastHeartRate;
    }

    public abstract boolean connect();
    public abstract void disconnect();
    public abstract void close(boolean keepClosed);
    public abstract void reset();

    public DutyCycleTime getLastDutyCycle() {
        lastDutyCycle.setTransmitting(connectionState.isInStatus(Status.TRANSMITTING));
        return lastDutyCycle;
    }

}
