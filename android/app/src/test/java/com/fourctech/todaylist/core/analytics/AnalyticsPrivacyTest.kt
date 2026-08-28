package com.fourctech.todaylist.core.analytics

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class AnalyticsPrivacyTest {
    private val analytics = FakeAnalytics()

    @Test
    fun allowsEnumParamsWithoutUserContent() {
        analytics.log(
            AnalyticsEvents.TASK_COMPLETED,
            mapOf(
                AnalyticsParams.LOCATION to AnalyticsParams.LOCATION_TODAY,
                AnalyticsParams.SOURCE to AnalyticsParams.SOURCE_LIST,
            ),
        )
        assertThat(analytics.events).hasSize(1)
        assertThat(analytics.events.first().name).isEqualTo(AnalyticsEvents.TASK_COMPLETED)
    }

    @Test
    fun rejectsTitleParam() {
        assertThrows(IllegalStateException::class.java) {
            analytics.log(
                AnalyticsEvents.TASK_CREATED,
                mapOf("title" to "Buy milk"),
            )
        }
        assertThat(analytics.events).isEmpty()
    }

    @Test
    fun rejectsNotesParam() {
        assertThrows(IllegalStateException::class.java) {
            analytics.log(
                AnalyticsEvents.TASK_CREATED,
                mapOf("notes" to "secret"),
            )
        }
    }
}
