package com.cardiomood.multiuser.mvp

import android.util.Log

class PresenterContainer(private val name: String) {

    private val presenters = mutableMapOf<String, Presenter<*, *>>()

    init {
        Log.d("ScopeContainer", "Scope created: $name")
    }

    fun getOrCreate(key: String, creator: () -> Presenter<*, *>) =
            synchronized(presenters) {
                presenters.getOrPut(key) {
                    creator().apply { create() }
                }
            }

    fun destroyAll() {
        synchronized(presenters) {
            presenters.forEach { it.value.destroy() }
            presenters.clear()
        }
        Log.d("ScopeContainer", "Scope destroyed: $name")
    }

}