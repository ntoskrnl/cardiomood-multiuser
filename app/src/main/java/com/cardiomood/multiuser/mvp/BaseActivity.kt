package com.cardiomood.multiuser.mvp

import android.os.Bundle
import com.trello.navi.component.support.NaviAppCompatActivity

open class BaseActivity : NaviAppCompatActivity() {

    protected lateinit var scopeKey: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        scopeKey = when (savedInstanceState) {
            null -> "${javaClass.canonicalName}.${hashCode()}"
            else -> savedInstanceState.getString("activity_scope_key")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("activity_scope_key", scopeKey)
    }

    fun <P : Presenter<*, *>> getOrCreatePresenter(key: String, creator: () -> P): P {
        @Suppress("UNCHECKED_CAST")
        return (application as PresenterScopeAware).presenterScopes
                .getOrPut(scopeKey, { PresenterContainer(scopeKey) })
                .getOrCreate(key, creator) as P
    }

    override fun onDestroy() {
        if (isFinishing) {
            // destroy scope
            (application as PresenterScopeAware).presenterScopes
                    .remove(scopeKey)
                    ?.destroyAll()
        }
        super.onDestroy()
    }

}