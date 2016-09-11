package ru.test.multydevicetest.bluetooth

interface HeartRateListener {

    fun onDataReceived(address: String, hr: Int, rrIntervals: List<Int>)

}