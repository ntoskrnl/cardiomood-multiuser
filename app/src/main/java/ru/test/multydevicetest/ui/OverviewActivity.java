package ru.test.multydevicetest.ui;

import android.app.Activity;
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
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.cardiomood.group.R;

import java.util.ArrayList;

import ru.test.multydevicetest.DeviceService;
import ru.test.multydevicetest.bluetooth.BluetoothGattWrapper;
import ru.test.multydevicetest.bluetooth.BluetoothStat;
import ru.test.multydevicetest.bluetooth.SensorDevice;
import ru.test.multydevicetest.utils.Utils;

public class OverviewActivity extends AppCompatActivity implements IDeviceProvider{

    private final static String TAG = OverviewActivity.class.getSimpleName();
    private static final int REQUEST_ENABLE_BT = 1;
    public static final int LATENCY_LIMIT_GOOD = 2000;
    public static final int LATENCY_LIMIT_WARNING = 10000;
    private static final long SCAN_PERIOD = 8000;
    private static final long SCAN_COOLDOWN_PERIOD = 15000;
    private static final long UPDATE_PERIOD = 500;

    private BluetoothAdapter bluetoothAdapter;
    private DeviceService bluetoothService;

    private GridAdapter gridAdapter;

    private TextView bluetoothStatView;
    private TextView totalDevicesCountView;
    private TextView activeDeviceCountView;
    private TextView longestLatencyView;
    private TextView maxActiveView;
    private volatile boolean isUpdateCycleRunning;
    private Thread updaterThread = null;

    private Handler scanHandler;
    private boolean isScanning = false;
    private Menu scanMenu = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_overview);

        scanHandler = new Handler();

        Log.d(TAG, "onCreate");
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
        bluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "BLUETOOTH не поддерживается", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes  view adapter.
        gridAdapter = new GridAdapter(this);
        GridView gridView = ((GridView) findViewById(R.id.pulses));
        if (gridView != null)
            gridView.setAdapter(gridAdapter);


        totalDevicesCountView = (TextView) findViewById(R.id.device_total_count);
        activeDeviceCountView = (TextView) findViewById(R.id.device_active_count);
        longestLatencyView = (TextView) findViewById(R.id.device_longest_update);
        maxActiveView = (TextView) findViewById(R.id.device_max_active_count);
        bluetoothStatView = (TextView) findViewById(R.id.device_bluetooth_stat);
        bluetoothStatView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BluetoothGattWrapper.resetStat();
            }
        });

        Button buttonInc = (Button) findViewById(R.id.button_inc);
        Button buttonDec = (Button) findViewById(R.id.button_dec);
        if(buttonInc != null)
        buttonInc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bluetoothService == null)
                    maxActiveView.setText(OverviewActivity.this.getString(R.string.no_data));
                else
                    maxActiveView.setText(String.valueOf(bluetoothService.maxActiveCountInc()));
            }
        });

        if(buttonDec != null)
        buttonDec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bluetoothService == null)
                    maxActiveView.setText(OverviewActivity.this.getString(R.string.no_data));
                else
                    maxActiveView.setText(String.valueOf(bluetoothService.maxActiveCountDec()));
            }
        });

        Intent gattServiceIntent = new Intent(this, DeviceService.class);
        bindService(gattServiceIntent, serviceConnection, BIND_AUTO_CREATE);

    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d(TAG, "onResume");
        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!bluetoothAdapter.isEnabled()) {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        registerReceiver(updateReceiver, makeGattUpdateIntentFilter());



        //gridAdapter.notifyDataSetChanged();

        (updaterThread = new Thread(new Runnable() {
            @Override
            public void run() {
                isUpdateCycleRunning = true;
                int scanCountDownStart = (int) ((SCAN_COOLDOWN_PERIOD + SCAN_PERIOD) / UPDATE_PERIOD);
                if ((SCAN_COOLDOWN_PERIOD + SCAN_PERIOD) % UPDATE_PERIOD > 0) scanCountDownStart++;
                int scanCountDown = scanCountDownStart;
                try {
                    while (isUpdateCycleRunning) {
                        Thread.sleep(UPDATE_PERIOD);

                        if (scanCountDown <= 0) {
                            scanCountDown = scanCountDownStart;
                            //doScan(true);
                        }
                        scanCountDown--;

                        if (bluetoothService == null) continue;
                        bluetoothService.doUpdateStats();

                        if (gridAdapter == null) continue;
                        {
                            final ArrayList<String> addressesToRemove = new ArrayList<>();
                            for (final String address : gridAdapter.deviceAddresses) {
                                SensorDevice device = bluetoothService == null ? null : bluetoothService.getDevice(address);
                                if (device == null) {
                                    addressesToRemove.add(address);
                                } else {
                                    gridAdapter.displayData(device);
                                }
                            }

                            if (addressesToRemove.size() > 0)
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        GridAdapter newGridAdapter = new GridAdapter(OverviewActivity.this);

                                        for (String address : gridAdapter.deviceAddresses) {
                                            if(addressesToRemove.contains(address)) continue;
                                            newGridAdapter.deviceAddresses.add(address);
                                        }

                                        GridView gridView = ((GridView) findViewById(R.id.pulses));
                                        if (gridView != null)
                                            gridView.setAdapter(newGridAdapter);
                                        gridAdapter = newGridAdapter;
                                        //gridAdapter.notifyDataSetChanged();
                                    }
                                });
                        }

                    }
                } catch (InterruptedException e) {
                    Log.d(TAG, "Update process interrupted");
                }
            }
        })).start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        isUpdateCycleRunning = false;
        if (updaterThread != null) {
            updaterThread.interrupt();
            updaterThread = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        unbindService(serviceConnection);
        unregisterReceiver(updateReceiver);
        bluetoothService = null;
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        scanMenu = menu;
        if (!isScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                doScan(true);
                break;
            case R.id.menu_stop:
                doScan(false);
                break;
            case R.id.menu_exit:
                if (bluetoothService != null) {
                    Utils.execInNewThread(new Runnable() {
                        @Override
                        public void run() {
                            bluetoothService.doStop();
                        }
                    });

                }
                finish();
                break;
        }
        return true;
    }

    // Code to manage Service lifecycle.
    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.d(TAG, "connected to service");
            bluetoothService = ((DeviceService.LocalBinder) service).getService();

            gridAdapter.clear();

            for (String address : bluetoothService.getAllAddresses())
                gridAdapter.addDevice(address);
            gridAdapter.notifyDataSetChanged();

            maxActiveView.setText(String.valueOf(bluetoothService.getMaxActiveDevices()));
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, "disconnected from service");
            bluetoothService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_UNSUPPORTED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (SensorDevice.ACTION_GATT_CONNECTED.equals(action)) {
                gridAdapter.updateConnectionState(
                        intent.getStringExtra(DeviceService.EXTRA_ADDRESS),
                        true,
                        0);
                invalidateOptionsMenu();
            } else if (SensorDevice.ACTION_GATT_DISCONNECTED.equals(action)) {
                gridAdapter.updateConnectionState(
                        intent.getStringExtra(DeviceService.EXTRA_ADDRESS),
                        false,
                        0);
            } else if (SensorDevice.ACTION_GATT_SERVICES_UNSUPPORTED.equals(action)) {
                gridAdapter.showUnsupported(intent.getStringExtra(DeviceService.EXTRA_ADDRESS));
            } else if (SensorDevice.ACTION_DATA_AVAILABLE.equals(action)) {
                gridAdapter.displayData(
                        intent.getStringExtra(DeviceService.EXTRA_ADDRESS),
                        intent.getStringExtra(DeviceService.EXTRA_HEART_RATE),
                        intent.getLongExtra(DeviceService.EXTRA_LATENCY, -1)
                );
            } else if (DeviceService.INFO_STATISTICS.equals(action)) {
                showStatistics(
                        intent.getIntExtra(DeviceService.EXTRA_TOTAL, -1),
                        intent.getIntExtra(DeviceService.EXTRA_ACTIVE, -1),
                        intent.getIntExtra(DeviceService.EXTRA_MAX_ACTIVE, -1),
                        intent.getLongExtra(DeviceService.EXTRA_MAX_LATENCY, -1)
                );
            } else if (SensorDevice.ACTION_BATTERY_LEVEL.equals(action)) {
                gridAdapter.displayBattery(
                        intent.getStringExtra(DeviceService.EXTRA_ADDRESS),
                        intent.getIntExtra(DeviceService.EXTRA_BATTERY_LEVEL, -1)
                );
            } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF) {
                    Log.i(TAG, "Bluetooth goes OFF");
                    if (bluetoothService != null) bluetoothService.onBluetoothOff();

                } else if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_ON) {
                    Log.i(TAG, "Bluetooth goes ON");
                    if (bluetoothService != null) bluetoothService.onBluetoothOn();
                }

            }
        }
    };


    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SensorDevice.ACTION_GATT_CONNECTED);
        intentFilter.addAction(SensorDevice.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(SensorDevice.ACTION_GATT_SERVICES_UNSUPPORTED);
        intentFilter.addAction(SensorDevice.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(DeviceService.INFO_STATISTICS);
        intentFilter.addAction(SensorDevice.ACTION_BATTERY_LEVEL);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        return intentFilter;
    }



    private void showStatistics(
            final int totalCount,
            final int activeCount,
            final int activeMax,
            final long longestLatency
            ) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                totalDevicesCountView.setText((String.valueOf(totalCount)));
                activeDeviceCountView.setText(String.format(getString(R.string.active_count_format), activeCount, activeMax));
                longestLatencyView.setText(String.format(getString(R.string.latency_format), longestLatency / 1000.));

                BluetoothStat bluetoothStat = BluetoothGattWrapper.getStat();

                String bluetoothStatText = "";
                bluetoothStatText += "C:" +bluetoothStat.connectionSuccess + "/" + bluetoothStat.connectionsTotal + " ";
                bluetoothStatText += "D:" +bluetoothStat.disconnectionSuccess + "/" + bluetoothStat.disconnectionTotal + "\n";
                bluetoothStatText += "N:" +bluetoothStat.notificationSuccess + "/" + bluetoothStat.notificationTotal + " ";
                bluetoothStatText += "S:" +bluetoothStat.discoverSuccess + "/" + bluetoothStat.discoverTotal + "\n";

                bluetoothStatView.setText(bluetoothStatText);

            }
        });
    }


    // Device scan callback.
    private BluetoothAdapter.LeScanCallback leScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (bluetoothService != null) {
                                if(bluetoothService.addDevice(device))
                                    gridAdapter.addDevice(device.getAddress());
                            }
                        }
                    });
                }
            };

    private void doScan(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            scanHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    isScanning = false;
                    bluetoothAdapter.stopLeScan(leScanCallback);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (scanMenu != null) {
                                scanMenu.findItem(R.id.menu_stop).setVisible(false);
                                scanMenu.findItem(R.id.menu_scan).setVisible(true);
                            }
                        }
                    });
                }
            }, SCAN_PERIOD);

            isScanning = true;
            bluetoothAdapter.startLeScan(leScanCallback);
        } else {
            isScanning = false;
            bluetoothAdapter.stopLeScan(leScanCallback);
        }
        invalidateOptionsMenu();
    }

    @Override
    public SensorDevice getDevice(String address) {
        if(bluetoothService == null) return null;
        return bluetoothService.getDevice(address);
    }
}
