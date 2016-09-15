package ru.test.multydevicetest

import android.os.SystemClock
import android.util.Log
import com.cardiomood.group.api.Api
import com.cardiomood.group.api.DataRequest
import com.cardiomood.group.api.RealTimeUploadChunk
import com.cardiomood.group.api.User
import ru.test.multydevicetest.bluetooth.HeartRateListener
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.lang.kotlin.filterNotNull
import rx.lang.kotlin.onErrorReturnNull
import rx.schedulers.Schedulers
import rx.subscriptions.CompositeSubscription
import java.util.*
import java.util.concurrent.TimeUnit

internal class DataCollector(private val api: Api) : HeartRateListener {

    val data = mutableMapOf<String, Data>()

    val subscription = CompositeSubscription()

    override fun onDataReceived(address: String, hr: Int, rrIntervals: List<Int>) {
        synchronized(data) {
            val user = getUser(address)
            if (user != null) {
                Log.d("DataCollector", "${user.lastName} ${user.firstName}: $address: hr=$hr, rrs=$rrIntervals")
                val userData = data.getOrPut(address, { Data(address, user) })
                val uptime = SystemClock.uptimeMillis()
                if (userData.startSystemTimestamp == 0L) {
                    userData.startSystemTimestamp = System.currentTimeMillis()
                    userData.startTimestamp = uptime
                }
                userData.rrs.addAll(rrIntervals)
                var lastTimestamp = userData.lastTimestamp
                rrIntervals.forEach {
                    userData.times.add(lastTimestamp)
                    lastTimestamp += it
                }
                userData.lastTimestamp = lastTimestamp

                Log.d("DataCollector", "userData=$userData")
            }
        }
    }

    fun startRecording(address: String, user: User) {
        synchronized(data) {
            if (data.isEmpty()) {
                startRealTimeUpload()
            }
            if (!data.containsKey(address)) {
                data[address] = Data(address, user)
            }
        }
    }

    fun resetPairing() {
        synchronized(data) {
            stopRealTimeUpload()
            data.clear()
        }
    }

    fun getUser(address: String) = data[address]?.user

    private fun startRealTimeUpload() {
        subscription.addAll(
                Observable.interval(2, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                        .map { prepareUploadChunk() }
                        .onBackpressureLatest()
                        .flatMap { request ->
                            api.uploadRealTime(DataRequest(request))
                                    .subscribeOn(Schedulers.io())
                                    .map { request }
                                    .doOnError {
                                        it.printStackTrace()
                                    }
                                    .onErrorReturnNull()
                        }
                        .filterNotNull()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe {
                            removeSentData(it)
                        }
        )
    }

    private fun stopRealTimeUpload() {
        subscription.clear()
    }

    private fun prepareUploadChunk(): List<RealTimeUploadChunk> {
        return synchronized(data) {
            data.values
                    .filter { it.startSystemTimestamp > 0 }
                    .map {
                        RealTimeUploadChunk(
                                userId = it.user.id,
                                startTimestamp = it.startSystemTimestamp,
                                rrs = it.rrs.take(50),
                                times = it.times.take(50)
                        )
                    }
        }
    }

    private fun removeSentData(request: List<RealTimeUploadChunk>) {
        synchronized(data) {
            request.forEach { point ->
                val userData = data.values.firstOrNull { it.user.id == point.userId }
                if (userData != null) {
                    for (i in 0..point.rrs.size-1)
                        userData.rrs.removeAt(0)
                    for (i in 0..point.times.size-1)
                        userData.times.removeAt(0)
                }
            }
        }
    }


    data class Data(
            val address: String,
            val user: User,
            var startSystemTimestamp: Long = 0,
            var startTimestamp: Long = 0,
            var lastTimestamp: Long = 0,
            val rrs: MutableList<Int> = ArrayList(),
            val times: MutableList<Long> = ArrayList()
    )
}


