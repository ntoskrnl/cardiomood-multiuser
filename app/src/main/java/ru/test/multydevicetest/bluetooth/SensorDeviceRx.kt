package ru.test.multydevicetest.bluetooth

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log

import com.polidea.rxandroidble.RxBleClient
import com.polidea.rxandroidble.RxBleConnection
import com.polidea.rxandroidble.RxBleDevice
import com.polidea.rxandroidble.internal.RxBleLog
import com.polidea.rxandroidble.utils.ConnectionSharingAdapter

import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.subjects.PublishSubject

class SensorDeviceRx protected constructor(ctx: Context, device: BluetoothDevice, listener: IDeviceEventListener) : SensorDevice(ctx, device, listener) {

    protected var TAG = SensorDeviceRx::class.java.simpleName
    private val device: RxBleDevice
    private val disconnectTriggerSubject = PublishSubject.create<Void>()
    private val connectionObservable: Observable<RxBleConnection>
    private val rxBleConnection: RxBleConnection? = null

    init {
        if (bleClient == null) {
            bleClient = RxBleClient.create(ctx.applicationContext)
            RxBleClient.setLogLevel(RxBleLog.WARN)
        }
        this.device = bleClient!!.getBleDevice(device.address)
        connectionObservable = this.device.establishConnection(ctx, true)
                .takeUntil(disconnectTriggerSubject)
                .doOnUnsubscribe({ this.close() })
                .compose(ConnectionSharingAdapter())

        connectionState.setStatus(Status.DISCONNECTED)
    }


    override fun tag(): String {
        return TAG + ":" + device.name + "(" + device.macAddress + "):" + status
    }

    override fun connect(): Boolean {
        if (keepClosed) return false
        if (!isDisconnected) {
            Log.d(tag(), "will not connect in current state ")
            return false
        }
        this.connectionState.setStatus(Status.CONNECTING)
        this.activationTime = SensorDevice.getTime()
        connectionObservable.subscribe({ this.onConnectionReceived(it) }, { this.onConnectionFailure(it) })
        return true
    }

    private fun onConnectionFailure(throwable: Throwable) {
        Log.w(tag(), "Connection failed", throwable)
        this.connectionState.setStatus(Status.DISCONNECTED)
    }

    private fun onConnectionReceived(connection: RxBleConnection) {
        Log.d(tag(), "Connection succeeded")
        this.connectionState.setStatus(Status.CONNECTED)
        this.lastChangeConnectionTime = SensorDevice.getTime()
        this.isConnected = true
        connectionObservable.flatMap { rxBleConnection -> rxBleConnection.setupNotification(SensorDevice.HEART_RATE_CHARACTERISTIC) }
                .doOnNext { notificationObservable -> notificationHasBeenSetUp() }
                .flatMap { notificationObservable -> notificationObservable }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ this.onNotificationReceived(it) }, { this.onNotificationSetupFailure(it) })
    }

    private fun onNotificationSetupFailure(throwable: Throwable) {
        Log.w(tag(), "notification setup failure", throwable)
    }

    private fun onNotificationReceived(bytes: ByteArray) {
        //Log.d(tag(), "received notification: " + HexString.bytesToHex(bytes));
        if (!connectionState.isInStatus(Status.TRANSMITTING))
            connectionState.setStatus(Status.TRANSMITTING)
        this.lastHeartRate = parseHeartRate(bytes)

        super.onDataReceived()
    }

    private fun notificationHasBeenSetUp() {
        Log.d(tag(), "notification has been set up!")
    }

    /**
     * Convert a signed byte to an unsigned int.
     */
    private fun unsignedByteToInt(b: Byte): Int {
        return b.toInt() and 0xFF
    }

    /**
     * Convert signed bytes to a 16-bit unsigned int.
     */
    private fun unsignedBytesToInt(b0: Byte, b1: Byte): Int {
        return unsignedByteToInt(b0) + (unsignedByteToInt(b1) shl 8)
    }

    private fun parseHeartRate(bytes: ByteArray): Int {
        if (bytes[0].toInt() and 0x01 != 0) {
            return unsignedBytesToInt(bytes[1], bytes[2])
            //Log.d(tag(), "Heart rate format UINT16.");
        } else {
            return unsignedByteToInt(bytes[1])
            //Log.d(tag(), "Heart rate format UINT8.");
        }
    }

    override fun disconnect() {
        connectionState.setStatus(Status.DISCONNECTING)
        disconnectTriggerSubject.onNext(null)
    }

    override fun close(keepClosed: Boolean) {
        this.keepClosed = keepClosed
        connectionState.setStatus(Status.DISCONNECTED)
        lastChangeConnectionTime = SensorDevice.getTime()
        isConnected = false
    }

    override fun reset() {
        keepClosed = false
        disconnectTriggerSubject.onNext(null)
        connectionState.setStatus(Status.DISCONNECTED)
        activationTime = SensorDevice.getTime()
        lastDataTime = -1
        lastLatency = 0
        lastHeartRate = 0
        Log.i(tag(), "Reset")
    }

    companion object {

        private var bleClient: RxBleClient? = null
    }
}
