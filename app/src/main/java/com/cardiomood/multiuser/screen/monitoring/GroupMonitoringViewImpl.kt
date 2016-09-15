package com.cardiomood.multiuser.screen.monitoring

import android.content.Intent
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.cardiomood.multiuser.R
import com.cardiomood.multiuser.api.User
import com.cardiomood.multiuser.screen.entry.EntryActivity
import com.jakewharton.rxrelay.PublishRelay
import com.trello.rxlifecycle.kotlin.bindToLifecycle
import ru.test.multydevicetest.DeviceService
import ru.test.multydevicetest.bluetooth.SensorDevice
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.functions.Action1
import rx.lang.kotlin.filterNotNull
import rx.subscriptions.CompositeSubscription
import java.util.concurrent.TimeUnit

class GroupMonitoringViewImpl(view: View, private val activity: GroupMonitoringActivity) : GroupMonitoringView {

    private val recycler = view.findViewById(R.id.users_list) as RecyclerView
    private val adapter = UserListAdapter()

    override val itemClicks: Observable<MonitoredUser> = adapter.itemClicks()

    override val deviceSelections = PublishRelay.create<Pair<User, String>>()

    override val backPresses: PublishRelay<Unit> = PublishRelay.create()

    override val clearPairingRequests: PublishRelay<Unit> = PublishRelay.create()

    // todo: move this in router
    override val devices = Observable.interval(100, 500, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
            .map {
                activity.bluetoothLeService
            }
            .filterNotNull()
            .map { service ->
                service.allAddresses
                        .map { service.getDevice(it) }
                        .filterNotNull()
                        .map { it.toDeviceInfo(service.getUser(it.address)) }
            }
            .distinctUntilChanged()
            .bindToLifecycle(view)

    override val updateTitle = Action1<String> {
        activity.title = "Group: $it"
    }

    override val updateContent = Action1<List<MonitoredUser>> {
        adapter.updateContent(it)
    }

    override val goBack = Action1<GoBackResolution> {
        @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
        when (it) {
            GoBackResolution.GO_TO_ENTRY -> {
                activity.bluetoothLeService?.doStop(true)
                activity.startActivity(Intent(activity, EntryActivity::class.java))
                activity.finish()
            }
            GoBackResolution.FINISH -> activity.finish()
        }
    }

    override val pairDevice = Action1<Pair<User, String>> {
        val (user, address) = it
        activity.bluetoothLeService?.pairDeviceWithUser(address, user)
    }

    override val resetPairing = Action1<Unit> {
        activity.bluetoothLeService?.clearDevicePairing()
    }

    override val selectDevice = Action1<Pair<User, List<String>>> {
        val (user, usedAddresses) = it
        activity.doScan(true)
        activity.bluetoothLeService?.let { service ->
            val adapter = DeviceListAdapter(view, service, usedAddresses)
            adapter.startUpdates()
            AlertDialog.Builder(view.context)
                    .setTitle("Select device")
                    .setAdapter(adapter) { dlg, index ->
                        val address = adapter.getItem(index).address
                        deviceSelections.call(user to address)
                        dlg.dismiss()
                    }
                    .setOnDismissListener {
                        adapter.stopUpdates()
                    }
                    .create()
                    .show()
        }
    }

    init {
        recycler.setHasFixedSize(true)
        recycler.layoutManager = LinearLayoutManager(view.context)
        recycler.adapter = adapter
    }

    private fun SensorDevice.toDeviceInfo(user: User?) = DeviceInfo(
            address = address,
            status = when {
                isTransmitting || isConnected -> DeviceStatus.CONNECTED
                isConnecting -> DeviceStatus.CONNECTING
                isDisconnected -> DeviceStatus.DISCONNECTED
                else -> DeviceStatus.NONE
            },
            lastHeartRate = when {
                isTransmitting -> lastHeartRate
                else -> 0
            },
            user = user
    )

    private class DeviceListAdapter(val view: View, val service: DeviceService, val usedAddresses: List<String>) : BaseAdapter() {

        private val inflater = LayoutInflater.from(view.context)

        private var items = listOf<SensorDevice>()

        private val subscription = CompositeSubscription()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val itemView = convertView ?: inflater.inflate(R.layout.item_device, parent, false)
            val deviceName = itemView.findViewById(R.id.device_name) as TextView
            val status = itemView.findViewById(R.id.status) as TextView
            val deviceAddress = itemView.findViewById(R.id.device_address) as TextView
            val item = getItem(position)
            deviceName.text = item.name
            deviceAddress.text = item.address
            status.text = when {
                item.isTransmitting -> item.heartRate.toString()
                item.isConnected -> "Connected"
                item.isConnecting -> "Connecting"
                else -> "N/A"
            }
            return itemView
        }

        override fun getItem(position: Int): SensorDevice = items[position]

        override fun getItemId(position: Int): Long = getItem(position).address.hashCode().toLong()

        override fun getCount(): Int = items.size

        fun startUpdates() {
            subscription.addAll(
                    Observable.interval(0, 1000, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
                            .map {
                                service.allAddresses
                                        .map { service.getDevice(it) }
                                        .filterNotNull()
                                        .filterNot { it.address in usedAddresses }
                            }
                            .bindToLifecycle(view)
                            .subscribe {
                                items = it
                                notifyDataSetChanged()
                            }
            )
        }

        fun stopUpdates() {
            subscription.clear()
        }

    }

}