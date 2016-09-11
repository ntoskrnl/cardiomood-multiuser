package ru.test.multydevicetest.devicemanagers;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import ru.test.multydevicetest.bluetooth.HeartRateListener;
import ru.test.multydevicetest.bluetooth.IDeviceEventListener;
import ru.test.multydevicetest.bluetooth.SensorDevice;

/**
 * Created by Bes on 24.07.2016.
 */
public class DeviceManagerDoublePolar extends AbstractDeviceManager {

    private final static String TAG = DeviceManagerSinglePolarDevice.class.getSimpleName();

    public final int MIN_CHECK_PERIOD = 1000;
    public final int EXPECTED_WORK_TIME = 10000;

    protected SensorDevice[] devices = new SensorDevice[2];
    private String[] addresses = new String[2];

    protected boolean isRunning = true;

    /**
     * Calls the <code>run()</code> method of the Runnable object the receiver
     * holds. If no Runnable is set, does nothing.
     *
     * @see Thread#start
     */

    @Override
    public void run() {

        int index = 0;
        int lastIndex = 1;
        try {
            while (isRunning) {
                if(devices[index] != null && devices[lastIndex] != null) {
                    if (devices[index].isDisconnected() && devices[lastIndex].isDisconnected()) {
                        devices[index].connect();
                        lastIndex = index++;
                        if(index > 1) index = 0;
                    }
                    else if (devices[lastIndex].getLastConnectionStatusTime() > EXPECTED_WORK_TIME)
                        devices[lastIndex].disconnect();
                }
                sleep(MIN_CHECK_PERIOD);
            }
        } catch (InterruptedException e)
        {
            Log.d(TAG+":"+this.getClass().getSimpleName(),"interrupted");
            isRunning = false;
        }
    }

    @Override
    public long getMaxLatency() {
        if(devices[0] == null && devices[1] == null) return 0;
        if(devices[0] == null) return devices[1].getLatency();
        if(devices[1] == null) return devices[0].getLatency();
        else return Math.max(devices[0].getLatency(),devices[1].getLatency());
    }

    @Override
    public int countAllDevices() {
        int ret = 0;
        if(devices[0] != null) ret++;
        if(devices[1] != null) ret++;
        return ret;
    }

    @Override
    public int countActiveDevices() {
        int ret = 0;
        if(devices[0] != null && (devices[0].isConnected() || devices[0].isTransmitting())) ret++;
        if(devices[1] != null && (devices[1].isConnected() || devices[1].isTransmitting())) ret++;
        return ret;
    }

    @Override
    public Collection<SensorDevice> allDevices() {
        ArrayList<SensorDevice> ret = new ArrayList<>(2);
        if(devices[0] != null) ret.add(devices[0]);
        if(devices[1] != null) ret.add(devices[1]);
        return ret;
    }

    @Override
    public Set<String> allAddresses() {
        Set<String> ret = new HashSet<>(2);
        if(addresses[0] != null) ret.add(addresses[0]);
        if(addresses[1] != null) ret.add(addresses[1]);
        return ret;
    }

    @Override
    public boolean addDevice(BluetoothDevice device, Context ctx, IDeviceEventListener listener) {
        if(device == null) return false;
        if(this.devices[0] != null && this.devices[1] != null) return false;
        if(!device.getName().contains("Polar")) return false;
        int index = 0;
        if(devices[index] != null)
        {
            if(addresses[index].equals(device.getAddress())) return false;
            index++;
        }
        this.devices[index] = SensorDevice.newInstance(ctx,device,listener);
        this.addresses[index] = device.getAddress();
        Log.d(TAG, "added device: " + device.getName() + " with address " + device.getAddress());
        return true;
    }

    @Override
    public void doStop() {
        isRunning = false;
        if(devices[0] != null) devices[0].disconnect();
        if(devices[1] != null) devices[1].disconnect();
        devices[0] = devices[1] = null;
        addresses[0] = addresses[1] = null;
        if(this.isAlive()) this.interrupt();
    }


    @Override
    public void closeAll() {
        if(devices[0] != null) devices[0].close(true);
        if(devices[1] != null) devices[1].close(true);
    }

    @Override
    public void resetAll() {
        if(devices[0] != null) devices[0].reset();
        if(devices[1] != null) devices[1].reset();
    }

    @Override
    public SensorDevice getDevice(String address) {
        if(address == null ) return  null;
        if(address.equals(this.addresses[0])) return devices[0];
        if(address.equals(this.addresses[1])) return devices[1];
        return null;
    }

    @Override
    public int getMaxActiveDevices() {
        return 2;
    }

    @Override
    public int maxActiveInc() {
        return 2;
    }

    @Override
    public int maxActiveDec() {
        return 2;
    }

    @Override
    public void setHeartRateListener(HeartRateListener listener) {
        // this implementation doesn't support this type of listener
    }
}
