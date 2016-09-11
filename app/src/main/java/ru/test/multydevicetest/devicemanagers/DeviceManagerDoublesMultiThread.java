package ru.test.multydevicetest.devicemanagers;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

import ru.test.multydevicetest.bluetooth.IDeviceEventListener;
import ru.test.multydevicetest.bluetooth.SensorDevice;

/**
 * Created by Bes on 30.07.2016.
 */
public class DeviceManagerDoublesMultiThread extends DeviceManager {

    private static final long MAX_LATENCY = 160000;
    public final int MIN_CHECK_PERIOD = 2000;
    public final int EXPECTED_WORK_TIME = 14000;
    private static final long CONNECTION_TIMEOUT = 7000;

    private final static String TAG = DeviceManagerDoublesMultiThread.class.getSimpleName();
    private volatile boolean isPaused = false;
    private final Object doublesSyncObject = new Object();
    protected int maxActiveDevices = 7;

    protected class DoubleDevice extends Thread {


        private final Object syncObject = new Object();
        private volatile long lastSwitchTime = System.currentTimeMillis();

        private String[] addresses = new String[2];
        private SensorDevice[] devices = new SensorDevice[2];
        private int curIndex = 0;
        private int lastIndex = 1;
        public int id = -1;

        private boolean switchIndex() {
            synchronized (syncObject) {
                if (addresses[lastIndex] == null)
                    return false;
                Log.d(tag(), "switching from " + addresses[curIndex] + " to " + addresses[lastIndex]);
                lastIndex = curIndex++;
                if (curIndex > 1) curIndex = 0;
                lastSwitchTime = System.currentTimeMillis();
                return true;
            }
        }

        private String tag() {
            return TAG + " : " +id;
        }

        public String currentAddress() {

            synchronized (syncObject) {
                return addresses[curIndex];
            }
        }

        public String lastAddress() {

            synchronized (syncObject) {
                return addresses[lastIndex];
            }
        }

        public boolean addFistDevice(String address) {
            return addNDevice(0, address);
        }

        public boolean addSecondDevice(String address) {
            return addNDevice(1, address);
        }

        private boolean addNDevice(int index, String address) {
            synchronized (syncObject) {
                if (addresses[index] != null) return false;
                addresses[index] = address;
                devices[index] = sensorDevices.get(address);
                devices[index].deviceManagerId = id;
                lastSwitchTime = System.currentTimeMillis();
                return true;
            }
        }

        private boolean isRunning = true;

        @Override
        public void run() {
            SensorDevice curDevice, lastDevice;
            try {
                sleep((long) (Math.random() * (double) EXPECTED_WORK_TIME));
                Log.d(tag(), "loop for double started");
                while (isRunning) {
                    synchronized (syncObject) {
                        curDevice = devices[curIndex];
                        lastDevice = devices[lastIndex];

                    }
                    if ((curDevice != null)) {

                        if (System.currentTimeMillis() - lastSwitchTime > EXPECTED_WORK_TIME) {

                            if (lastDevice != null) {
                                if (switchIndex()) curDevice.disconnect();
                            } else {
                                if (curDevice.getLatency() > EXPECTED_WORK_TIME)
                                    curDevice.disconnect();
                            }
                            lastSwitchTime = System.currentTimeMillis();
                        } else {
                            if (curDevice.isDisconnected()) {
                                curDevice.connect();
                            }else if((curDevice.isConnecting() || curDevice.isConnected()) && curDevice.getTimeInStatus() > CONNECTION_TIMEOUT)
                            {
                                Log.w(tag(), " connection timeout exceeded for " + curDevice.getAddress());
                                curDevice.disconnect();
                                switchIndex();
                                lastSwitchTime = System.currentTimeMillis();
                            } else if(curDevice.isTransmitting())
                            {
                                lastSwitchTime = curDevice.getStatusTime();
                            }
                        }
                    }
                    sleep(MIN_CHECK_PERIOD);
                }
            } catch (InterruptedException e) {
                Log.d(tag(), "interrupted");

            } finally {
                devices[0] = devices[1] = null;
                addresses[0] = addresses[1] = null;

            }
            Log.d(tag(), "loop for double stopped");

        }

        public void doStop() {
            isRunning = false;
            interrupt();
        }

        public boolean tryRemove(String address) {
            boolean ret = false;
            synchronized (syncObject) {
                if (addresses[0] != null && addresses[0].equalsIgnoreCase(address)) {
                    devices[0].disconnect();
                    addresses[0] = addresses[1];
                    devices[0] = devices[1];
                    addresses[1] = null;
                    devices[1] = null;
                    ret = true;
                }

                if (addresses[1] != null && addresses[1].equalsIgnoreCase(address)) {
                    devices[1].disconnect();
                    addresses[1] = null;
                    devices[1] = null;
                    ret = true;
                }
            }

            switchIndex();
            return ret;
        }

        public void tryTakeFromDouble(DoubleDevice source) {
            if (this.equals(source)) return;

            synchronized (syncObject) {
                if (addresses[0] != null) return;
                synchronized (source.syncObject) {
                    if(source.addresses[1] == null) return;
                    addresses[0] = source.addresses[1];
                    devices[0] = source.devices[1];
                    devices[0].deviceManagerId = id;
                    source.devices[1] = null;
                    source.addresses[1] = null;

                    Log.d(TAG, "device " + addresses[0] + " transferred from " + source.id + " to " +id);
                }
                source.switchIndex();
            }
            switchIndex();
        }

        public boolean isFull() {
            synchronized (syncObject) {
                return addresses[0] != null && addresses[1] != null;
            }
        }

        public boolean isEmpty() {
            synchronized (syncObject) {
                return addresses[0] == null && addresses[1] == null;
            }
        }
    }

    DoubleDevice[] doubles;

    public DeviceManagerDoublesMultiThread() {
        synchronized (doublesSyncObject) {
            doubles = new DoubleDevice[maxActiveDevices];
            for (int i = 0; i < doubles.length; i++) {
                doubles[i] = new DoubleDevice();
                doubles[i].id = i+1;
                doubles[i].start();
            }
        }
    }

    @Override
    public void run() {
        try {
            ArrayList<String> addressesToRemove = new ArrayList<>();
            DoubleDevice[] curDoubles;
            while (isRunning) {
                sleep(EXPECTED_WORK_TIME);
                if (isPaused) continue;
                synchronized (doublesSyncObject) {
                    curDoubles = Arrays.copyOf(doubles, doubles.length);// async protection
                }

                for (DoubleDevice doubleDevice : curDoubles) {
                    if (checkForExceedMaxLatency(doubleDevice.currentAddress()))
                        addressesToRemove.add(doubleDevice.currentAddress());
                    if (checkForExceedMaxLatency(doubleDevice.lastAddress()))
                        addressesToRemove.add(doubleDevice.lastAddress());
                }

                synchronized (doublesSyncObject) {
                    for (String address : addressesToRemove) {
                        removeDevice(address);
                    }
                }


                if (addressesToRemove.size() > 0) {
                    //rearrangeDoubles();
                    addressesToRemove.clear();
                }
            }
            Log.d(TAG + ":" + this.getClass().getSimpleName(), "finished cycling");
        } catch (InterruptedException e) {
            Log.d(TAG + ":" + this.getClass().getSimpleName(), "interrupted");

        } finally {
            isRunning = false;
            synchronized (doublesSyncObject) {
                for (DoubleDevice doubleDevice : doubles)
                    doubleDevice.doStop();
            }
        }

    }

    public boolean checkForExceedMaxLatency(String address) {
        return
                address != null &&
                        sensorDevices.get(address) != null &&
                        sensorDevices.get(address).getLatency() > MAX_LATENCY;
    }

    @Override
    public boolean addDevice(BluetoothDevice device, Context ctx, IDeviceEventListener
            listener) {
        String name = device.getName();
        if(sensorDevices.size() >= maxActiveDevices*2) return false;
        //if(sensorDevices.size() >= maxActiveDevices) return false;
        return !(name == null || !name.contains("Polar")) &&
                super.addDevice(device, ctx, listener) &&
                addAddressToDoubles(device.getAddress());
    }

    private boolean addAddressToDoubles(String address) {
        synchronized (doublesSyncObject) {
            for (DoubleDevice aDouble : doubles)
                if (aDouble.addFistDevice(address))
                    return true;

            for (DoubleDevice aDouble : doubles)
                if (aDouble.addSecondDevice(address))
                    return true;
        }
        return false;
    }

    private void rearrangeDoubles() {
        isPaused = true;
        if (maxActiveDevices <= 0) return;
        ArrayList<String> addressesToRemove = new ArrayList<>();
        synchronized (doublesSyncObject) {

            if (doubles != null) {
                for (DoubleDevice doubleDevice : doubles)
                    doubleDevice.doStop();
            }

            doubles = new DoubleDevice[maxActiveDevices];
            for (int i = 0; i < doubles.length; i++) {
                doubles[i] = new DoubleDevice();
                doubles[i].id = i+1;
                doubles[i].start();
            }

            for (String address : sensorDevices.keySet()) {
                if (!addAddressToDoubles(address))
                    addressesToRemove.add(address);
            }
            isPaused = false;

            for (String address : addressesToRemove)
                removeDevice(address);
        }


    }

    private void removeDevice(String address) {
        Log.d(TAG, "removing device " + address);
        if (sensorDevices.containsKey(address)) {
            sensorDevices.get(address).close();
            sensorDevices.remove(address);
        }

        synchronized (doublesSyncObject) {
            DoubleDevice fullDevice = null;
            DoubleDevice freeDevice = null;

            for (DoubleDevice doubleDevice : doubles) {

                doubleDevice.tryRemove(address);

                if (fullDevice == null && doubleDevice.isFull())
                    fullDevice = doubleDevice;
                if (freeDevice == null && doubleDevice.isEmpty())
                    freeDevice = doubleDevice;

                if (fullDevice != null && freeDevice != null) {
                    freeDevice.tryTakeFromDouble(fullDevice);
                    freeDevice = fullDevice = null;
                }
            }
        }
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

    @Override
    public void doStop() {
        super.doStop();
        for (DoubleDevice doubleDevice : doubles)
            doubleDevice.doStop();
    }
}
