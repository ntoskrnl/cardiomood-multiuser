package ru.test.multydevicetest.ui;

import ru.test.multydevicetest.bluetooth.SensorDevice;

/**
 * Created by Bes on 17.08.2016.
 */
public interface IDeviceProvider {
    SensorDevice getDevice(String address);
}
