package com.cardiomood.group.screen.entry

import com.cardiomood.group.api.GroupInfo

interface EntryRouter {

    fun gotoMainScreen(data: GroupInfo, noAnimation: Boolean = false)

    fun finish()

}