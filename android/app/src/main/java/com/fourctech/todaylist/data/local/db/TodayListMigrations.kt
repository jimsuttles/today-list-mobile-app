package com.fourctech.todaylist.data.local.db

import androidx.room.migration.Migration

/**
 * Registry for Room schema migrations.
 * Version 1 is the initial schema; add Migration(from, to) entries as the schema evolves.
 * Destructive fallback is intentionally not used.
 */
object TodayListMigrations {
    val ALL: Array<Migration> = emptyArray<Migration>()
}
