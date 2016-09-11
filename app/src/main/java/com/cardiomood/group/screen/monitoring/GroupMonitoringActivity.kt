package com.cardiomood.group.screen.monitoring

import android.os.Bundle
import com.cardiomood.group.R
import com.cardiomood.group.mvp.BaseActivity
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.KodeinInjector
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import com.github.salomonbrys.kodein.singleton

class GroupMonitoringActivity : BaseActivity() {

    private val injector = KodeinInjector()
    private val presenter by injector.instance<GroupMonitoringPresenter>()
    private val view by injector.instance<GroupMonitoringView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_monitoring)

        injector.inject(diConfig())
    }

    override fun onResume() {
        super.onResume()
        presenter.attachView(view)
    }

    override fun onPause() {
        presenter.detachView()
        super.onPause()
    }

    private fun diConfig() = Kodein {
        extend(appKodein())

        bind<GroupMonitoringPresenter>() with singleton {
            getOrCreatePresenter("group_monitoring") { GroupMonitoringPresenter(instance()) }
        }

        bind<GroupMonitoringView>() with singleton {
            GroupMonitoringViewImpl(window.decorView, this@GroupMonitoringActivity)
        }
    }
}