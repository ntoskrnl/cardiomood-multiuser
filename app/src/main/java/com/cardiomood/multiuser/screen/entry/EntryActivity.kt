package com.cardiomood.multiuser.screen.entry

import android.os.Bundle
import com.cardiomood.multiuser.R
import com.cardiomood.multiuser.api.GroupInfo
import com.cardiomood.multiuser.mvp.BaseActivity
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.KodeinInjector
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import com.github.salomonbrys.kodein.singleton
import com.jakewharton.rxrelay.BehaviorRelay

class EntryActivity : BaseActivity() {

    private val appInjector = KodeinInjector()
    private val injector = KodeinInjector()

    private val groupInfo by appInjector.instance<BehaviorRelay<GroupInfo?>>()
    private val router by appInjector.instance<EntryRouter>()
    private val presenter by injector.instance<EntryPresenter>()
    private val view by injector.instance<EntryView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appInjector.inject(diConfig())

        if (groupInfo.value != null) {
            router.gotoMainScreen(groupInfo.value!!, true)
            finish()
            return
        }

        setContentView(R.layout.activity_entry)
        injector.inject(diConfig())


        presenter.attachRouter(router)
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

        bind<EntryPresenter>() with singleton {
            getOrCreatePresenter("entry") { EntryPresenter(instance(), instance()) }
        }

        bind<EntryView>() with singleton { EntryViewImpl(window.decorView) }

        bind<EntryRouter>() with singleton { EntryRouterImpl(this@EntryActivity) }
    }

}