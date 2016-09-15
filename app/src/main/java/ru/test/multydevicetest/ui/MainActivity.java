package ru.test.multydevicetest.ui;

import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.cardiomood.group.R;

import java.util.ArrayList;
import java.util.HashMap;

import ru.test.multydevicetest.DeviceService;
import ru.test.multydevicetest.bluetooth.SensorDevice;

public class MainActivity extends ListActivity {

    private final static String TAG = MainActivity.class.getSimpleName();

    private static final int REQUEST_ENABLE_BT = 1;

    private BluetoothAdapter mBluetoothAdapter;
    private LeDeviceListAdapter mLeDeviceListAdapter;

    private DeviceService mBluetoothLeService;

    private HashMap<String, ViewHolder> views = new HashMap<>();
    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.d(TAG,"connected to service");
            mBluetoothLeService = ((DeviceService.LocalBinder) service).getService();


            mLeDeviceListAdapter.clear();

            for(String address : mBluetoothLeService.getAllAddresses()) {
                mLeDeviceListAdapter.addDevice(address);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG,"disconnected from service");
            mBluetoothLeService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //getActionBar().setTitle(R.string.title);

        Log.d(TAG,"onCreate");
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE не поддерживается", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this,"BLUETOOTH не поддерживается", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        setListAdapter(mLeDeviceListAdapter);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d(TAG,"onResume");
        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

        Intent gattServiceIntent = new Intent(this, DeviceService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        mLeDeviceListAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG,"onPause");
        mLeDeviceListAdapter.clear();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"onDestroy");
        unbindService(mServiceConnection);
        unregisterReceiver(mGattUpdateReceiver);
        mBluetoothLeService = null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<String> mLeDeviceAddresses;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDeviceAddresses = new ArrayList<>();
            mInflator = MainActivity.this.getLayoutInflater();
            views = new HashMap<>();
        }

        public void addDevice(String address) {
            if(!mLeDeviceAddresses.contains(address)) {
                mLeDeviceAddresses.add(address);
                notifyDataSetChanged();
            }
        }

        public String getDeviceAddress(int position) {
            return mLeDeviceAddresses.get(position);
        }

        public void clear() {
            mLeDeviceAddresses.clear();
        }

        @Override
        public int getCount() {
            return mLeDeviceAddresses.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDeviceAddresses.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                viewHolder.deviceStatus = (TextView) view.findViewById(R.id.device_status);
                viewHolder.deviceData = (TextView) view.findViewById(R.id.device_pulse);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            SensorDevice device = mBluetoothLeService.getDevice(mLeDeviceAddresses.get(i));
            final String deviceName = device == null ? null : device.getName();

            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);

            viewHolder.deviceAddress.setText(device == null ? "" :device.getAddress());
            viewHolder.deviceStatus.setText(device == null ? "" : device.getStatus());

            viewHolder.deviceData.setText(R.string.no_data);
            if(device != null) views.put(device.getAddress(),viewHolder);

            return view;
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            addDeviceAddressToList(device.getAddress());
                        }
                    });
                }
            };

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        TextView deviceStatus;
        TextView deviceData;
    }

    private void addDeviceAddressToList(String address)
    {
        mLeDeviceListAdapter.addDevice(address);
        mLeDeviceListAdapter.notifyDataSetChanged();

    }

    private void displayData(final String address,final  String heartRate) {
        if(!views.containsKey(address)) return;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                views.get(address).deviceData.setText(heartRate);
            }
        });

    }

    private void updateConnectionState(final String address, final int stringId) {
        if(!views.containsKey(address)) return;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                views.get(address).deviceStatus.setText(stringId);
            }
        });
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SensorDevice.ACTION_GATT_CONNECTED);
        intentFilter.addAction(SensorDevice.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(SensorDevice.ACTION_GATT_SERVICES_UNSUPPORTED);
        intentFilter.addAction(SensorDevice.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_UNSUPPORTED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (SensorDevice.ACTION_GATT_CONNECTED.equals(action)) {
                updateConnectionState(intent.getStringExtra(DeviceService.Companion.getEXTRA_ADDRESS()),R.string.connected);
                invalidateOptionsMenu();
            } else if (SensorDevice.ACTION_GATT_DISCONNECTED.equals(action)) {
                updateConnectionState(intent.getStringExtra(DeviceService.Companion.getEXTRA_ADDRESS()),R.string.disconnected);
                displayData(intent.getStringExtra(DeviceService.Companion.getEXTRA_ADDRESS()), "NA");
            } else if (SensorDevice.ACTION_GATT_SERVICES_UNSUPPORTED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                //displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (SensorDevice.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(DeviceService.Companion.getEXTRA_ADDRESS()), intent.getStringExtra(DeviceService.Companion.getEXTRA_HEART_RATE()));
            }
        }
    };

}
