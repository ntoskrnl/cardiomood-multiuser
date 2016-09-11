package ru.test.multydevicetest.ui;

import ru.test.multydevicetest.DeviceService;

/**
 * Created by Bes on 17.08.2016.
 */
public interface IDeviceServiceHolder {

    void updateDeviceService(DeviceService deviceService);
    void clearDeviceService();
}
