package ru.test.multydevicetest.ui;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.cardiomood.group.R;

import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import ru.test.multydevicetest.bluetooth.SensorDevice;

/**
 * Created by Bes on 17.08.2016.
 */
public class GridAdapter extends BaseAdapter {
    private HashMap<String, ViewHolder> views;
    public CopyOnWriteArrayList<String> deviceAddresses;
    private Activity activity;

    public GridAdapter(Activity activity) {
        super();
        this.activity = activity;
        views = new HashMap<>();
        deviceAddresses = new CopyOnWriteArrayList<>();
    }

    public void addDevice(String address) {
        if (!deviceAddresses.contains(address)) {
            deviceAddresses.add(address);
            notifyDataSetChanged();
        }
    }

    public void clear() {
        deviceAddresses.clear();
    }

    /*public void removeDevice(String address) {
            if (deviceAddresses.contains(address)) {
                deviceAddresses.remove(address);
                notifyDataSetChanged();
            }
        }*/

    @Override
    public int getCount() {
        return deviceAddresses.size();
    }

    @Override
    public Object getItem(int i) {
        return deviceAddresses.get(i);
    }

    @Override
    public long getItemId(int i) {
        return deviceAddresses.get(i).hashCode();
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder viewHolder;
        //General ListView optimization code.
        if (view == null) {
            LayoutInflater inflater =
                    (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.griditem_device,null);
            viewHolder = new ViewHolder();
            viewHolder.cellView = view;
            viewHolder.statusView = view.findViewById(R.id.device_stats);
            viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
            viewHolder.deviceMAC = (TextView) view.findViewById(R.id.device_mac);
            viewHolder.pulse = (TextView) view.findViewById(R.id.device_pulse);
            viewHolder.dutyCycle = (TextView) view.findViewById(R.id.device_duty_cycle);
            viewHolder.batteryLevel = (TextView) view.findViewById(R.id.battery_level);
            viewHolder.status = (TextView) view.findViewById(R.id.device_status);
            viewHolder.timeInStatus = (TextView) view.findViewById(R.id.device_time_in_status);
            viewHolder.latency = (TextView) view.findViewById(R.id.device_latency);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
            viewHolder.cellView = view;
        }

        SensorDevice device = ((IDeviceProvider)activity).getDevice(deviceAddresses.get(i));
        final String deviceName = device == null ? null : device.getName() + " :" +  device.deviceManagerId;

        if (deviceName != null && deviceName.length() > 0)
            viewHolder.deviceName.setText(deviceName);
        else
            viewHolder.deviceName.setText(R.string.unknown_device);


        if(device == null) {
            viewHolder.pulse.setText(R.string.no_data);
            viewHolder.dutyCycle.setText(R.string.no_data);
            viewHolder.status.setText(R.string.no_data);
            viewHolder.timeInStatus.setText(R.string.no_data);
        } else
        {
            viewHolder.pulse.setText(String.valueOf(device.getHeartRate()));
            viewHolder.dutyCycle.setText(device.getLastDutyCycle().calcPercent() + "%");
            viewHolder.status.setText(device.getStatus());
            viewHolder.timeInStatus.setText(String.valueOf(device.getLastConnectionStatusTime()/1000) + "с");
        }

        viewHolder.cellView.setBackgroundColor(activity.getResources().getColor(R.color.connecting));
        if (device != null && !views.containsKey(device.getAddress())) {
            viewHolder.deviceMAC.setText(device.getAddress());
            views.put(device.getAddress(), viewHolder);
        }

        return view;
    }

    public void displayData(final String address, final String heartRate, final long latency) {
        if (!views.containsKey(address)) return;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                views.get(address).pulse.setText(heartRate);
                views.get(address).latency.setText(String.format(activity.getString(R.string.latency_format), latency / 1000.));
                /*views.get(address).dutyCycle.setText(String.format(getString(R.string.latency_format), latency / 1000.));
                if (latency == 0)
                    views.get(address).cellView.setBackgroundColor(getResources().getColor(R.color.disconnected));
                else if (latency < LATANCY_LIMIT_GOOD)
                    views.get(address).cellView.setBackgroundColor(getResources().getColor(R.color.latency_good));
                else if (latency < LATENCY_LIMIT_WARNING)
                    views.get(address).cellView.setBackgroundColor(getResources().getColor(R.color.latency_warning));
                else
                    views.get(address).cellView.setBackgroundColor(getResources().getColor(R.color.latency_bad));*/
            }
        });

    }

    public void displayData(final SensorDevice device) {
        if (!views.containsKey(device.getAddress())) return;
        final ViewHolder viewHolder = views.get(device.getAddress());
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                viewHolder.pulse.setText(String.valueOf(device.getHeartRate()));
                viewHolder.dutyCycle.setText(device.getLastDutyCycle().calcPercent() + "%");
                viewHolder.latency.setText(String.format(activity.getString(R.string.latency_format), device.getLatency()/ 1000.));
                if(device.getLastConnectionState()) {
                    viewHolder.cellView.setBackgroundColor(activity.getResources().getColor(R.color.connected));
                    viewHolder.status.setText(activity.getString(R.string.connected));
                }
                else{
                    viewHolder.cellView.setBackgroundColor(activity.getResources().getColor(R.color.disconnected));
                    viewHolder.status.setText(activity.getString(R.string.disconnected));
                }
                String timeInfo = String.valueOf(device.getLastConnectionStatusTime()/1000) + "с";
                viewHolder.timeInStatus.setText(timeInfo);
            }

        });
    }

    public void displayBattery(final String address, final int batteryLevel) {
        if (!views.containsKey(address)) return;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (batteryLevel >= 0) {
                    views.get(address).batteryLevel.setText(String.valueOf(batteryLevel) + "%");
                } else {
                    views.get(address).batteryLevel.setText("");
                }
            }
        });
    }


    public void updateConnectionState(final String address, final boolean isConnected, final long lastConnectionTime) {
        if (!views.containsKey(address)) return;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isConnected) {
                    views.get(address).cellView.setBackgroundColor(activity.getResources().getColor(R.color.connected));
                    views.get(address).status.setText(activity.getString(R.string.connected));
                } else {
                    views.get(address).cellView.setBackgroundColor(activity.getResources().getColor(R.color.disconnected));

                    views.get(address).status.setText(activity.getString(R.string.disconnected));
                }
                views.get(address).pulse.setText(R.string.no_data);
                if(lastConnectionTime > 0) {
                        String timeInfo = String.valueOf(lastConnectionTime / 1000)+"с";
                        views.get(address).timeInStatus.setText(timeInfo);
                } else
                {
                    views.get(address).timeInStatus.setText(activity.getString(R.string.no_data));
                }
            }
        });
    }

    public void showUnsupported(final String address) {
        if (!views.containsKey(address)) return;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                views.get(address).cellView.setBackgroundColor(activity.getResources().getColor(R.color.unsupported));
            }
        });
    }

}
