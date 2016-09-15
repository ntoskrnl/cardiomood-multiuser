package ru.test.multydevicetest;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by Bes on 10.08.2016.
 */
public class AlarmReceiver extends BroadcastReceiver {

    public static final String TAG = AlarmReceiver.class.getSimpleName();
    private static final long ALARM_INTERVAL = 30 * 1000;

    @Override
    public void onReceive(Context context, Intent intent) {

        if(DeviceService.Companion.shouldRun()) {

            if (!DeviceService.Companion.isRunning(context)) {
                Log.w(TAG, "Service is not running while it should! Will try to restart");
                Intent startIntent = new Intent(context, DeviceService.class);
                startIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
                context.startService(startIntent);
            }

            startAlarm(context);
        }
    }


    public static void startAlarm(Context ctx) {
        AlarmManager alarmManager = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.w(TAG,"alarm manager is not initialized!");
            return;
        }
        //ProxyLogger.instance().debug("Setting alarm to check service in " + ALARM_INTERVAL / 1000 + " seconds");
        PendingIntent alarmIntent = PendingIntent.getBroadcast(ctx, 0, new Intent(ctx, AlarmReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + ALARM_INTERVAL, alarmIntent);
    }
}
