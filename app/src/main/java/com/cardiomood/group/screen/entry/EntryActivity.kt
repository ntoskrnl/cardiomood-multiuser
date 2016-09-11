package com.cardiomood.group.screen.entry

import android.os.Bundle
import com.cardiomood.group.R
import com.cardiomood.group.mvp.BaseActivity
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.KodeinInjector
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import com.github.salomonbrys.kodein.singleton

class EntryActivity : BaseActivity() {

    private val injector = KodeinInjector()

    private val presenter by injector.instance<EntryPresenter>()
    private val view by injector.instance<EntryView>()
    private val router by injector.instance<EntryRouter>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

    override fun onDestroy() {
        presenter.detachView()
        super.onDestroy()
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