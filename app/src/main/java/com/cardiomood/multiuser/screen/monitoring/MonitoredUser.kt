package com.cardiomood.multiuser.screen.monitoring

import com.cardiomood.multiuser.api.User

data class MonitoredUser(
        val user: User,
        val status: DeviceStatus,
        val address: String?,
        val heartRate: Int = 0
)