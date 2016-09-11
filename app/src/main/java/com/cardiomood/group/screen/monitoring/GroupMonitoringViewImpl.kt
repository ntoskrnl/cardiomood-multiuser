package com.cardiomood.group.screen.monitoring

import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import com.cardiomood.group.R
import com.cardiomood.group.api.User
import rx.functions.Action1

class GroupMonitoringViewImpl(view: View, private val activity: AppCompatActivity) : GroupMonitoringView {

    private val recycler = view.findViewById(R.id.users_list) as RecyclerView
    private val adapter = UserListAdapter()

    override val updateTitle = Action1<String> {
        activity.title = "Group: $it"
    }

    override val updateContent = Action1<List<User>> {
        adapter.updateContent(it)
    }

    init {
        recycler.setHasFixedSize(true)
        recycler.layoutManager = LinearLayoutManager(view.context)
        recycler.adapter = adapter
    }

}