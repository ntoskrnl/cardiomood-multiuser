package com.cardiomood.multiuser.mvp

interface PresenterScopeAware {

    val presenterScopes: MutableMap<String, PresenterContainer>

}