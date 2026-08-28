package com.fourctech.todaylist.core.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemClockProvider @Inject constructor() : ClockProvider {
    override fun now(): Instant = Instant.now()

    override fun today(): LocalDate = LocalDate.now(zoneId())

    override fun zoneId(): ZoneId = ZoneId.systemDefault()
}
