plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
    id("dagger.hilt.android.plugin")
    id("com.google.gms.google-services")
    alias(libs.plugins.google.firebase.crashlytics)
}

android {
    namespace = "com.example.pedometr"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.pedometr"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        viewBinding = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.firestore)
    implementation(libs.androidx.hilt.common)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("com.mikhaellopez:circularprogressbar:3.1.0")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation(libs.okhttp.v4120)
    implementation("com.google.dagger:hilt-android:2.48")
    implementation(libs.androidx.espresso.idling.resource)
    implementation(libs.androidx.uiautomator)
    implementation(libs.androidx.fragment.testing)
    kapt("com.google.dagger:hilt-compiler:2.48")
    implementation(libs.kotlinx.coroutines.android)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("androidx.navigation:navigation-fragment-ktx:2.3.5")
    implementation("androidx.hilt:hilt-navigation-compose:1.0.0")
    implementation("androidx.navigation:navigation-compose:2.5.3")
    implementation("androidx.fragment:fragment-ktx:1.5.2")
    implementation("com.yandex.android:maps.mobile:4.8.1-full")
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("androidx.work:work-runtime-ktx:2.8.1")
    implementation("androidx.hilt:hilt-work:1.0.0")
    kapt("androidx.hilt:hilt-compiler:1.0.0")
    implementation(libs.mockwebserver)

    // Unit tests
    testImplementation(libs.junit)
    testImplementation("org.mockito:mockito-core:5.12.0")
    testImplementation("org.mockito:mockito-inline:5.12.0")

    // Android Instrumentation tests (aligned versions)
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test:runner:1.5.2") // Downgrade to match core:1.5.0
    androidTestImplementation("androidx.test:core:1.5.0") // Explicitly set to 1.5.0
    androidTestImplementation(platform(libs.androidx.compose.bom)) {
        exclude(group ="androidx.test", module = "core")// Exclude to avoid BOM overriding core version
    }
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("org.mockito:mockito-core:5.12.0")
    androidTestImplementation("org.mockito:mockito-android:5.12.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Force consistent versions for androidx.test dependencies
    configurations.all {
        resolutionStrategy {
            force("androidx.test:core:1.5.0")
            force("androidx.test:runner:1.5.2")
            force("androidx.test.ext:junit:1.1.5")
            force("androidx.test.espresso:espresso-core:3.5.1")
        }
    }
}