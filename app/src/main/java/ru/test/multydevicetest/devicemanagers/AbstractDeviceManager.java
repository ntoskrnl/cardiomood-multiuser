package ru.test.multydevicetest.devicemanagers;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import java.util.Collection;
import java.util.Set;

import ru.test.multydevicetest.bluetooth.IDeviceEventListener;
import ru.test.multydevicetest.bluetooth.SensorDevice;

/**
 * Created by Bes on 24.07.2016.
 */
public abstract class AbstractDeviceManager extends Thread{

    public static Class<? extends AbstractDeviceManager> deviceManagerClass = DeviceManager.class;
    //public static Class<? extends AbstractDeviceManager> deviceManagerClass = DeviceManagerSinglePolarDevice.class;
    //public static Class<? extends AbstractDeviceManager> deviceManagerClass = DeviceManagerDoublePolar.class;
    //public static Class<? extends AbstractDeviceManager> deviceManagerClass = DeviceManagerDoublePolarNoWait.class;
    //public static Class<? extends AbstractDeviceManager> deviceManagerClass = DeviceManagerDoubles.class;
    //public static Class<? extends AbstractDeviceManager> deviceManagerClass = DeviceManagerDoublesNonSimultaneous.class;
    //public static Class<? extends AbstractDeviceManager> deviceManagerClass = DeviceManagerDoublesMultiThread.class;
    //public static Class<? extends AbstractDeviceManager> deviceManagerClass = DeviceManagerSimple.class;

    public abstract long getMaxLatency();

    public abstract int countAllDevices();

    public abstract int countActiveDevices();

    public abstract Collection<SensorDevice> allDevices();

    public abstract Set<String> allAddresses();

    public abstract boolean addDevice(BluetoothDevice device, Context ctx, IDeviceEventListener listener);

    public abstract void doStop();

    public abstract void closeAll();

    public abstract void resetAll();

    public abstract SensorDevice getDevice(String address);

    public abstract int getMaxActiveDevices();

    public abstract int maxActiveInc();

    public abstract int maxActiveDec();
}
