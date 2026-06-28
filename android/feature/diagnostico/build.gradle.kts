plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    alias(libs.plugins.hilt)
}

android {
    namespace = "io.signallq.app.feature.diagnostico"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField(
            "String",
            "AI_WORKER_URL",
            "\"https://linka-ai-diagnosis-worker.giammattey-luiz.workers.dev\"",
        )
        buildConfigField(
            "String",
            "APP_VERSION",
            "\"${libs.versions.versionName.get()}\"",
        )
        buildConfigField(
            "int",
            "VERSION_CODE",
            libs.versions.versionCode.get(),
        )
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(project(":featureSpeedtest"))
    implementation(project(":coreDatabase"))
    implementation(project(":coreDatastore"))
    implementation(project(":coreNetwork"))
    implementation(libs.timber)
    implementation(libs.okhttp)
    implementation(libs.androidx.datastore.preferences)
    testImplementation(libs.junit)
    // org.json.JSONObject vem do Android SDK em runtime, mas nao esta disponivel
    // nos unit tests JVM (testDebugUnitTest). Sem este dep, qualquer teste que
    // chame AiDiagnosisRepository.parseResult cai no `catch (Throwable)` e
    // recebe null. Ref: https://stackoverflow.com/q/24197773
    testImplementation("org.json:json:20240303")
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.room.testing)
}
