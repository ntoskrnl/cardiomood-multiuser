package com.cardiomood.group.api

import com.google.gson.JsonElement
import retrofit2.http.Body
import retrofit2.http.POST
import rx.Observable

interface Api {

    @POST("1/functions/loadGroupByCode")
    fun getGroup(@Body request: GroupRequest): Observable<ApiResponse<GroupInfo>>

    @POST("1/functions/uploadRealtimeCardioPoints")
    fun uploadRealTime(@Body request: DataRequest<List<RealTimeUploadChunk>>): Observable<ApiResponse<JsonElement>>

}