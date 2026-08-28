package com.fourctech.todaylist.core.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class FakeClockProvider(
    private var instant: Instant = Instant.parse("2026-08-27T12:00:00Z"),
    private var zone: ZoneId = ZoneId.of("UTC"),
) : ClockProvider {
    override fun now(): Instant = instant

    override fun today(): LocalDate = LocalDate.ofInstant(instant, zone)

    override fun zoneId(): ZoneId = zone

    fun setInstant(value: Instant) {
        instant = value
    }
}
