plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "io.linka.app.kotlin.feature.diagnostico"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(project(":featureFibra"))
    implementation(libs.okhttp)
    testImplementation(libs.junit)
    // org.json.JSONObject vem do Android SDK em runtime, mas nao esta disponivel
    // nos unit tests JVM (testDebugUnitTest). Sem este dep, qualquer teste que
    // chame AiDiagnosisRepository.parseResult cai no `catch (Throwable)` e
    // recebe null. Ref: https://stackoverflow.com/q/24197773
    testImplementation("org.json:json:20240303")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
