package com.fourctech.todaylist.domain.model

enum class DeleteScope {
    /** Delete only this task; leave the recurrence rule if unused. */
    THIS_TASK,

    /** Delete this task and its recurrence series. */
    ENTIRE_SERIES,
}
