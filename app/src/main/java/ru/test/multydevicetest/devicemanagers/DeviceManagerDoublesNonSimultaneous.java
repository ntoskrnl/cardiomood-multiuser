package ru.test.multydevicetest.devicemanagers;

import android.util.Log;

/**
 * Created by Bes on 25.07.2016.
 */
public class DeviceManagerDoublesNonSimultaneous extends DeviceManagerDoubles{

    public final int MIN_CHECK_PERIOD = 1000;


    private final static String TAG = DeviceManagerDoublesNonSimultaneous.class.getSimpleName();
    @Override
    public void run() {
        try {
            while (isRunning) {
                if(doubles == null) {
                    sleep(MIN_CHECK_PERIOD);
                    continue;
                }

                for (int i = 0 ; i < doubles.length ; i++) {
                    switchDeviceIfNeeded(doubles[i]);
                    sleep(MIN_CHECK_PERIOD/doubles.length);
                }
            }
            Log.d(TAG + ":" + this.getClass().getSimpleName(), "finished cycling");
        } catch (InterruptedException e) {
            Log.d(TAG + ":" + this.getClass().getSimpleName(), "interrupted");
            isRunning = false;
        }

    }

}
