package com.cardiomood.group.api

import io.mironov.smuggler.AutoParcelable

data class Group(val id: String, val ownerId: String, val invitationCode: String, val name: String, val description: String) : AutoParcelable