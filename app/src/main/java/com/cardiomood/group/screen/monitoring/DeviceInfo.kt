package com.cardiomood.group.screen.monitoring

import com.cardiomood.group.api.User

data class DeviceInfo(val address: String, val status: DeviceStatus, val lastHeartRate: Int, val user: User? = null)