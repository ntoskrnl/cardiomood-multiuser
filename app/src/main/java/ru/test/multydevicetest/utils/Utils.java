package ru.test.multydevicetest.utils;

import android.util.Log;

/**
 * Created by Bes on 09.08.2016.
 */
public class Utils {

    public static Thread execInNewThreadWithDelay(final Runnable runnable, final long sleepTimeMS)
    {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(sleepTimeMS);
                } catch (InterruptedException e) {
                    return;
                }
                try {
                    runnable.run();
                } catch (Throwable e)
                {
                    Log.w("New Thread Executor","Abnormal execution termination : ",e);
                }
            }
        });
        thread.start();
        return thread;
    }

    public static Thread execInNewThread(final Runnable runnable)
    {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } catch (Throwable e)
                {
                    Log.w("New Thread Executor","Abnormal execution termination : ",e);
                }
            }
        });
        thread.start();
        return thread;
    }

}
