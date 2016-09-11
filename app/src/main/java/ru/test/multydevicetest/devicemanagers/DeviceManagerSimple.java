package ru.test.multydevicetest.devicemanagers;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import ru.test.multydevicetest.bluetooth.IDeviceEventListener;
import ru.test.multydevicetest.bluetooth.SensorDevice;

/**
 * Created by Bes on 16.08.2016.
 */
public class DeviceManagerSimple extends AbstractDeviceManager {
    private final static String TAG = DeviceManagerSimple.class.getSimpleName();

    protected ConcurrentHashMap<String,SensorDevice> sensorDevices = new ConcurrentHashMap<>();
    public final int MAX_ACTIVE_DEVICES = 100;
    public final int MIN_CHECK_PERIOD = 2000;
    protected boolean isRunning = true;

    protected int maxActiveDevices = 7;


    /**
     * Calls the <code>run()</code> method of the Runnable object the receiver
     * holds. If no Runnable is set, does nothing.
     *
     * @see Thread#start
     */
    @Override
    public void run() {

        try {
        while (isRunning)
        {
            for(SensorDevice device : sensorDevices.values()) {
                if(!isRunning) break;
                Log.d(TAG, "checking " + device.getAddress());
                if(device.isDisconnected())
                    device.connect();
                if((device.isConnecting() || device.isTransmitting()) && device.getTimeInStatus() > 5000)
                    device.disconnect();
                sleep(MIN_CHECK_PERIOD);
            }
        }
        } catch (InterruptedException e)
        {
            Log.d(TAG,"interrupted");
            isRunning = false;
        }
    }

    @Override
    public long getMaxLatency() {
        return 0;
    }

    @Override
    public int countAllDevices() {
        return sensorDevices.size();
    }

    @Override
    public int countActiveDevices() {
        int ret = 0;
        for(SensorDevice device : sensorDevices.values())
            if(device.isConnected() || device.isTransmitting()) ret++;
        return ret;
    }


    @Override
    public Collection<SensorDevice> allDevices() {
        return sensorDevices.values();
    }

    @Override
    public Set<String> allAddresses() {
        return sensorDevices.keySet();
    }

    @Override
    public boolean addDevice(BluetoothDevice device, Context ctx, IDeviceEventListener listener) {
        if(device == null) return false;
        if(sensorDevices.size() >= maxActiveDevices) return false;
        if(sensorDevices.containsKey(device.getAddress())) return false;
        sensorDevices.put(device.getAddress(),SensorDevice.newInstance(ctx,device,listener));
        Log.d(TAG, "added device: " + device.getName() + " with address " + device.getAddress());
        return true;
    }

    @Override
    public void doStop() {
        isRunning = false;
        for(SensorDevice device : sensorDevices.values())
            device.disconnect();
        sensorDevices.clear();
        if(this.isAlive()) this.interrupt();
    }


    @Override
    public void closeAll() {
        for(SensorDevice device : sensorDevices.values())
            device.close(true);
    }

    @Override
    public void resetAll() {
        for(SensorDevice device : sensorDevices.values())
            device.reset();
    }

    @Override
    public SensorDevice getDevice(String address) {
        if(!sensorDevices.containsKey(address))
            return null;
        else
            return sensorDevices.get(address);
    }

    @Override
    public int getMaxActiveDevices() {
        return maxActiveDevices;
    }

    @Override
    public int maxActiveInc() {
        if(maxActiveDevices < MAX_ACTIVE_DEVICES) maxActiveDevices++;
        return maxActiveDevices;
    }

    @Override
    public int maxActiveDec() {
        if(maxActiveDevices > 1) maxActiveDevices--;
        while (sensorDevices.size() > maxActiveDevices)
        {
            String addressToDelete = sensorDevices.keySet().iterator().next();
            sensorDevices.get(addressToDelete).disconnect();
            sensorDevices.remove(addressToDelete);
        }
        return maxActiveDevices;
    }


}
