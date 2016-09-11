package com.cardiomood.group.api

import retrofit2.http.Body
import retrofit2.http.POST
import rx.Observable

interface Api {

    @POST("1/functions/loadGroupByCode")
    fun getGroup(@Body request: GroupRequest): Observable<ApiResponse<GroupInfo>>

}