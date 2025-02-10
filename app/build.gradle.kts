plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    namespace = "com.zynksoftware.documentscannersample"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.zynksoftware.documentscannersample"
        minSdk = 26
        targetSdk = 35
        versionCode = 9
        versionName = "1.2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("debug") {
            manifestPlaceholders["enableCrashReporting"] = "false"
        }
        getByName("release") {
            manifestPlaceholders["enableCrashReporting"] = "true"
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
        viewBinding = true
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(libs.kotlin.stdlib)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.glide)
    implementation(libs.kpermissions)
    implementation(libs.rxandroid)
    implementation(libs.viewpager2)
    implementation(libs.photo.view)
    implementation(project(":DocumentScanner"))
}
