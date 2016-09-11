package ru.test.multydevicetest.ui;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import ru.test.multydevicetest.DeviceService;

/**
 * Created by Bes on 17.08.2016.
 */
public class DeviceServiceConnection implements ServiceConnection {
    private final static String TAG = DeviceServiceConnection.class.getSimpleName();
    private final IDeviceServiceHolder serviceHolder;

    public DeviceServiceConnection(IDeviceServiceHolder serviceHolder)
    {
        this.serviceHolder = serviceHolder;
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder service) {
        Log.d(TAG, "connected to service");
        DeviceService deviceService = ((DeviceService.LocalBinder) service).getService();

        serviceHolder.updateDeviceService(deviceService);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        Log.d(TAG, "disconnected from service");
        serviceHolder.clearDeviceService();
    }
}
