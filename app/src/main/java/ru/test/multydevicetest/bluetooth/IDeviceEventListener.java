package ru.test.multydevicetest.bluetooth;

/**
 * Created by Bes on 27.06.2016.
 */
public interface IDeviceEventListener {

    void onAction(String address, String action);

    void onHeartRate(String address, int heartRate, long latency);

    void onBatteryLevel(final String address, final int batteryLevel);
}
