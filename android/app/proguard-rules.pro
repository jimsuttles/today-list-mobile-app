# Today List — keep rules for future minify / R8

-keep class com.fourctech.todaylist.data.local.entity.** { *; }
-keep class com.fourctech.todaylist.domain.model.** { *; }

# Glance / widgets
-keep class com.fourctech.todaylist.widget.** { *; }

# Billing / ads reflection surfaces
-keep class com.android.vending.billing.** { *; }
