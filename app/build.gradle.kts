import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val versionProps = Properties().apply {
    val f = rootProject.file("version.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun prop(name: String): String = (project.findProperty(name) as String?) ?: ""

android {
    namespace = "com.bawabat.alorood"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.bawabat.alorood"
        minSdk = 26
        targetSdk = 36
        versionCode = (versionProps.getProperty("VERSION_CODE") ?: "1").toInt()
        versionName = versionProps.getProperty("VERSION_NAME") ?: "1.0.0"
        buildConfigField("String", "API_BASE_URL", "\"${prop("API_BASE_URL")}\"")
        buildConfigField("boolean", "DEMO_MODE", if (prop("API_BASE_URL").isBlank()) "true" else "false")
        buildConfigField("String", "SUPPORT_EMAIL", "\"${prop("SUPPORT_EMAIL")}\"")
        buildConfigField("String", "SUPPORT_WHATSAPP", "\"${prop("SUPPORT_WHATSAPP")}\"")
        buildConfigField("String", "PRIVACY_URL", "\"${prop("PRIVACY_URL")}\"")
        buildConfigField("String", "TERMS_URL", "\"${prop("TERMS_URL")}\"")
        buildConfigField("String", "ACCOUNT_DELETION_URL", "\"${prop("ACCOUNT_DELETION_URL")}\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        val ksPath = System.getenv("KEYSTORE_PATH")
        if (!ksPath.isNullOrBlank() && file(ksPath).exists()) {
            create("release") {
                storeFile = file(ksPath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    lint { abortOnError = false }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.05.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.navigation:navigation-compose:2.10.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("com.google.android.play:app-update:2.1.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
}
