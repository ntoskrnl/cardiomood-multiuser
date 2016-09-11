package ru.test.multydevicetest.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * This class is responsible for http connection to server,
 * sending messages to server and receiving response.
 * Created by Bes on 29.07.2016.
 */
public class HttpManager {

    private static final String TAG = HttpManager.class.getSimpleName();
    public static final int readTimeout = 30000;
    public static final int connectTimeout = 30000;

    public static String postJSONTextBlocking(URL url, String text) throws IOException {
        if(text == null || text.trim().length() <= 0) return "";

        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        try {
            //initializing connection
            urlConnection.setReadTimeout(readTimeout);
            urlConnection.setConnectTimeout(connectTimeout);
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoOutput(true);
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.connect();

            //sending json
            //Log.d(TAG, "Posting to \"" + url + "\" bytes  " + text.length());
            OutputStreamWriter out = new OutputStreamWriter(urlConnection.getOutputStream());
            out.write(text);
            out.close();

            //receiving response

            int httpResult = urlConnection.getResponseCode();
            if (httpResult == HttpURLConnection.HTTP_OK) {
                StringBuilder sbResponse = new StringBuilder();
                BufferedReader br = new BufferedReader(new InputStreamReader(
                        urlConnection.getInputStream(), "utf-8"));
                String line;
                while ((line = br.readLine()) != null) {
                    sbResponse.append(line);
                    sbResponse.append(System.getProperty("line.separator"));
                }
                br.close();

                String answer = sbResponse.toString();
                //Log.d(TAG, "Got from \"" + url + "\": " + answer);
                return answer;

            } else {
                throw new IOException("Server refused request with code: " + httpResult + " and message: " + urlConnection.getResponseMessage());
            }

        } catch (Exception e){
            String cause = (e.getLocalizedMessage() == null ? e.getClass().getSimpleName() :e.getLocalizedMessage());
            //Log.e(TAG,"Error in http communication: " + cause);
            throw new IOException("Error in http communication",e);
        }finally {
            urlConnection.disconnect();
        }
    }
}