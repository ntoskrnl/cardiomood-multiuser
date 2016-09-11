package com.cardiomood.group.screen.entry

import com.cardiomood.group.api.GroupInfo
import rx.Observable

interface EntryModel {

    fun getGroupByCode(groupCode: String): Observable<GroupInfo>

}