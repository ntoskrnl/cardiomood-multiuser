package com.cardiomood.multiuser.screen.monitoring

import com.cardiomood.multiuser.api.User

data class DeviceInfo(val address: String, val status: DeviceStatus, val lastHeartRate: Int, val user: User? = null)