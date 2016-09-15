package com.cardiomood.multiuser.screen.entry

import rx.Observable
import rx.functions.Action1

interface EntryView {

    val submitClicks: Observable<Unit>
    val groupCodeInputStream: Observable<String>

    val showNetworkError: Action1<Unit>
    val showNotFoundError: Action1<Unit>
    val clearError: Action1<Unit>
    val enableButton: Action1<Boolean>

    fun showProgress()
    fun hideProgress()

}