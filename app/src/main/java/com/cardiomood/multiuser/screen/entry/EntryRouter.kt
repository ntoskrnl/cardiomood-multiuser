package com.cardiomood.multiuser.screen.entry

import com.cardiomood.multiuser.api.GroupInfo

interface EntryRouter {

    fun gotoMainScreen(data: GroupInfo, noAnimation: Boolean = false)

    fun finish()

}