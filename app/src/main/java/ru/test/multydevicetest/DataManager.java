package ru.test.multydevicetest;

import android.util.Log;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.LinkedBlockingDeque;

import ru.test.multydevicetest.network.HttpManager;

public class DataManager {

    public static final String TAG = DataManager.class.getSimpleName();
    public static final int MAX_QUEUE_SIZE = 2000;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);

    public class DataRecord{

        private String macAddress;
        private Date timeStamp;
        private int pulse;

        public DataRecord(String macAddress,Date timeStamp, int pulse)
        {
            this.macAddress = macAddress == null ? "NONE" : macAddress;
            this.timeStamp = timeStamp;
            this.pulse = pulse;
        }

        @Override
        public String toString()
        {
            return
                    macAddress + " ; " +
                    dateFormat.format(timeStamp)+ " ; " +
                    timeStamp.getTime() + " ; " +
                    pulse;
        }

        public String toJSONString() {
            return "{ data : \""+toString()+"\" }";
        }
    }

    private class SendOutLooper extends Thread
    {
        @Override
        public void run() {
            while (isActive)
            {
                try {
                    sleep(15000);
                } catch (InterruptedException e)
                {
                    Log.w(TAG, "Looper was interrupted");
                    if(isActive)
                        dispose();
                    return;
                }

                                /*
                int failedCount = 0;
                int successCount = 0;
                DataRecord record;
                long startTime= System.currentTimeMillis();

                while(!messageQueue.isEmpty() && (System.currentTimeMillis() - startTime) < 5000) {
                    record = messageQueue.poll();
                    try {

                        String response = HttpManager.postJSONTextBlocking(serverUrl, record.toJSONString());

                        if (response != null && !response.trim().contains("OK"))
                        {
                            successCount++;
                            continue;
                        }
                    } catch (IOException e) {
                        //Log.e(TAG, "Exception while posting data to server", e);
                    }

                    failedCount++;
                    messageQueue.offerFirst(record);
                }*/

                ArrayList<DataRecord> tmpBuffer = new ArrayList<>();
                StringBuilder jsonText = new StringBuilder();
                jsonText.append("{ data : \"");
                long startTime= System.currentTimeMillis();
                DataRecord record;
                int recordCount = 0;

                while(!messageQueue.isEmpty() && (System.currentTimeMillis() - startTime) < 5000) {
                    record = messageQueue.poll();
                    if(recordCount > 0) {
                        jsonText.append('\n');
                        jsonText.append('\r');
                    }
                    jsonText.append(record.toString());
                    tmpBuffer.add(record);
                    recordCount++;
                }
                jsonText.append("\" }");

                if(recordCount <=0 ) continue;

                String response = "";
                try {

                    response = HttpManager.postJSONTextBlocking(serverUrl, jsonText.toString());

                    if (response != null && !response.trim().contains("OK"))
                    {
                        Log.d(TAG, "Sent to server " + recordCount + " records");
                        continue;
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Exception while posting data to server", e);

                }

                Log.d(TAG, "Failed to send to server " + recordCount + " records. Return code is \""  +response +"\"");

                for(int i = tmpBuffer.size() -1 ; i >=0 ; i--)
                    if(!messageQueue.offerFirst(tmpBuffer.get(i)))
                        break;
            }
            isActive = false;
        }
    }

    private LinkedBlockingDeque<DataRecord> messageQueue = new LinkedBlockingDeque<>(MAX_QUEUE_SIZE);
    private URL serverUrl;
    private boolean isActive;
    private SendOutLooper looper;

    public DataManager(String serverAddress)
    {
        try {
            this.serverUrl = new URL(serverAddress);
        } catch (MalformedURLException e) {
            Log.e(TAG, "Wrong URL! " + e.getLocalizedMessage());
            isActive = false;
            return;
        }

        isActive = true;
        looper = new SendOutLooper();
        looper.start();
    }

    public void addRecord(String macAddress,Date timeStamp, int pulse)
    {
        if(!isActive) return;
        while(!messageQueue.offer(new DataRecord(macAddress,timeStamp,pulse)))
        {
            messageQueue.poll();
        }
    }

    public void dispose(){
        Log.d(TAG,"disposing");
        isActive = false;
        looper.interrupt();
        messageQueue.clear();
    }
}
