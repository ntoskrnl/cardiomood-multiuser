package com.cardiomood.group.api

import okhttp3.Interceptor
import okhttp3.Response

class ParseRequestInterceptor(private val appId: String, private val apiKey: String): Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val request = when (original.url().host()) {
            "api.parse.com" -> {
                original.newBuilder()
                        .addHeader("X-Parse-Application-Id", appId)
                        .addHeader("X-Parse-REST-API-Key", apiKey)
                        .build()

            }
            else -> original
        }
        return chain.proceed(request)
    }

}