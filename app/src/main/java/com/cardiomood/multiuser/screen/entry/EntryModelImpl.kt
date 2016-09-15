package com.cardiomood.multiuser.screen.entry

import com.cardiomood.multiuser.api.Api
import com.cardiomood.multiuser.api.GroupRequest

class EntryModelImpl(private val api: Api) : EntryModel {

    override fun getGroupByCode(groupCode: String) = api.getGroup(GroupRequest(groupCode)).map { it.result }

}