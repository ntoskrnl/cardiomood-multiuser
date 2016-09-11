package com.cardiomood.group.mvp

import android.support.annotation.CallSuper
import com.jakewharton.rxrelay.PublishRelay
import com.jakewharton.rxrelay.Relay
import rx.Observable
import rx.observables.ConnectableObservable
import rx.subscriptions.CompositeSubscription

open class BasePresenter<V, R> : Presenter<V, R> {

    protected val viewSubscription = CompositeSubscription()
    protected val routerSubscription = CompositeSubscription()
    protected val presenterSubscription = CompositeSubscription()

    private val viewStreams = mutableListOf<Pair<Relay<Any?, Any?>, (Any?) -> Observable<*>>>()
    private val routerStreams = mutableListOf<Pair<Relay<Any?, Any?>, (Any?) -> Observable<*>>>()
    private val presenterStreams = mutableListOf<ConnectableObservable<*>>()

    protected val viewAttaches: PublishRelay<Unit> = PublishRelay.create()


    @CallSuper
    override fun create() {
        presenterStreams.forEach { presenterSubscription.add(it.connect()) }
    }

    @CallSuper
    override fun attachRouter(router: R) {
        routerStreams.forEach {
            val (relay, mapper) = it
            routerSubscription.add(mapper.invoke(router).subscribe { relay.call(it) })
        }
    }

    @CallSuper
    override fun attachView(view: V) {
        viewStreams.forEach {
            val (relay, mapper) = it
            viewSubscription.add(mapper.invoke(view).subscribe { relay.call(it) })
        }
        viewAttaches.call(Unit)
    }

    @CallSuper
    override fun detachView() = viewSubscription.clear()

    @CallSuper
    override fun detachRouter() = routerSubscription.clear()

    @CallSuper
    override fun destroy() = presenterSubscription.unsubscribe()

    fun <T> viewStream(relay: Relay<T, T> = PublishRelay.create(), mapper: (V) -> Observable<T>): Observable<T> {
        @Suppress("UNCHECKED_CAST")
        viewStreams.add(Pair(relay as Relay<Any?, Any?>, mapper as (Any?) -> Observable<*>))
        return relay
    }

    fun <T> routerStream(relay: Relay<T, T> = PublishRelay.create(), mapper: (R) -> Observable<T>): Observable<T> {
        @Suppress("UNCHECKED_CAST")
        routerStreams.add(Pair(relay as Relay<Any?, Any?>, mapper as (Any?) -> Observable<*>))
        return relay
    }

    fun <T> presenterStream(provider: () -> ConnectableObservable<T>): Observable<T> {
        val stream = provider.invoke()
        presenterStreams.add(stream)
        return stream
    }

}