package com.cardiomood.group.screen.monitoring

import android.content.Context
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.cardiomood.group.R
import com.cardiomood.group.api.User
import com.jakewharton.rxrelay.PublishRelay
import ru.test.multydevicetest.DeviceService
import ru.test.multydevicetest.bluetooth.SensorDevice
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.functions.Action1
import rx.subscriptions.CompositeSubscription
import java.util.concurrent.TimeUnit

class GroupMonitoringViewImpl(view: View, private val activity: GroupMonitoringActivity) : GroupMonitoringView {

    private val recycler = view.findViewById(R.id.users_list) as RecyclerView
    private val adapter = UserListAdapter()

    override val itemClicks: Observable<MonitoredUser> = adapter.itemClicks()

    override val deviceSelections = PublishRelay.create<Pair<User, String>>()

    override val updateTitle = Action1<String> {
        activity.title = "Group: $it"
    }

    override val updateContent = Action1<List<MonitoredUser>> {
        adapter.updateContent(it)
    }

    override val selectDevice = Action1<User> { user ->
        activity.doScan(true)
        activity.bluetoothLeService?.let { service ->
            val adapter = DeviceListAdapter(view.context, service)
            AlertDialog.Builder(view.context)
                    .setTitle("Select device")
                    .setAdapter(adapter) { dlg, index ->
                        deviceSelections.call(user to adapter.getItem(index).address)
                        dlg.dismiss()
                    }
                    .setOnDismissListener {
                        adapter.stopUpdates()
                    }
                    .create()
                    .show()

            adapter.startUpdates()
        }
    }

    init {
        recycler.setHasFixedSize(true)
        recycler.layoutManager = LinearLayoutManager(view.context)
        recycler.adapter = adapter
    }

    private class DeviceListAdapter(val context: Context, val service: DeviceService) : BaseAdapter() {

        private var items = listOf<SensorDevice>()

        private val subscription = CompositeSubscription()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val itemView = convertView ?:
                    LayoutInflater.from(context).inflate(R.layout.item_device, parent, false)
            val deviceName = itemView.findViewById(R.id.device_name) as TextView
            val status = itemView.findViewById(R.id.status) as TextView
            val item = getItem(position)
            deviceName.text = "${item.name} (${item.address})"
            status.text = item.status

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
                            }
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