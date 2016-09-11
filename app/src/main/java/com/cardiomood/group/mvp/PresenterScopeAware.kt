package com.cardiomood.group.mvp

interface PresenterScopeAware {

    val presenterScopes: MutableMap<String, PresenterContainer>

}