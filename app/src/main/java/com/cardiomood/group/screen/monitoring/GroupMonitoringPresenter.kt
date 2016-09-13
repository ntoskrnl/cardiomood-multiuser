package com.cardiomood.group.screen.monitoring

import com.cardiomood.group.api.GroupInfo
import com.cardiomood.group.api.User
import com.cardiomood.group.mvp.BasePresenter
import com.jakewharton.rxrelay.BehaviorRelay
import rx.Observable
import rx.android.schedulers.AndroidSchedulers

class GroupMonitoringPresenter(groupState: BehaviorRelay<GroupInfo>) : BasePresenter<GroupMonitoringView, Nothing>() {

    private val allDevices = viewStream { view -> view.devices }

    private val itemClicks = viewStream { view -> view.itemClicks }

    private val deviceSelections = viewStream { view -> view.deviceSelections }

    private val userListStream = presenterStream { groupState.map { it.users }.replay(1) }

    private val titleStream = presenterStream {
        groupState.map { it.group.name }.replay(1)
    }

    private val deviceCache = presenterStream {
        allDevices.startWith(emptyList<DeviceInfo>())
                .map { it.associate { it.address to it } }
                .replay(1)
    }

    private val userStateStream = presenterStream {
        userListStream.map { it.map { Pair<User, String?>(it, null) } }
                .switchMap { users ->
                    Observable.combineLatest(
                            deviceSelections.scan(users) {
                                a, b ->
                                a.map {
                                    when (it.first) {
                                        b.first -> Pair(b.first, b.second)
                                        else -> it.first to it.second
                                    }
                                }
                            },
                            deviceCache,
                            { items, devices ->
                                items.map {
                                    val device = devices[it.second]
                                    MonitoredUser(it.first, device?.status ?: DeviceStatus.NONE, it.second, device?.lastHeartRate ?: 0)
                                }
                            }
                    )
                }
                .replay(1)
    }

    private val shouldChooseDeviceStream = presenterStream {
        itemClicks
                .filter { it.address == null }
                .map { it.user }
                .withLatestFrom(userStateStream) { user, state ->
                    user to state.map { it.address }.filterNotNull()
                }
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