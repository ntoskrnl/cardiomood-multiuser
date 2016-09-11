package ru.test.multydevicetest.devicemanagers;

import android.util.Log;

/**
 * Created by Bes on 25.07.2016.
 */
public class DeviceManagerDoublePolarNoWait extends DeviceManagerDoublePolar {

    private final static String TAG = DeviceManagerDoublePolarNoWait.class.getSimpleName();

    public final int MIN_CHECK_PERIOD = 1000;
    public final int EXPECTED_WORK_TIME = 5000;

    @Override
    public void run() {

        int index = 0;
        int lastIndex = 1;
        try {
            while (isRunning) {
                if(devices[index] != null && devices[lastIndex] != null) {
                    if (devices[index].isDisconnected() && devices[lastIndex].isDisconnected()) {
                        devices[index].connect();
                    }
                    else if (devices[index].isTransmitting() && devices[index].getLastConnectionStatusTime() > EXPECTED_WORK_TIME) {
                        devices[index].disconnect();
                        devices[lastIndex].connect();
                        lastIndex = index++;
                        if (index > 1) index = 0;
                    }
                }
                sleep(MIN_CHECK_PERIOD);
            }
        } catch (InterruptedException e)
        {
            Log.d(TAG+":"+this.getClass().getSimpleName(),"interrupted");
            isRunning = false;
        }
    }
}
