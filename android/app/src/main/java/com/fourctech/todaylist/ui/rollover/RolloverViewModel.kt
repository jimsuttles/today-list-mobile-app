package com.fourctech.todaylist.ui.rollover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fourctech.todaylist.core.analytics.Analytics
import com.fourctech.todaylist.core.analytics.AnalyticsEvents
import com.fourctech.todaylist.core.analytics.AnalyticsParams
import com.fourctech.todaylist.domain.model.Task
import com.fourctech.todaylist.domain.model.TaskLocation
import com.fourctech.todaylist.domain.rollover.RolloverEvaluation
import com.fourctech.todaylist.domain.rollover.RolloverManager
import com.fourctech.todaylist.domain.rollover.RunDailyRolloverUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class RolloverReviewUiState(
    val tasks: List<Task> = emptyList(),
    val decisions: Map<String, TaskLocation> = emptyMap(),
    val missedDays: Long = 1,
    val submitting: Boolean = false,
)

@HiltViewModel
class RolloverViewModel @Inject constructor(
    private val rolloverManager: RolloverManager,
    private val runDailyRollover: RunDailyRolloverUseCase,
    private val analytics: Analytics,
) : ViewModel() {

    private val evaluateMutex = Mutex()
    private val _review = MutableStateFlow<RolloverReviewUiState?>(null)
    val review: StateFlow<RolloverReviewUiState?> = _review.asStateFlow()

    fun onForeground() {
        viewModelScope.launch {
            evaluateMutex.withLock {
                if (_review.value != null) return@withLock
                when (val result = rolloverManager.evaluate()) {
                    is RolloverEvaluation.NeedsReview -> {
                        _review.value = RolloverReviewUiState(
                            tasks = result.unfinished,
                            decisions = result.unfinished.associate { it.id to TaskLocation.TODAY },
                            missedDays = result.missedDays,
                        )
                        analytics.log(
                            AnalyticsEvents.ROLLOVER_REVIEW_SHOWN,
                            mapOf(
                                AnalyticsParams.TASK_COUNT to result.unfinished.size,
                                AnalyticsParams.MISSED_DAYS to result.missedDays,
                            ),
                        )
                    }
                    is RolloverEvaluation.AppliedSilently -> {
                        val taskCount = result.keptOnTodayCount + result.movedToLaterCount
                        if (taskCount > 0) {
                            analytics.log(
                                AnalyticsEvents.ROLLOVER_AUTO_APPLIED,
                                mapOf(
                                    AnalyticsParams.TASK_COUNT to taskCount,
                                    AnalyticsParams.KEPT_TODAY_COUNT to result.keptOnTodayCount,
                                    AnalyticsParams.MOVED_LATER_COUNT to result.movedToLaterCount,
                                ),
                            )
                        }
                    }
                    is RolloverEvaluation.UpToDate -> Unit
                }
            }
        }
    }

    fun setDecision(taskId: String, location: TaskLocation) {
        _review.update { current ->
            current?.copy(decisions = current.decisions + (taskId to location))
        }
    }

    fun keepAllToday() {
        _review.update { current ->
            current ?: return@update null
            current.copy(decisions = current.tasks.associate { it.id to TaskLocation.TODAY })
        }
    }

    fun moveAllLater() {
        _review.update { current ->
            current ?: return@update null
            current.copy(decisions = current.tasks.associate { it.id to TaskLocation.LATER })
        }
    }

    fun confirm() {
        val current = _review.value ?: return
        viewModelScope.launch {
            _review.update { it?.copy(submitting = true) }
            runDailyRollover.applyDecisions(current.decisions)
            val kept = current.decisions.values.count { it == TaskLocation.TODAY }
            val moved = current.decisions.values.count { it == TaskLocation.LATER }
            analytics.log(
                AnalyticsEvents.ROLLOVER_REVIEW_COMPLETED,
                mapOf(
                    AnalyticsParams.TASK_COUNT to current.tasks.size,
                    AnalyticsParams.KEPT_TODAY_COUNT to kept,
                    AnalyticsParams.MOVED_LATER_COUNT to moved,
                    AnalyticsParams.MISSED_DAYS to current.missedDays,
                ),
            )
            _review.value = null
        }
    }
}
