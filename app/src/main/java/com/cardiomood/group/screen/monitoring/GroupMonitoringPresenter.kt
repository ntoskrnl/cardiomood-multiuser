package com.cardiomood.group.screen.monitoring

import com.cardiomood.group.api.GroupInfo
import com.cardiomood.group.mvp.BasePresenter
import com.jakewharton.rxrelay.BehaviorRelay
import rx.android.schedulers.AndroidSchedulers

class GroupMonitoringPresenter(groupState: BehaviorRelay<GroupInfo>) : BasePresenter<GroupMonitoringView, Nothing>() {

    private val itemClicks = viewStream { view -> view.itemClicks }

    private val deviceSelections = viewStream { view -> view.deviceSelections }

    private val userListStream = presenterStream { groupState.map { it.users }.replay(1) }

    private val titleStream = presenterStream {
        groupState.map { it.group.name }.replay(1)
    }

    private val userStateStream = presenterStream {
        userListStream
                .map { it.map { MonitoredUser(it, DeviceStatus.NONE, null) } }
                .switchMap { users ->
                    deviceSelections.scan(users) {
                        a, b ->
                        a.map {
                            when (it.user) {
                                b.first -> it.copy(address = b.second, status = DeviceStatus.CONNECTING)
                                else -> it
                            }
                        }
                    }
                }
                .replay(1)
    }

    private val shouldChooseDeviceStream = presenterStream {
        itemClicks
                .filter { it.address == null }
                .map { it.user }
                .publish()
    }

    override fun attachView(view: GroupMonitoringView) {
        viewSubscription.addAll(
                userStateStream.observeOn(AndroidSchedulers.mainThread())
                        .subscribe(view.updateContent),
                titleStream.observeOn(AndroidSchedulers.mainThread())
                        .subscribe(view.updateTitle),
                shouldChooseDeviceStream.observeOn(AndroidSchedulers.mainThread())
                        .subscribe(view.selectDevice)
        )
        super.attachView(view)
    }

}