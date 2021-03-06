package ru.test.multydevicetest

import android.os.SystemClock
import android.util.Log
import com.cardiomood.multiuser.api.Api
import com.cardiomood.multiuser.api.DataRequest
import com.cardiomood.multiuser.api.RealTimeUploadChunk
import com.cardiomood.multiuser.api.User
import ru.test.multydevicetest.bluetooth.HeartRateListener
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.lang.kotlin.filterNotNull
import rx.lang.kotlin.onErrorReturnNull
import rx.schedulers.Schedulers
import rx.subscriptions.CompositeSubscription
import timber.log.Timber
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
                uptime - userData.startTimestamp
                var lastTimestamp = (uptime - userData.startTimestamp).coerceAtLeast(userData.lastTimestamp + 1) - 1
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

    fun getUser(address: String) = synchronized(data) {
        data[address]?.user
    }

    private fun startRealTimeUpload() {
        subscription.addAll(
                Observable.interval(2, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                        .onBackpressureLatest()
                        .flatMap({ submitChunk() }, 1)
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

    private fun submitChunk(): Observable<List<RealTimeUploadChunk>?> {
        val request = prepareUploadChunk()
        return when {
            request.isNotEmpty() -> {
                api.uploadRealTime(DataRequest(request))
                        .subscribeOn(Schedulers.io())
                        .map { request }
                        .doOnError {
                            Timber.i(it, "Failed to send data")
                        }
                        .onErrorReturnNull()
            }
            else -> Observable.empty()
        }
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
                                .apply {
                                    assert(it.rrs.size == it.times.size) {
                                        "Data points rrs and times mismatch"
                                    }
                                }
                    }
                    .filter { it.rrs.isNotEmpty() }
        }
    }

    private fun removeSentData(request: List<RealTimeUploadChunk>) {
        synchronized(data) {
            request.forEach { point ->
                val userData = data.values.firstOrNull { it.user.id == point.userId }
                if (userData != null) {
                    if (userData.rrs.size < point.rrs.size) {
                        Timber.d("Inconsistent state for user ${userData.user.email}: rrs = ${userData.rrs} vs ${point.rrs}")
                    }
                    if (userData.times.size < point.times.size) {
                        Timber.d("Inconsistent state for user ${userData.user.email}: times = ${userData.times} vs ${point.times}")
                    }
                    for (i in 1..point.rrs.size)
                        userData.rrs.removeAt(0)
                    for (i in 1..point.times.size)
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