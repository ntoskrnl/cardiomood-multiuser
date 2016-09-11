package ru.test.multydevicetest.utils;

/**
 * Created by Bes on 09.08.2016.
 */
public class EnumRef<T extends Enum<?>> {
    public T value;

    public EnumRef(T value)
    {
        this.value = value;
    }

    @Override
    public String toString()
    {
        return value.toString();
    }
}
