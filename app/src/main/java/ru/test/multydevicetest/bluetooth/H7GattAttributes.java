package ru.test.multydevicetest.bluetooth;

import java.util.HashMap;

/**
 * Created by Bes on 27.06.2016.
 */
public class H7GattAttributes {

    private static HashMap<String, String> attributes = new HashMap<>();
    public static String HEART_RATE_CHARACTERISTIC = "00002a37-0000-1000-8000-00805f9b34fb";
    public static String BATTERY_CHARACTERISTIC = "00002a19-0000-1000-8000-00805f9b34fb";
    public static String HEART_RATE_SERVICE = "0000180d-0000-1000-8000-00805f9b34fb";
    public static String BATTERY_SERVICE = "0000180f-0000-1000-8000-00805f9b34fb";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    static {
        // Sample Services.
        attributes.put(HEART_RATE_SERVICE, "Heart Rate Service");
        attributes.put(BATTERY_SERVICE, "Battery Service");
        attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "Device Information Service");
        // Sample Characteristics.
        attributes.put(HEART_RATE_CHARACTERISTIC, "Heart Rate Measurement");
        attributes.put(BATTERY_CHARACTERISTIC, "Battery Level");
        attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
