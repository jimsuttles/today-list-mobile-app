package com.fourctech.todaylist.core.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

interface ClockProvider {
    fun now(): Instant
    fun today(): LocalDate
    fun zoneId(): ZoneId
}
