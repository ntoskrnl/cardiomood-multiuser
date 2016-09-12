package com.cardiomood.group.screen.monitoring

import com.cardiomood.group.api.User
import rx.Observable
import rx.functions.Action1

interface GroupMonitoringView {

    val itemClicks: Observable<MonitoredUser>

    val deviceSelections: Observable<Pair<User, String>>

    val updateContent: Action1<List<MonitoredUser>>

    val updateTitle: Action1<String>

    val selectDevice: Action1<User>

}