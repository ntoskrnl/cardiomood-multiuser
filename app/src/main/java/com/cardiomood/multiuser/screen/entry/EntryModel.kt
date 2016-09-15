package com.cardiomood.multiuser.screen.entry

import com.cardiomood.multiuser.api.GroupInfo
import rx.Observable

interface EntryModel {

    fun getGroupByCode(groupCode: String): Observable<GroupInfo>

}