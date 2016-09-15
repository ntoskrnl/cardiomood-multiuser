package com.cardiomood.group.api

data class RealTimeUploadChunk(
        val userId: String,
        val startTimestamp: Long,
        val rrs: List<Int>,
        val times: List<Long>
)