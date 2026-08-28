package com.fourctech.todaylist.core.notifications

import android.content.Intent
import android.net.Uri
import com.fourctech.todaylist.MainActivity
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReminderIntentsTest {
    @Test
    fun taskDeepLink_roundTripsThroughIntentParser() {
        val taskId = "abc-123"
        val uri = Uri.parse(ReminderIntents.taskDeepLink(taskId))
        val intent = Intent(Intent.ACTION_VIEW, uri)

        assertThat(MainActivity.taskIdFromIntent(intent)).isEqualTo(taskId)
    }

    @Test
    fun extraTaskId_isPreferred() {
        val intent = Intent().putExtra(ReminderIntents.EXTRA_TASK_ID, "from-extra")
        assertThat(MainActivity.taskIdFromIntent(intent)).isEqualTo("from-extra")
    }
}
