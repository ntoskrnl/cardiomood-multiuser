package ru.test.multydevicetest.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.util.Log;

import java.util.UUID;

import ru.test.multydevicetest.utils.Utils;

/**
 * Created by Bes on 27.06.2016.
 */
public class SensorDeviceClassic extends SensorDevice {

    private String TAG = SensorDeviceClassic.class.getSimpleName();
    @Override
    protected String tag() {
        return this.TAG + ":" + address + " " + getStatus();
    }

    private static final long UNSUCCESSFUL_CONNECTION_TIME_OUT = 2500;

    protected BluetoothManager mBluetoothManager;
    protected BluetoothAdapter mBluetoothAdapter;
    protected BluetoothGattWrapper mBluetoothGatt;


    private long lastUnsuccessfulConnectionTime = 0;

    public SensorDeviceClassic(Context ctx, BluetoothDevice device, IDeviceEventListener listener) {
        super(ctx,device,listener);

        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) ctx.getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(tag(), "Unable to initialize BluetoothManager.");
                return;
            }
        }
        tryGetBlueToothAdapter();
    }

    private boolean tryGetBlueToothAdapter() {
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        mBluetoothGatt = null;
        if (mBluetoothAdapter == null) {
            Log.e(tag(), "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        return true;
    }

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final IBluetoothEventListener mGattCallback = new IBluetoothEventListener() {

        @Override
        public void onConnected(BluetoothGattWrapper gatt) {
            Log.i(tag(), "Connected to GATT server.");
            connectionState.setStatus(Status.CONNECTED);

            listener.onAction(address, ACTION_GATT_CONNECTED);
            // Attempts to discover services after successful connection.
            //if (heartRateCharacteristic == null)
            Utils.execInNewThread(new Runnable() {
                @Override
                public void run() {
                    boolean isDiscoveryStarted = mBluetoothGatt.discoverServices();
                    Log.i(tag(), "Attempting to start service discovery: " + isDiscoveryStarted);
                    if (!isDiscoveryStarted)
                        //onConnected(mBluetoothGatt);
                        disconnect();
                }
            });

            isConnected = true;
            lastChangeConnectionTime = System.currentTimeMillis();
        }

        @Override
        public void onDisconnected(BluetoothGattWrapper gatt) {
            Log.i(tag(), "Disconnected from GATT server.");

            if (!connectionState.isInStatus(Status.DISCONNECTING)) {
                Log.i(tag(), "unexpected disconnection");
                lastUnsuccessfulConnectionTime = System.currentTimeMillis();
            }

            connectionState.setStatus(Status.CLOSING);
            Utils.execInNewThreadWithDelay(new Runnable() {
                                               @Override
                                               public void run() {
                                                   connectionState.setStatus(Status.DISCONNECTED);
                                                   isConnected = false;
                                                   lastChangeConnectionTime = System.currentTimeMillis();
                                                   close();
                                                   listener.onAction(address, ACTION_GATT_DISCONNECTED);
                                               }
                                           },
                    500);
        }

        @Override
        public void onUnknownConnectionState(BluetoothGattWrapper gatt, int newState) {
            Log.w(tag(), "Unknown state " + newState + "!");
        }


        @Override
        public void onServicesDiscovered(BluetoothGattWrapper gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                for (BluetoothGattService service : mBluetoothGatt.getServices()) {
                    if (HEART_RATE_SERVICE.equals(service.getUuid())) {
                        Log.d(tag(), "Found heart rate service");
                        // Loops through available Characteristics.
                        for (final BluetoothGattCharacteristic gattCharacteristic : service.getCharacteristics()) {
                            if (!HEART_RATE_CHARACTERISTIC.equals(gattCharacteristic.getUuid()))
                                continue;
                            Log.d(tag(), "Found heart rate measurement");
                            heartRateCharacteristic = gattCharacteristic;
                            Utils.execInNewThread(new Runnable() {
                                @Override
                                public void run() {
                                    setCharacteristicNotification(gattCharacteristic, true);
                                }
                            });

                        }
                    } else if (BATTERY_SERVICE.equals(service.getUuid())) {
                        //Log.d(tag(),"Found heart rate service");
                        // Loops through available Characteristics.
                        for (BluetoothGattCharacteristic gattCharacteristic : service.getCharacteristics()) {
                            if (!BATTERY_CHARACTERISTIC.equals(gattCharacteristic.getUuid()))
                                continue;
                            Log.d(tag(), "Found battery level");
                            batteryLevelCharacteristic = gattCharacteristic;
                            Utils.execInNewThreadWithDelay(new Runnable() {
                                                               @Override
                                                               public void run() {
                                                                   //readBatteryLevel();
                                                               }
                                                           },
                                    1000);

                        }
                    }
                }

                if (heartRateCharacteristic == null) {
                    Log.d(tag(), "This device could not measure heart rate");
                    //connectionState.setStatus(Status.UNSUPPORTED);
                    //listener.onAction(address, ACTION_GATT_SERVICES_UNSUPPORTED);

                    Utils.execInNewThread(new Runnable() {
                        @Override
                        public void run() {
                           /* boolean isDiscoveryStarted = mBluetoothGatt.discoverServices();
                            Log.i(tag(), "Attempting to start service discovery AGAIN: " + isDiscoveryStarted);
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                return;
                            }
                            if(!isDiscoveryStarted)
                                onConnected(mBluetoothGatt);*/
                            disconnect();

                        }
                    });
                }
                if (batteryLevelCharacteristic == null) {
                    Log.d(tag(), "This device could not measure battery Level");
                }

            } else {
                Log.w(tag(), "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onRead(BluetoothGattWrapper gatt,
                           BluetoothGattCharacteristic characteristic,
                           int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(characteristic);
            }
        }

        @Override
        public void onNotification(BluetoothGattWrapper gatt,
                                   BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(characteristic);
        }
    };


    private void broadcastUpdate(final BluetoothGattCharacteristic characteristic) {
        //Log.d(tag(), "Characteristic update " +action +" : "+ characteristic.getUuid());
        if (HEART_RATE_CHARACTERISTIC.equals(characteristic.getUuid())) {

            int flag = characteristic.getProperties();
            int format;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
                //Log.d(tag(), "Heart rate format UINT16.");
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                //Log.d(tag(), "Heart rate format UINT8.");
            }
            final int heartRate = characteristic.getIntValue(format, 1);
            //Log.d(tag(), String.format("Received heart rate: %d", heartRate));
            lastHeartRate = heartRate;

            super.onDataReceived();

            listener.onHeartRate(address, heartRate, getLatency());

            if (connectionState.isInStatus(Status.CONNECTED))
                connectionState.setStatus(Status.TRANSMITTING);

        } else if (BATTERY_CHARACTERISTIC.equals(characteristic.getUuid())) {
            lastBatteryLevel = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            listener.onBatteryLevel(address, lastBatteryLevel);
        }
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    @Override
    public boolean connect() {
        if (keepClosed) return false;
        if (mBluetoothAdapter == null) {
            Log.w(tag(), "BluetoothAdapter not initialized in connect");
            if (!tryGetBlueToothAdapter()) return false;
        }

        if (address == null) {
            Log.w(tag(), "Unspecified address");
            return false;
        }

        if (!connectionState.isInStatus(Status.DISCONNECTED)) {
            Log.d(tag(), "will not connect in wrong status " + getStatus());
            return false;
        }

        if (System.currentTimeMillis() - lastUnsuccessfulConnectionTime < UNSUCCESSFUL_CONNECTION_TIME_OUT) {
            Log.d(tag(), "will not connect: unsuccessful connection cool down in progress " + (System.currentTimeMillis() - lastUnsuccessfulConnectionTime));
            return false;
        }
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        try {
            mBluetoothGatt = BluetoothGattWrapper.connectDevice(ctx, address, mGattCallback);
            if (mBluetoothGatt == null) {
                Log.w(tag(), "unable to connect");
                return false;
            }
            //Log.d(tag(), "Trying to create a new connection.");
        } catch (Exception e) {
            Log.w(tag(), "Error while connecting", e);
            close(false);
            return false;
        }
        connectionState.setStatus(Status.CONNECTING);
        activationTime = System.currentTimeMillis();
        Log.i(tag(), "Direct connected");
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    @Override
    public void disconnect() {
        if (mBluetoothAdapter == null) {
            Log.w(tag(), "BluetoothAdapter not initialized in disconnect");
            if (!tryGetBlueToothAdapter()) return;
        }

        if (connectionState.isInStatus(Status.CLOSING, Status.DISCONNECTED)) {
            Log.d(tag(), "will not disconnect in wrong status " + getStatus());
            return;
        }

        if (mBluetoothGatt == null
                //|| connectionState.isInStatus(Status.CONNECTING)
                ) {
            Log.w(tag(), "Device is not connected while disconnecting  - closing");
            close();
        } else {
            try {
                if (!connectionState.isInStatus(Status.DISCONNECTED))
                    connectionState.setStatus(Status.DISCONNECTING);
                mBluetoothGatt.disconnect();
            } catch (Exception e) {
                Log.w(tag(), "Error while disconnecting", e);
                close();
            }
        }
        Log.i(tag(), "Disconnecting");
    }


    @Override
    public void close() {
        close(false);
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    @Override
    public void close(boolean keepClosed) {
        if (mBluetoothGatt != null) {
            try {
                mBluetoothGatt.close();
            } catch (Exception e) {
                Log.w(tag(), "Error while closing", e);

            } finally {
                mBluetoothGatt = null;
            }

        }
        this.keepClosed = keepClosed;
        connectionState.setStatus(Status.DISCONNECTED);
        //heartRateCharacteristic = null;
        //batteryLevelCharacteristic = null;
        Log.i(tag(), "Closed");
    }

    @Override
    public void reset() {
        tryGetBlueToothAdapter();
        keepClosed = false;
        mBluetoothGatt = null;
        connectionState.setStatus(Status.DISCONNECTED);
        activationTime = System.currentTimeMillis();
        heartRateCharacteristic = null;
        batteryLevelCharacteristic = null;
        lastDataTime = -1;
        lastLatency = 0;
        lastHeartRate = 0;
        Log.i(tag(), "Reset");
    }

    private void readDeviceCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null) {
            Log.w(tag(), "BluetoothAdapter not initialized");
            if (!tryGetBlueToothAdapter()) return;
            return;
        }

        if (characteristic == null) {
            return;
        }

        if (mBluetoothGatt == null)
            Log.w(tag(), "BluetoothGatt not initialized");
        else
            mBluetoothGatt.readCharacteristic(characteristic);
    }

    public void readHeartRate() {
        try {
            readDeviceCharacteristic(heartRateCharacteristic);
        } catch (Exception e) {
            Log.w(tag(), "Error while reading heart rate", e);
        }
    }


    public void readBatteryLevel() {
        if (!connectionState.isInStatus(Status.TRANSMITTING)) {
            //lastBatteryLevel = -1;
            return;
        }
        if (lastBatteryLevel < 0) {
            lastBatteryLevel = 0;
            try {

                //readDeviceCharacteristic(batteryLevelCharacteristic);
            } catch (Exception e) {
                Log.w(tag(), "Error while reading battery level", e);
            }
        }

    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(final BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        {
            if (mBluetoothAdapter == null) {
                Log.w(tag(), "BluetoothAdapter not initialized");
                if (!tryGetBlueToothAdapter()) return;

            }
            if (mBluetoothGatt == null) {
                Log.w(tag(), "BluetoothGatt not initialized");
                return;
            }

            Log.d(tag(), "Setting notifications of characteristic " + characteristic.getUuid().toString() + " enabled " + enabled);
            mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

            // This is specific to Heart Rate Measurement.
            if (enabled && HEART_RATE_CHARACTERISTIC.equals(characteristic.getUuid())) {
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                        UUID.fromString(H7GattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                if (mBluetoothAdapter != null)
                    mBluetoothGatt.writeDescriptor(descriptor);
                else
                    tryGetBlueToothAdapter();
            }
        }
    }
}
