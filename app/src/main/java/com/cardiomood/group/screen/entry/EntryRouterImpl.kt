package com.cardiomood.group.screen.entry

import android.content.Intent
import com.cardiomood.group.api.GroupInfo
import com.cardiomood.group.screen.monitoring.GroupMonitoringActivity
import com.trello.navi.component.support.NaviAppCompatActivity

class EntryRouterImpl(private val context: NaviAppCompatActivity) : EntryRouter {

    override fun gotoMainScreen(data: GroupInfo) {
        context.startActivity(Intent(context, GroupMonitoringActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK))
    }

}