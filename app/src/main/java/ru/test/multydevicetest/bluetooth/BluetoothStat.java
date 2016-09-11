package ru.test.multydevicetest.bluetooth;

/**
 * Created by Bes on 09.08.2016.
 */
public class BluetoothStat //implements Serializable
{
    protected final Object statSyncObject = new Object();
    public volatile long connectionsTotal = 0;
    public volatile long connectionSuccess = 0;
    public volatile long disconnectionTotal = 0;
    public volatile long disconnectionSuccess = 0;
    public volatile long discoverTotal = 0;
    public volatile long discoverSuccess = 0;
    public volatile long notificationTotal = 0;
    public volatile long notificationSuccess = 0;

    public void reset()
    {
        connectionsTotal = 0;
        connectionSuccess = 0;
        disconnectionTotal = 0;
        disconnectionSuccess = 0;
        discoverTotal = 0;
        discoverSuccess = 0;
        notificationTotal = 0;
        notificationSuccess = 0;
    }

    public BluetoothStat()
    {
        reset();
    }

    public  BluetoothStat(BluetoothStat source)
    {
        connectionsTotal = source.connectionsTotal;
        connectionSuccess = source.connectionSuccess;
        disconnectionTotal = source.disconnectionTotal;
        disconnectionSuccess = source.disconnectionSuccess;
        discoverTotal = source.discoverTotal;
        discoverSuccess = source.discoverSuccess;
        notificationTotal = source.notificationTotal;
        notificationSuccess = source.notificationSuccess;
    }
}
