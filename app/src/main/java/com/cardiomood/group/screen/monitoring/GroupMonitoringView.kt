package com.cardiomood.group.screen.monitoring

import com.cardiomood.group.api.User
import com.jakewharton.rxrelay.PublishRelay
import rx.Observable
import rx.functions.Action1

interface GroupMonitoringView {

    val itemClicks: Observable<MonitoredUser>

    val deviceSelections: Observable<Pair<User, String>>

    val devices: Observable<List<DeviceInfo>>

    val backPresses: PublishRelay<Unit>

    val clearPairingRequests: PublishRelay<Unit>

    val updateContent: Action1<List<MonitoredUser>>

    val updateTitle: Action1<String>

    val selectDevice: Action1<Pair<User, List<String>>>

    val goBack: Action1<GoBackResolution>

    val pairDevice: Action1<Pair<User, String>>

    val resetPairing: Action1<Unit>

}