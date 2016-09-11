package com.cardiomood.group.screen.monitoring

import com.cardiomood.group.api.GroupInfo
import com.cardiomood.group.mvp.BasePresenter
import com.jakewharton.rxrelay.BehaviorRelay
import rx.android.schedulers.AndroidSchedulers

class GroupMonitoringPresenter(groupState: BehaviorRelay<GroupInfo>) : BasePresenter<GroupMonitoringView, Nothing>() {

    private val userListStream = presenterStream { groupState.map { it.users }.replay(1) }
    private val titleStream = presenterStream {
        groupState.map { it.group.name }.replay(1)
    }

    override fun attachView(view: GroupMonitoringView) {
        viewSubscription.addAll(
                userListStream.observeOn(AndroidSchedulers.mainThread())
                        .subscribe(view.updateContent),
                titleStream.observeOn(AndroidSchedulers.mainThread())
                        .subscribe(view.updateTitle)
        )
        super.attachView(view)
    }

}