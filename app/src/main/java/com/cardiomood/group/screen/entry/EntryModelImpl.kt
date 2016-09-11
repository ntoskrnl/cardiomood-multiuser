package com.cardiomood.group.screen.entry

import com.cardiomood.group.api.Api
import com.cardiomood.group.api.GroupRequest

class EntryModelImpl(private val api: Api) : EntryModel {

    override fun getGroupByCode(groupCode: String) = api.getGroup(GroupRequest(groupCode)).map { it.result }

}