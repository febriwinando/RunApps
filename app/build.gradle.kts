plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "tech.id.runappsandroid"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "tech.id.runappsandroid"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.google.maps.services)
    implementation(libs.circleimageview)
    implementation(libs.shapeofview)
    implementation(libs.glide)
    implementation(libs.play.services.location)
    implementation(libs.play.services.auth.api.phone)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.auth)
    implementation(libs.logging.interceptor)
    implementation(libs.converter.scalars)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.gson)
    implementation(libs.places)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}