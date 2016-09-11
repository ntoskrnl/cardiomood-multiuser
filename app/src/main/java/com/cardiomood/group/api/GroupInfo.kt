package com.cardiomood.group.api

import io.mironov.smuggler.AutoParcelable

data class GroupInfo(val admin: User, val group: Group, val users: List<User>) : AutoParcelable