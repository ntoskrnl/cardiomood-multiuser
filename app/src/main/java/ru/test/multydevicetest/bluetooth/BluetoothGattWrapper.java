package ru.test.multydevicetest.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import ru.test.multydevicetest.utils.EnumRef;
import ru.test.multydevicetest.utils.Utils;

/**
 * Created by Bes on 27.07.2016.
 */
public class BluetoothGattWrapper {

    private static final String TAG = BluetoothGattWrapper.class.getSimpleName();
    private static final int MAX_ACTIVE_ACTION = 5;
    private static final long ACTION_TIMEOUT = 8000;
    private static final String NO_ACTION = "no action";

    private static BluetoothStat stat = new BluetoothStat();

    public static BluetoothStat getStat() {
        synchronized (stat.statSyncObject) {
            return new BluetoothStat(stat);
        }
    }

    public static void resetStat() {
        synchronized (stat.statSyncObject) {
            stat.reset();
        }
    }


    private enum State {
        UNINIT,
        IDLE,
        ACTION,
        CLOSED;

        @Override
        public String toString() {
            switch (this) {
                case UNINIT:
                    return "Uninitialized";
                case IDLE:
                    return "IDLE";
                case ACTION:
                    return "ACTION";
                case CLOSED:
                    return "CLOSED";
                default:
                    return "UNKNOWN";
            }
        }
    }

    private static ConcurrentHashMap<String, BluetoothGattWrapper> wrappers = new ConcurrentHashMap<>();
    private static final Object syncObject = new Object();
    private static final Object disconnectionSyncObject = new Object();
    private static final Object connectionSyncObject = new Object();
    private static final Object notificationSyncObject = new Object();
    private static final Object discoverServiceSyncObject = new Object();
    private static final EnumRef<State> disconnectionState = new EnumRef<>(State.IDLE);
    private static final EnumRef<State> connectionState = new EnumRef<>(State.IDLE);
    private static final EnumRef<State> discoverState = new EnumRef<>(State.IDLE);
    private static final EnumRef<State> notificationState = new EnumRef<>(State.IDLE);
    private static volatile int activeActionCount;

    private static final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            stopActionForAddress(gatt.getDevice().getAddress());
            Log.d(TAG, "onDescriptorWrite: " + gatt.getDevice().getAddress());
        }


        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
            stopActionForAddress(gatt.getDevice().getAddress());
            Log.d(TAG, "onReliableWriteCompleted: " + gatt.getDevice().getAddress());
        }


        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            stopActionForAddress(gatt.getDevice().getAddress());
            Log.d(TAG, "onReadRemoteRssi: " + gatt.getDevice().getAddress());
        }


        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            stopActionForAddress(gatt.getDevice().getAddress());
            Log.d(TAG, "onMtuChanged: " + gatt.getDevice().getAddress());
        }


        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            BluetoothGattWrapper wrapper;
            wrapper = getWrapper(gatt.getDevice().getAddress());
            if (wrapper != null)
                wrapper.listener.onNotification(wrapper, characteristic);
            else
                Log.d(TAG, "no wrapper for " + gatt.getDevice().getAddress());
        }


        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            stopActionForAddress(gatt.getDevice().getAddress());
            Log.d(TAG, "onDescriptorRead: " + gatt.getDevice().getAddress());
        }


        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            final BluetoothGattWrapper wrapper = stopActionForAddress(gatt.getDevice().getAddress());
            if (wrapper != null)
                Utils.execInNewThread(new Runnable() {
                    @Override
                    public void run() {
                        wrapper.listener.onRead(wrapper, characteristic, status);
                    }
                });
            else
                Log.d(TAG, "no wrapper for " + gatt.getDevice().getAddress());
            Log.d(TAG, "onCharacteristicRead: " + gatt.getDevice().getAddress());
        }


        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            stopActionForAddress(gatt.getDevice().getAddress());
            Log.d(TAG, "onCharacteristicWrite: " + gatt.getDevice().getAddress());
        }


        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            final BluetoothGattWrapper wrapper = stopActionForAddress(gatt.getDevice().getAddress());
            Log.d(TAG, "onConnectionStateChange: " + gatt.getDevice().getAddress());
            if (wrapper == null) {
                Log.d(TAG, "no wrapper for " + gatt.getDevice().getAddress());
                return;
            }
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    /*synchronized (connectionSyncObject) {
                        connectionState.value = State.IDLE;
                        connectionSyncObject.notifyAll();
                    }*/
                    Utils.execInNewThread(new Runnable() {
                        @Override
                        public void run() {
                            wrapper.listener.onConnected(wrapper);
                        }
                    });

                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    /*
                    {
                        wrapper.stopAction();
                    }*/
                    /*synchronized (disconnectionSyncObject) {
                        disconnectionState.value = State.IDLE;
                        disconnectionSyncObject.notifyAll();
                    }
                    */
                    Utils.execInNewThread(new Runnable() {
                        @Override
                        public void run() {
                            wrapper.listener.onDisconnected(wrapper);
                        }
                    });
                    break;
                default:
                    wrapper.listener.onUnknownConnectionState(wrapper, newState);
            }

        }


        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, final int status) {
            Log.d(TAG, "onServicesDiscovered: " + gatt.getDevice().getAddress());
            synchronized (discoverServiceSyncObject) {
                discoverState.value = State.IDLE;
                discoverServiceSyncObject.notifyAll();
            }
            final BluetoothGattWrapper wrapper = stopActionForAddress(gatt.getDevice().getAddress());
            if (wrapper != null) {
                Utils.execInNewThread(new Runnable() {
                    @Override
                    public void run() {
                        wrapper.listener.onServicesDiscovered(wrapper, status);
                    }
                });
            } else
                Log.d(TAG, "no wrapper for " + gatt.getDevice().getAddress());
        }
    };

    private static BluetoothGattWrapper getWrapper(String address) {
        synchronized (syncObject) {
            if (!wrappers.keySet().contains(address)) return null;
            else return wrappers.get(address);
        }
    }

    public static BluetoothGattWrapper connectDevice(Context ctx, String address, IBluetoothEventListener listener) {

        synchronized (stat.statSyncObject) {
            stat.connectionsTotal++;
        }

        BluetoothManager bluetoothManager = (BluetoothManager) ctx.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            Log.e(TAG, "Unable to initialize BluetoothManager for " + address);
            return null;
        }
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter for " + address);
            return null;
        }
        final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Can't find bluetooth device " + address);
            return null;
        }

        BluetoothGattWrapper wrapper = null;


        synchronized (syncObject) {

            if (!waitForFreeAction(address)) return null;


            BluetoothGatt gatt = device.connectGatt(ctx, true, gattCallback);
            if (gatt == null) {
                Log.w(TAG, "Can't start connection to " + address);
                return null;
            }

            wrapper = new BluetoothGattWrapper(address, gatt, listener);
            wrappers.put(address, wrapper);
            wrapper.actionState.value = State.IDLE;
            Log.d(TAG, "Connecting new device " + address);
            wrapper.startAction("connecting");

        }
        synchronized (stat.statSyncObject) {
            stat.connectionSuccess++;
        }
        return wrapper;
/*
        synchronized (syncObject) {
            if (!waitForFreeAction(address)) return null;
            activeActionCount++;
            Log.d(TAG + ":" + address, " connection action started " + activeActionCount + "/" + MAX_ACTIVE_ACTION);
        }
        try {
            synchronized (connectionSyncObject) {
                if (!waitOnObjectForIdleState(connectionSyncObject, ACTION_TIMEOUT, connectionState)) {
                    Log.w(TAG, "Could not connect " + address + " - waiting timeout exceeded");
                    throw new TimeoutException();
                }


                connectionState.value = State.ACTION;
            }

        } catch (Exception e) {
            Log.w(TAG, "Waiting to connect " + address + " failed :" + e.getClass().getSimpleName());
            synchronized (syncObject) {
                if (!waitForFreeAction(address)) return null;
                activeActionCount--;
                Log.d(TAG + ":" + address, " connection action aborted " + activeActionCount + "/" + MAX_ACTIVE_ACTION);
                syncObject.notifyAll();
            }
            return null;
        }


        BluetoothGatt gatt = device.connectGatt(ctx, false, gattCallback);
        if (gatt == null) {
            Log.w(TAG, "Can't start connection to " + address);
            synchronized (connectionSyncObject) {
                connectionState.value = State.IDLE;
                connectionSyncObject.notifyAll();
            }
            synchronized (syncObject) {
                if (!waitForFreeAction(address)) return null;
                activeActionCount--;
                Log.d(TAG + ":" + address, " connection action failed " + activeActionCount + "/" + MAX_ACTIVE_ACTION);
                syncObject.notifyAll();
            }
            return null;
        }
        synchronized (syncObject) {
            wrapper = new BluetoothGattWrapper(address, gatt, listener);
            wrappers.put(address, wrapper);
            Log.d(TAG, "Connecting new device " + address);
            wrapper.actionState.value = State.ACTION;
            wrapper.curActionName = "connection";
        }

        synchronized (stat.statSyncObject) {
            stat.connectionSuccess++;
        }
        return wrapper;
        */
    }

    private final String address;
    private final EnumRef<State> actionState = new EnumRef<>(State.IDLE);
    private final BluetoothGatt gatt;
    private final IBluetoothEventListener listener;
    private volatile String curActionName = NO_ACTION;

    private BluetoothGattWrapper(String address, BluetoothGatt gatt, IBluetoothEventListener listener) {
        this.address = address;
        this.actionState.value = State.UNINIT;
        this.gatt = gatt;

        if (listener == null) throw new NullPointerException();
        this.listener = listener;
    }

    /**
     * Use only in synchronized(syncObject) block!!!
     *
     * @param address - address of device, which needs action
     * @return true if there is free action, false if waiting timeout  exceeded
     */
    private static boolean waitForFreeAction(String address) {
        if (activeActionCount >= MAX_ACTIVE_ACTION) {
            Log.d(TAG, "exceeded maximum of active actions (" + MAX_ACTIVE_ACTION + ") - now waiting");
            try {
                if (!waitOnObjectForCondition(syncObject, ACTION_TIMEOUT, new ICondition() {
                    @Override
                    public boolean isTrue() {
                        return activeActionCount < MAX_ACTIVE_ACTION;
                    }
                })) {
                    Log.w(TAG, "Could not do action " + address + ", no free action available");
                    return false;
                }

            } catch (InterruptedException e) {
                Log.w(TAG, "Waiting for free action was interrupted");
                return false;
            }
        }
        Log.d(TAG, "action finished, now continue");
        return true;
    }

    private static BluetoothGattWrapper stopActionForAddress(String address) {
        BluetoothGattWrapper wrapper;
        synchronized (syncObject) {
            wrapper = getWrapper(address);
            if (wrapper == null) return null;
            wrapper.stopAction();
            return wrapper;
        }
    }


    private final Object localSyncObject = new Object();

    private void startAction(String actionName) {
        if (actionState.value != State.IDLE)
            throw new IllegalStateException("Can't reserve action for " + address + " in state " + actionState.toString());
        if (activeActionCount >= MAX_ACTIVE_ACTION)
            throw new IndexOutOfBoundsException("Can't reserve action for " + address + "- no more free action");
        activeActionCount++;
        actionState.value = State.ACTION;
        curActionName = actionName;
        Log.d(tag(), "startAction: " + activeActionCount + "/" + MAX_ACTIVE_ACTION);
    }

    private void stopAction() {
        if (actionState.value != State.ACTION) return;
        if (activeActionCount <= 0)
            throw new IndexOutOfBoundsException("Can't free action for " + address + " - no action is reserved");
        activeActionCount--;
        Log.d(tag(), "stopAction: " + activeActionCount + "/" + MAX_ACTIVE_ACTION);
        actionState.value = State.IDLE;
        curActionName = NO_ACTION;
        syncObject.notifyAll();
    }

    /**
     * Use only in synchronized(localSyncObject) block!!!
     *
     * @return true if there is no ongoing action, false if waiting timeout  exceeded
     */
    private boolean waitActionToFinish() {
        if (actionState.value == State.CLOSED) {
            Log.d(tag(), "We are closed - no actions allowed");
            return false;
        }
        if (actionState.value != State.ACTION) return true;
        Log.d(tag(), " there is ongoing action - now waiting");

        try {

            if (!waitOnObjectForIdleState(syncObject, ACTION_TIMEOUT, actionState)) {
                Log.w(tag(), "Could not do action there is unfinished ongoing action");
                return false;
            }

        } catch (InterruptedException e) {
            Log.w(tag(), "Waiting for action end was interrupted");
            return false;
        }

        Log.d(tag(), "Ongoing action finished!");

        return true;
    }

    private String tag() {
        return TAG + ": " + address + ": " + actionState.toString() + ":" + curActionName + ":";
    }


    public boolean connect() {
        synchronized (stat.statSyncObject) {
            stat.connectionsTotal++;
        }
        synchronized (syncObject) {
            Log.d(tag(), "Connecting existing device ");
            if (!waitForFreeAction(address)) return false;
            //if (!waitActionToFinish()) return false;
            synchronized (stat.statSyncObject) {
                stat.connectionSuccess++;
            }
            startAction("connecting");
        }
        try {
            return gatt.connect();
        } catch (Throwable e) {
            Log.e(tag(), "Exception while reconnecting", e);
            synchronized (localSyncObject) {
                stopAction();
            }
            throw e;
        }
    }

    public void disconnect() {
        synchronized (stat.statSyncObject) {
            stat.disconnectionTotal++;
        }
        boolean isDisconnectionSuccessful =
                /*doSelfSyncAction(new IAction() {
            @Override
            public boolean doAction() {
                gatt.disconnect();
                return true;
            }
        }, disconnectionSyncObject,
                disconnectionState,
                "disconnecting");*/
                doGlobalSyncAction(new IAction() {
                                       @Override
                                       public boolean doAction() {
                                           gatt.disconnect();
                                           return true;
                                       }
                                   },
                        "disconnecting");

        if (isDisconnectionSuccessful) {
            synchronized (stat.statSyncObject) {
                stat.disconnectionSuccess++;
            }
        } else {
            Utils.execInNewThread(new Runnable() {
                @Override
                public void run() {
                    listener.onDisconnected(BluetoothGattWrapper.this);
                }
            });
            //stopAction();
            close();
        }
    }

    public void close() {
        Log.d(tag(), "Closing ");
        synchronized (syncObject) {
            if (actionState.value == State.ACTION)
                stopAction();
            if (wrappers.keySet().contains(address))
                wrappers.remove(address);
            actionState.value = State.CLOSED;
            syncObject.notifyAll();
        }
        synchronized (discoverServiceSyncObject) {
            discoverState.value = State.IDLE;
            discoverServiceSyncObject.notifyAll();
        }
        synchronized (notificationSyncObject) {
            notificationState.value = State.IDLE;
            notificationSyncObject.notifyAll();
        }
        synchronized (connectionSyncObject) {
            connectionState.value = State.IDLE;
            connectionSyncObject.notifyAll();
        }
        synchronized (disconnectionSyncObject) {
            disconnectionState.value = State.IDLE;
            disconnectionSyncObject.notifyAll();
        }
        gatt.close();
    }

    public void readCharacteristic(final BluetoothGattCharacteristic characteristic) {
        doGlobalSyncAction(new IAction() {
            @Override
            public boolean doAction() {
                return gatt.readCharacteristic(characteristic);
            }
        }, "reading characteristic");
    }

    public void setCharacteristicNotification(final BluetoothGattCharacteristic characteristic, final boolean enabled) {
        doGlobalSyncAction(new IAction() {
            @Override
            public boolean doAction() {
                boolean ret = gatt.setCharacteristicNotification(characteristic, enabled);
                synchronized (syncObject) {
                    stopAction();
                }
                return ret;
            }
        }, "switching on notification");
    }

    public void writeDescriptor(final BluetoothGattDescriptor descriptor) {
        synchronized (stat.statSyncObject) {
            stat.notificationTotal++;
        }
        if (doGlobalSyncAction(new IAction() {
            @Override
            public boolean doAction() {
                return gatt.writeDescriptor(descriptor);
            }
        }, "writing descriptor"))
            synchronized (stat.statSyncObject) {
                stat.notificationSuccess++;
            }

    }

    public boolean discoverServices() {
        synchronized (stat.statSyncObject) {
            stat.discoverTotal++;
        }

        if (doSelfSyncAction(new IAction() {
                                 @Override
                                 public boolean doAction() {
                                     return gatt.discoverServices();
                                 }
                             },
                discoverServiceSyncObject,
                discoverState,
                "discovering services")) {
            synchronized (stat.statSyncObject) {
                stat.discoverSuccess++;
            }
            return true;
        }
        return false;
    }

    public List<BluetoothGattService> getServices() {
        return gatt.getServices();
    }

    private interface ICondition {
        boolean isTrue();
    }

    private static boolean waitOnObjectForCondition(Object sync, long timeout, ICondition condition) throws InterruptedException {
        long startWaitTime = System.currentTimeMillis();
        if (condition == null) return false;
        if (condition.isTrue()) return true;
        while (System.currentTimeMillis() - startWaitTime < timeout && !condition.isTrue()) {
            sync.wait(timeout);
        }
        return condition.isTrue();
    }

    private static boolean waitOnObjectForIdleState(Object sync, long timeout, final EnumRef<State> state) throws InterruptedException {
        return waitOnObjectForCondition(sync, timeout, new ICondition() {
            @Override
            public boolean isTrue() {
                return state.value == State.IDLE;
            }
        });
    }

    private interface IAction {
        boolean doAction();
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public boolean doSelfSyncAction(IAction action, final Object sync, EnumRef<State> state, String description) {
        synchronized (syncObject) {
            Log.d(tag(), description);
            //if (!waitActionToFinish()) return false;
            if (!waitForFreeAction(address)) return false;
            startAction(description);
        }
        synchronized (sync) {
            Log.d(tag(), description);

            try {
                if (!waitOnObjectForIdleState(sync, ACTION_TIMEOUT, state)) {
                    Log.w(tag(), "Could not " + description + " - waiting timeout exceeded");
                    return false;
                }
                state.value = State.ACTION;
            } catch (InterruptedException e) {
                Log.w(tag(), "Waiting for " + description + " was interrupted");
                return false;
            }
        }
        try {
            return action.doAction();
        } catch (Throwable e) {
            Log.e(tag(), "Exception while " + description, e);
            synchronized (sync) {
                state.value = State.IDLE;
                sync.notifyAll();
            }
            stopAction();
            throw e;
        }
    }

    public boolean doGlobalSyncAction(IAction action, String description) {
        synchronized (syncObject) {
            Log.d(tag(), description);
            //if (!waitActionToFinish()) return false;
            if (!waitForFreeAction(address)) return false;
            startAction(description);
        }
        try {
            return action.doAction();
        } catch (Throwable e) {
            synchronized (syncObject) {
                Log.e(tag(), "Exception while " + description, e);
                stopAction();
            }
            throw e;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        //Log.d(tag(), "finalize");
        if (actionState.value == State.ACTION)
            stopAction();
    }
}
