package ru.test.multydevicetest;

import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import ru.test.multydevicetest.bluetooth.HeartRateListener;

class DataCollector implements HeartRateListener {

    @Override
    public void onDataReceived(@NotNull String address, int hr, @NotNull List<Integer> rrIntervals) {
        Log.d("DataCollector", address + ": hr=" + hr + ", rrs=" + rrIntervals);
    }
}
