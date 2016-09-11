package com.cardiomood.group.api

import io.mironov.smuggler.AutoParcelable

data class User(val id: String,
                val email: String?,
                val firstName: String?,
                val lastName: String?,
                val userRole: String?,
                val avatar: String?) : AutoParcelable