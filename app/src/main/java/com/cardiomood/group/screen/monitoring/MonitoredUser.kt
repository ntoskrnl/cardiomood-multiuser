package com.cardiomood.group.screen.monitoring

import com.cardiomood.group.api.User

data class MonitoredUser(
        val user: User,
        val status: DeviceStatus,
        val address: String?,
        val heartRate: Int = 0
)