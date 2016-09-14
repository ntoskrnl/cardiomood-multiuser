package ru.test.multydevicetest

import android.util.Log
import com.cardiomood.group.api.User
import ru.test.multydevicetest.bluetooth.HeartRateListener
import java.util.*

internal class DataCollector : HeartRateListener {

    val pairing = mutableMapOf<String, User>()

    val data = mutableMapOf<String, Data>()

    override fun onDataReceived(address: String, hr: Int, rrIntervals: List<Int>) {
        val user = pairing[address]
        if (user != null) {
            Log.d("DataCollector", "${user.lastName} ${user.firstName}: $address: hr=$hr, rrs=$rrIntervals")
            val userData = data.getOrPut(address, { Data(address, user, null) })
            if (userData.startTimestamp == null)
                userData.startTimestamp = System.currentTimeMillis()
            userData.rrs.addAll(rrIntervals)
            Log.d("DataCollector", "userData=$userData")
        }
    }

    fun startRecording(address: String, user: User) {
        pairing.getOrPut(address) {
            data[address] = Data(address, user, null)
            user
        }
    }

    fun resetPairing() {
        pairing.clear()
        data.clear()
    }

    fun getUser(address: String) = pairing[address]

    data class Data(val address: String, val user: User, var startTimestamp: Long?, val rrs: MutableList<Int> = ArrayList())
}
