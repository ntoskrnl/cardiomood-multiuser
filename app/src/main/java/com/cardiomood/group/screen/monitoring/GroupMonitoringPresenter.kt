package com.cardiomood.group.screen.monitoring

import com.cardiomood.group.api.GroupInfo
import com.cardiomood.group.mvp.BasePresenter
import com.jakewharton.rxrelay.BehaviorRelay
import rx.Observable
import rx.android.schedulers.AndroidSchedulers

class GroupMonitoringPresenter(groupState: BehaviorRelay<GroupInfo>) : BasePresenter<GroupMonitoringView, Nothing>() {

    private val allDevices = viewStream { view -> view.devices }

    private val itemClicks = viewStream { view -> view.itemClicks }

    private val deviceSelections = viewStream { view -> view.deviceSelections }

    private val backPresses = viewStream { view -> view.backPresses }

    private val clearPairingRequests = viewStream { view -> view.clearPairingRequests }

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
        Observable.combineLatest(
                userListStream,
                deviceCache,
                { users, devices ->
                    val t = devices.values.filter { it.user != null }.associate { it.user to it }
                    users.map { MonitoredUser(it, t[it]?.status ?: DeviceStatus.NONE, t[it]?.address, t[it]?.lastHeartRate ?: 0) }
                }
        )
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

    private val backReactionStream = presenterStream {
        backPresses.withLatestFrom(userStateStream) { click, users ->
            users.filter { it.status == DeviceStatus.NONE }
        }
                .map {
                    when {
                        it.isEmpty() -> GoBackResolution.GO_TO_ENTRY
                        else -> GoBackResolution.FINISH
                    }
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
                        .subscribe(view.selectDevice),
                backReactionStream.observeOn(AndroidSchedulers.mainThread())
                        .subscribe(view.goBack),
                deviceSelections.observeOn(AndroidSchedulers.mainThread())
                        .subscribe(view.pairDevice),
                clearPairingRequests.observeOn(AndroidSchedulers.mainThread())
                        .subscribe(view.resetPairing)
        )
        super.attachView(view)
    }

}

enum class GoBackResolution {
    FINISH,
    GO_TO_ENTRY
}