package com.cardiomood.group.screen.monitoring

import com.cardiomood.group.api.User
import rx.functions.Action1

interface GroupMonitoringView {

    val updateContent: Action1<List<User>>

    val updateTitle: Action1<String>

}