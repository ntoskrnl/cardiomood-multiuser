package ru.test.multydevicetest.bluetooth;

import android.bluetooth.BluetoothGattCharacteristic;

/**
 * Created by Bes on 27.07.2016.
 */
public interface IBluetoothEventListener {
    void onRead(BluetoothGattWrapper gatt, BluetoothGattCharacteristic characteristic, int status);

    void onConnected(BluetoothGattWrapper gatt);

    void onDisconnected(BluetoothGattWrapper gatt);

    void onUnknownConnectionState(BluetoothGattWrapper gatt, int newState);

    void onServicesDiscovered(BluetoothGattWrapper gatt, int status);

    void onNotification(BluetoothGattWrapper gatt, BluetoothGattCharacteristic characteristic);
}
