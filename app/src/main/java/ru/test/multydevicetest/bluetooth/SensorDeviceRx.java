package ru.test.multydevicetest.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import com.polidea.rxandroidble.RxBleClient;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.internal.RxBleLog;
import com.polidea.rxandroidble.utils.ConnectionSharingAdapter;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.PublishSubject;

/**
 * Created by Bes on 18.08.2016.
 */
public class SensorDeviceRx extends SensorDevice {

    protected String TAG = SensorDeviceRx.class.getSimpleName();

    private static RxBleClient bleClient = null;
    private final RxBleDevice device;
    private PublishSubject<Void> disconnectTriggerSubject = PublishSubject.create();
    private Observable<RxBleConnection> connectionObservable;
    private RxBleConnection rxBleConnection;

    protected SensorDeviceRx(Context ctx, BluetoothDevice device, IDeviceEventListener listener) {
        super(ctx, device, listener);
        if(bleClient == null)
        {
            bleClient = RxBleClient.create(ctx.getApplicationContext());
            RxBleClient.setLogLevel(RxBleLog.WARN);
        }
        this.device = bleClient.getBleDevice(device.getAddress());
        connectionObservable = this.device
                .establishConnection(ctx, true)
                .takeUntil(disconnectTriggerSubject)
                .doOnUnsubscribe(this::close)
                .compose(new ConnectionSharingAdapter());
        connectionState.setStatus(Status.DISCONNECTED);
    }


    @Override
    protected String tag() {
        return TAG + ":"+ device.getName() +"(" + device.getMacAddress() + "):" + getStatus();
    }

    @Override
    public boolean connect() {
        if(keepClosed) return false;
        if(!isDisconnected()) {
            Log.d(tag(), "will not connect in current state ");
            return false;
        }
        this.connectionState.setStatus(Status.CONNECTING);
        this.activationTime = getTime();
        connectionObservable.subscribe(this::onConnectionReceived, this::onConnectionFailure);
        return true;
    }

    private void onConnectionFailure(Throwable throwable) {
        Log.w(tag(),"Connection failed",throwable);
        this.connectionState.setStatus(Status.DISCONNECTED);
    }

    private void onConnectionReceived(RxBleConnection connection) {
        Log.d(tag(),"Connection succeeded");
        this.connectionState.setStatus(Status.CONNECTED);
        this.lastChangeConnectionTime = getTime();
        this.isConnected = true;
        connectionObservable
                .flatMap(rxBleConnection -> rxBleConnection.setupNotification(HEART_RATE_CHARACTERISTIC))
                .doOnNext(notificationObservable -> notificationHasBeenSetUp())
                .flatMap(notificationObservable -> notificationObservable)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onNotificationReceived, this::onNotificationSetupFailure);
    }

    private void onNotificationSetupFailure(Throwable throwable) {
        Log.w(tag(),"notification setup failure",throwable);
    }

    private void onNotificationReceived(byte[] bytes) {
        //Log.d(tag(), "received notification: " + HexString.bytesToHex(bytes));
        if(!connectionState.isInStatus(Status.TRANSMITTING))
            connectionState.setStatus(Status.TRANSMITTING);
        this.lastHeartRate = parseHeartRate(bytes);

        super.onDataReceived();
    }

    private void notificationHasBeenSetUp()
    {
        Log.d(tag(), "notification has been set up!");
    }

    /**
     * Convert a signed byte to an unsigned int.
     */
    private int unsignedByteToInt(byte b) {
        return b & 0xFF;
    }
    /**
     * Convert signed bytes to a 16-bit unsigned int.
     */
    private int unsignedBytesToInt(byte b0, byte b1) {
        return (unsignedByteToInt(b0) + (unsignedByteToInt(b1) << 8));
    }

    private int parseHeartRate(byte[] bytes)
    {
        if((bytes[0] & 0x01) != 0)
        {
            return unsignedBytesToInt(bytes[1],bytes[2]);
            //Log.d(tag(), "Heart rate format UINT16.");
        }else {
            return unsignedByteToInt(bytes[1]);
            //Log.d(tag(), "Heart rate format UINT8.");
        }
    }

    @Override
    public void disconnect() {
        connectionState.setStatus(Status.DISCONNECTING);
        disconnectTriggerSubject.onNext(null);
    }

    @Override
    public void close(boolean keepClosed) {
        this.keepClosed = keepClosed;
        connectionState.setStatus(Status.DISCONNECTED);
        lastChangeConnectionTime = getTime();
        isConnected = false;
    }

    @Override
    public void reset() {
        keepClosed = false;
        disconnectTriggerSubject.onNext(null);
        connectionState.setStatus(Status.DISCONNECTED);
        activationTime = getTime();
        lastDataTime = -1;
        lastLatency = 0;
        lastHeartRate = 0;
        Log.i(tag(), "Reset");
    }
}
