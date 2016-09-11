package ru.test.multydevicetest.devicemanagers;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import ru.test.multydevicetest.DeviceService;
import ru.test.multydevicetest.bluetooth.HeartRateListener;
import ru.test.multydevicetest.bluetooth.IDeviceEventListener;
import ru.test.multydevicetest.bluetooth.SensorDevice;

/**
 * Created by Bes on 24.07.2016.
 */
public class DeviceManager extends AbstractDeviceManager {
    private final static String TAG = DeviceService.class.getSimpleName();

    public final int MAX_ACTIVE_DEVICES = 100;
    public final int MIN_CHECK_PERIOD = 1000;
    public final int EXPECTED_WORK_TIME = 14000;

    protected ConcurrentHashMap<String,SensorDevice> sensorDevices = new ConcurrentHashMap<>();

    protected boolean isRunning = true;

    protected int maxActiveDevices = 7;

    private HeartRateListener heartRateListener = null;

    /**
     * Calls the <code>run()</code> method of the Runnable object the receiver
     * holds. If no Runnable is set, does nothing.
     *
     * @see Thread#start
     */

    public DeviceManager(){
    }

    @Override
    public void run() {
        super.run();

        int curPos = 0;
        int sleepTime = MIN_CHECK_PERIOD;
        try {
            while (isRunning) {
                //if(countActiveDevices() >= maxActiveDevices)
                //Log.d(TAG, "Waiting " + sleepTime+ "ms");
                sleep(sleepTime);
                ArrayList<String> addresses = new ArrayList<>(sensorDevices.keySet());
                if(addresses.size() <= 0)
                {
                    sleepTime = MIN_CHECK_PERIOD;
                    continue;
                }
                sleepTime = EXPECTED_WORK_TIME / maxActiveDevices;
                sleepTime = sleepTime < MIN_CHECK_PERIOD ? MIN_CHECK_PERIOD : sleepTime;

                Log.d(TAG, "Active devices : " + countActiveDevices());
                if(countActiveDevices() < maxActiveDevices && countActiveDevices() < addresses.size())
                    for(int countDown = addresses.size(); countDown > 0 ; countDown--) {
                        curPos++;
                        if (curPos >= addresses.size()) curPos = 0;
                        if(checkIfActiveDevice(addresses.get(curPos))) {
                            Log.d(TAG, "Device N "+curPos+ " " + addresses.get(curPos) +" is already active");
                            continue;
                        }
                        Log.d(TAG, "Activating device N "+curPos+ " " + addresses.get(curPos));
                        activateDevice(addresses.get(curPos));
                        break;
                    }

                //if(countActiveDevices() < maxActiveDevices)
                //    connectTheOldestDevice();

                if(countActiveDevices() >= maxActiveDevices && addresses.size() > maxActiveDevices)
                    disconnectTheOldestDevice();
            }
        } catch (InterruptedException e)
        {
            Log.d(TAG+":"+this.getClass().getSimpleName(),"interrupted");
            isRunning = false;
        }
    }

    @Override
    public long getMaxLatency() {
        long largestLatency = 0;
        for(SensorDevice device : sensorDevices.values())
            if( device.getLatency() > largestLatency)
                largestLatency = device.getLatency();

        return largestLatency;
    }

    private boolean checkIfActiveDevice(String address) {
        if (!sensorDevices.containsKey(address)) return true; //чтоб пропустить цикл
        SensorDevice device = sensorDevices.get(address);
        return device == null || device.isConnected() || device.isConnecting() || device.isTransmitting();
    }

    private boolean activateDevice(String address) {
        if (!sensorDevices.containsKey(address)) return false;
        SensorDevice device = sensorDevices.get(address);
        if(!device.isDisconnected()) device.close();
        if(device.isConnected()|| device.isTransmitting()) return false;
        return device.connect();
    }


    private boolean connectTheOldestDevice() {
        String oldestAddress = null;
        long oldestTime = Calendar.getInstance().getTimeInMillis()*2;
        for(SensorDevice device : sensorDevices.values())
            if(!device.isConnected() && device.getActivationTime() < oldestTime)
            {
                oldestTime = device.getActivationTime();
                oldestAddress = device.getAddress();
            }

        if(oldestAddress == null) return false;
        Log.d(TAG, "connecting the oldest working device: "+oldestAddress + " in state " + sensorDevices.get(oldestAddress).getStatus());
        sensorDevices.get(oldestAddress).connect();
        return true;
    }

    private boolean disconnectTheOldestDevice() {
        String oldestAddress = null;
        long curTime;
        long oldestTime = curTime = Calendar.getInstance().getTimeInMillis();
        for(SensorDevice device : sensorDevices.values())
            if(device.isTransmitting() && device.getActivationTime() < oldestTime)
            {
                oldestTime = device.getActivationTime();
                oldestAddress = device.getAddress();
            }

        if(oldestAddress == null) return false;
        if(curTime - oldestTime < EXPECTED_WORK_TIME) return false;
        Log.d(TAG, "disconnecting the oldest device: "+oldestAddress + " in state " + sensorDevices.get(oldestAddress).getStatus() + " last activation is " +(curTime - oldestTime) + "ms ago");
        sensorDevices.get(oldestAddress).disconnect();
        return true;
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
        if(sensorDevices.containsKey(device.getAddress())) return false;
        SensorDevice sensor = SensorDevice.newInstance(ctx,device,listener);
        sensor.setHeartRateListener(heartRateListener);
        sensorDevices.put(device.getAddress(), sensor);
        sensorDevices.get(device.getAddress()).deviceManagerId = sensorDevices.size() - 1;
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
        return maxActiveDevices;
    }

    public void setHeartRateListener(HeartRateListener listener) {
        heartRateListener = listener;
        for (SensorDevice sensor: allDevices())
            sensor.setHeartRateListener(heartRateListener);
    }
}
