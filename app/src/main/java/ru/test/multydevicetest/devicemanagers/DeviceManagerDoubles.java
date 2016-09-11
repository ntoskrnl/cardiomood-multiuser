package ru.test.multydevicetest.devicemanagers;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import java.util.ArrayList;

import ru.test.multydevicetest.bluetooth.IDeviceEventListener;
import ru.test.multydevicetest.bluetooth.SensorDevice;

/**
 * Created by Bes on 25.07.2016.
 */
public class DeviceManagerDoubles extends DeviceManager {

    private static final long MAX_LATENCY = 80000;
    public final int MIN_CHECK_PERIOD = 1000;
    public final int EXPECTED_WORK_TIME = 12000;

    private final static String TAG = DeviceManagerDoubles.class.getSimpleName();
    private volatile boolean isPaused = false;

    protected class DoubleDevice {
        private String[] addresses = new String[2];
        private int curIndex = 0;
        private int lastIndex = 1;



        public boolean addDevice(String address) {
            if (this.addresses[curIndex] != null && this.addresses[lastIndex] != null) return false;
            if (addresses[curIndex] != null)
                switchIndex();
            this.addresses[curIndex] = address;
            return true;
        }

        private void switchIndex() {
            lastIndex = curIndex++;
            if (curIndex > 1) curIndex = 0;
        }

        public String currentAddress() {
            return addresses[curIndex];
        }

        public String lastAddress() {
            return addresses[lastIndex];
        }

        public boolean addFistDevice(String address) {
           return addNDevice(0,address);
        }

        public boolean addSecondDevice(String address) {
            return addNDevice(1,address);
        }

        private boolean addNDevice(int index, String address)
        {
            if(addresses[index] != null) return false;
            addresses[index] = address;
            return true;
        }
    }

    DoubleDevice[] doubles = null;

    public DeviceManagerDoubles()
    {
        doubles = new DoubleDevice[maxActiveDevices];
        for (int i = 0; i < doubles.length; i++) doubles[i] = new DoubleDevice();
    }

    @Override
    public void run() {
        try {
            long lastCycleTime = System.currentTimeMillis();
            long timeToWait;
            ArrayList<String> addressesToRemove= new ArrayList<>();
            while (isRunning) {
                timeToWait = lastCycleTime + MIN_CHECK_PERIOD - (lastCycleTime = System.currentTimeMillis());
                if(timeToWait > 100)
                    sleep(timeToWait);
                if (doubles == null || isPaused) continue;
                DoubleDevice[] curDoubles = doubles;//async null pointer protection

                for (DoubleDevice doubleDevice : curDoubles) {
                    switchDeviceIfNeeded(doubleDevice);

                    if(checkForExceedMaxLatency(doubleDevice.currentAddress()))
                        addressesToRemove.add(doubleDevice.currentAddress());
                    if(checkForExceedMaxLatency(doubleDevice.lastAddress()))
                        addressesToRemove.add(doubleDevice.lastAddress());
                }

                for (String address: addressesToRemove) {
                    sensorDevices.get(address).close();
                    sensorDevices.remove(address);
                }

                if(addressesToRemove.size() > 0)
                {
                    rearrangeDoubles();
                    addressesToRemove.clear();
                }
            }
            Log.d(TAG + ":" + this.getClass().getSimpleName(), "finished cycling");
        } catch (InterruptedException e) {
            Log.d(TAG + ":" + this.getClass().getSimpleName(), "interrupted");
            isRunning = false;
        }

    }

    public boolean checkForExceedMaxLatency(String address) {
        return
                address != null &&
                sensorDevices.get(address) != null &&
                sensorDevices.get(address).getLatency() > MAX_LATENCY;
    }

    protected void switchDeviceIfNeeded(DoubleDevice doubleDevice) {
        if(!isRunning || isPaused) return;
        String address = doubleDevice.currentAddress();
        if (address == null) {
            doubleDevice.switchIndex();
            return;
        }
        SensorDevice curDevice = sensorDevices.get(address);
        if ((curDevice.isDisconnected() && !curDevice.connect()) || (
                doubleDevice.lastAddress() != null &&
                                curDevice.getLastConnectionStatusTime() > EXPECTED_WORK_TIME)
                ) {
            curDevice.disconnect();
            doubleDevice.switchIndex();
            address = doubleDevice.currentAddress();
            if( address!= null )
                sensorDevices.get(doubleDevice.currentAddress()).connect();
        }
    }

    @Override
    public boolean addDevice(BluetoothDevice device, Context ctx, IDeviceEventListener
            listener) {
        String name = device.getName();
        return !(name == null || !name.contains("Polar")) &&
                super.addDevice(device, ctx, listener) &&
                addAddressToDouble(device.getAddress());
    }

    private boolean addAddressToDouble(String address)
    {
        for (DoubleDevice aDouble : doubles)
            if (aDouble.addFistDevice(address))
                return true;

        for (DoubleDevice aDouble : doubles)
            if (aDouble.addSecondDevice(address))
                return true;

        return false;
    }

    private void rearrangeDoubles() {
        isPaused = true;
        if (maxActiveDevices <= 0) return;
        doubles = new DoubleDevice[maxActiveDevices];
        for (int i = 0; i < doubles.length; i++) doubles[i] = new DoubleDevice();
        ArrayList<String> addressesToRemove = new ArrayList<>();
        for (String address : sensorDevices.keySet()) {
            if(!addAddressToDouble(address))
                addressesToRemove.add(address);
        }
        isPaused = false;

        for(String address :addressesToRemove)
            sensorDevices.remove(address);
    }

    private void removeDevice(String address)
    {
        sensorDevices.remove(address);
    }

    @Override
    public int maxActiveInc() {
        if (maxActiveDevices < MAX_ACTIVE_DEVICES) {
            maxActiveDevices++;
            rearrangeDoubles();
        }
        return maxActiveDevices;
    }

    @Override
    public int maxActiveDec() {
        if (maxActiveDevices > 2) {
            maxActiveDevices--;
            rearrangeDoubles();
        }
        return maxActiveDevices;
    }


}
