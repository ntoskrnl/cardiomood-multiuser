package ru.test.multydevicetest.bluetooth;

/**
 * Created by Bes on 18.08.2016.
 */
public class DutyCycleTime {
    public long on = 0;
    public long off = 0;
    private boolean isTransmitting = false;

    public int calcPercent() {
        if (on == 0 || off == 0)// divide by 0!
        {
            if (isTransmitting)
                return 100; // we have just started
            else
                return 0; // we have not got anything yet
        }
        return (int) ((on * 100) / (off + on));
    }

    public void setTransmitting(boolean isTransmitting)
    {
        this.isTransmitting = isTransmitting;
    }
}
