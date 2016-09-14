package com.cardiomood.group.screen.entry

import android.content.Intent
import com.cardiomood.group.api.GroupInfo
import com.cardiomood.group.screen.monitoring.GroupMonitoringActivity
import com.trello.navi.component.support.NaviAppCompatActivity

class EntryRouterImpl(private val context: NaviAppCompatActivity) : EntryRouter {

    override fun gotoMainScreen(data: GroupInfo, noAnimation: Boolean) {
        val intent = Intent(context, GroupMonitoringActivity::class.java)
        if (noAnimation) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        }
        context.startActivity(intent)
    }

    override fun finish() {
        context.finish()
    }
}