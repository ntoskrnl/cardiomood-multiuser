package com.cardiomood.group.mvp

interface Presenter<in V, in R> {

    fun create()

    fun attachRouter(router: R)

    fun attachView(view: V)

    fun detachView()

    fun detachRouter()

    fun destroy()

}