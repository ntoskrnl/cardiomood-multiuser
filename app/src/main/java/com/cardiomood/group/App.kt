package com.cardiomood.group

import android.app.Application
import com.cardiomood.group.api.Api
import com.cardiomood.group.api.GroupInfo
import com.cardiomood.group.api.ParseRequestInterceptor
import com.cardiomood.group.mvp.PresenterContainer
import com.cardiomood.group.mvp.PresenterScopeAware
import com.cardiomood.group.screen.entry.EntryModel
import com.cardiomood.group.screen.entry.EntryModelImpl
import com.facebook.stetho.Stetho
import com.facebook.stetho.okhttp3.StethoInterceptor
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.KodeinAware
import com.github.salomonbrys.kodein.instance
import com.github.salomonbrys.kodein.singleton
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.jakewharton.rxrelay.BehaviorRelay
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*

class App : Application(), KodeinAware, PresenterScopeAware {

    override val presenterScopes: MutableMap<String, PresenterContainer> by lazy {
        Collections.synchronizedMap<String, PresenterContainer>(mutableMapOf())
    }

    override val kodein: Kodein by lazy {
        Kodein {

            bind<OkHttpClient>() with singleton {
                OkHttpClient.Builder()
                        .addNetworkInterceptor(StethoInterceptor())
                        .addInterceptor(
                                ParseRequestInterceptor(
                                        appId = "SSzU4YxI6Z6SwvfNc2vkZhYQYl86CvBpd3P2wHF1",
                                        apiKey = "pKDap5jqe7lyBG5vTRgvTz7t8AiRWXpMYbuS2oak"
                                )
                        )
                        .build()
            }

            bind<Gson>() with singleton { GsonBuilder().create() }

            bind<Api>() with singleton {
                Retrofit.Builder()
                        .baseUrl("https://api.parse.com/")
                        .client(instance())
                        .addConverterFactory(GsonConverterFactory.create(instance()))
                        .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                        .build()
                        .create(Api::class.java)
            }

            bind<EntryModel>() with singleton { EntryModelImpl(instance()) }

            bind<BehaviorRelay<GroupInfo>>() with singleton { BehaviorRelay.create<GroupInfo>() }

        }
    }

    override fun onCreate() {
        super.onCreate()

        Stetho.initializeWithDefaults(this)
    }

    override fun onTerminate() {
        presenterScopes.forEach { it.value.destroyAll() }
        super.onTerminate()
    }
}