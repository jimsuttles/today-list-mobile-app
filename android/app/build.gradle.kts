plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

import java.util.Properties

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { stream -> localProperties.load(stream) }
}

fun propOrEnv(name: String): String? {
    val fromFile = localProperties.getProperty(name)
    if (!fromFile.isNullOrBlank()) return fromFile
    val fromEnv = System.getenv(name)
    return if (!fromEnv.isNullOrBlank()) fromEnv else null
}

// Google sample IDs — safe for debug / until production units exist.
val testAdMobAppId = "ca-app-pub-3940256099942544~3347511713"
val testAdMobBannerUnitId = "ca-app-pub-3940256099942544/6300978111"
val releaseAdMobAppId = propOrEnv("ADMOB_APP_ID") ?: testAdMobAppId
val releaseAdMobBannerUnitId = propOrEnv("ADMOB_BANNER_UNIT_ID") ?: testAdMobBannerUnitId

val signStoreFile = propOrEnv("TL_SIGN_STORE_FILE")
val signKeyAlias = propOrEnv("TL_SIGN_KEY_ALIAS")
val signStorePassword = propOrEnv("TL_SIGN_STORE_PASSWORD")
val signKeyPassword = propOrEnv("TL_SIGN_KEY_PASSWORD")
val hasReleaseSigning =
    !signStoreFile.isNullOrBlank() &&
        !signKeyAlias.isNullOrBlank() &&
        !signStorePassword.isNullOrBlank() &&
        !signKeyPassword.isNullOrBlank()

android {
    namespace = "com.fourctech.todaylist"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.fourctech.todaylist"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "PUBLISHER_NAME", "\"4CTech, LLC\"")
        buildConfigField("String", "REMOVE_ADS_PRODUCT_ID", "\"com.fourctech.todaylist.removeads\"")
        buildConfigField("String", "ADMOB_BANNER_UNIT_ID", "\"$testAdMobBannerUnitId\"")
        manifestPlaceholders["admobAppId"] = testAdMobAppId

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(signStoreFile!!)
                storePassword = signStorePassword
                keyAlias = signKeyAlias
                keyPassword = signKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "ADMOB_BANNER_UNIT_ID", "\"$testAdMobBannerUnitId\"")
            manifestPlaceholders["admobAppId"] = testAdMobAppId
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "ADMOB_BANNER_UNIT_ID", "\"$releaseAdMobBannerUnitId\"")
            manifestPlaceholders["admobAppId"] = releaseAdMobAppId
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.01.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation("com.google.dagger:hilt-android:2.54")
    ksp("com.google.dagger:hilt-compiler:2.54")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.glance:glance-material3:1.1.1")

    val firebaseBom = platform("com.google.firebase:firebase-bom:33.9.0")
    implementation(firebaseBom)
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")

    implementation("com.google.android.gms:play-services-ads:23.6.0")
    implementation("com.google.android.ump:user-messaging-platform:3.1.0")
    implementation("com.android.billingclient:billing-ktx:7.1.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("com.google.truth:truth:1.4.4")
    testImplementation("androidx.room:room-testing:2.6.1")
    testImplementation("androidx.test:core-ktx:1.6.1")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.robolectric:robolectric:4.14.1")
}

// Apply only when a real Firebase config is present (copy from Console → android/app/).
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
}
