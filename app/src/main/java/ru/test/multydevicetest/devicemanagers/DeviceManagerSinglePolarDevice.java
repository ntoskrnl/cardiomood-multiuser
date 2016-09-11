package ru.test.multydevicetest.devicemanagers;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import ru.test.multydevicetest.bluetooth.IDeviceEventListener;
import ru.test.multydevicetest.bluetooth.SensorDevice;

/**
 * Created by Bes on 24.07.2016.
 */
public class DeviceManagerSinglePolarDevice extends AbstractDeviceManager {
    private final static String TAG = DeviceManagerSinglePolarDevice.class.getSimpleName();

    public final int MIN_CHECK_PERIOD = 1000;
    public final int EXPECTED_WORK_TIME = 20000;

    private SensorDevice device = null;
    private String address = null;

    private boolean isRunning = true;

    /**
     * Calls the <code>run()</code> method of the Runnable object the receiver
     * holds. If no Runnable is set, does nothing.
     *
     * @see Thread#start
     */

    @Override
    public void run() {
        super.run();

        int curPos = 0;
        int sleepTime = MIN_CHECK_PERIOD;
        try {
            while (isRunning) {
                if(device != null) {
                    if (device.isDisconnected())
                        device.connect();
                    else if (device.isTransmitting() && device.getLastConnectionStatusTime() > EXPECTED_WORK_TIME)
                        device.disconnect();
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
        if(device == null) return 0;
         else return device.getLatency();
    }

    private boolean checkIfActiveDevice(String address) {
        if(address == null || this.address == null) return  false;
        return  address.equals(this.address);
    }

    private boolean activateDevice(String address) {
        if(address == null || this.address == null) return  false;
        return device.connect();
    }


    @Override
    public int countAllDevices() {
        return device == null ? 0 : 1;
    }

    @Override
    public int countActiveDevices() {
        return device == null ? 0 : !device.isConnected() && !device.isTransmitting() ? 0 : 1;
    }

    @Override
    public Collection<SensorDevice> allDevices() {
       ArrayList<SensorDevice> ret = new ArrayList<>(1);
        if(device != null) ret.add(device);
        return ret;
    }

    @Override
    public Set<String> allAddresses() {
        Set<String> ret = new HashSet<>(1);
        if(address != null) ret.add(address);
        return ret;
    }

    @Override
    public boolean addDevice(BluetoothDevice device, Context ctx, IDeviceEventListener listener) {
        if(device == null) return false;
        if(this.device != null) return false;
        if(!device.getName().contains("Polar")) return false;
        this.device = SensorDevice.newInstance(ctx,device,listener);
        this.address = device.getAddress();
        Log.d(TAG, "added device: " + device.getName() + " with address " + device.getAddress());
        return true;
    }

    @Override
    public void doStop() {
        isRunning = false;
        if(device != null) device.close(false);
        device = null;
        address = null;
        if(this.isAlive()) this.interrupt();
    }


    @Override
    public void closeAll() {
        if(device != null) device.close(true);
    }

    @Override
    public void resetAll() {
        if(device != null) device.reset();
    }

    @Override
    public SensorDevice getDevice(String address) {
        if(address == null || this.address == null) return  null;
        if(address.equals(this.address)) return device;
        else return null;
    }

    @Override
    public int getMaxActiveDevices() {
        return 1;
    }

    @Override
    public int maxActiveInc() {
        return 1;
    }

    @Override
    public int maxActiveDec() {
        return 1;
    }
}
