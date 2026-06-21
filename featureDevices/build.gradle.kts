plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    alias(libs.plugins.hilt)
}

android {
    namespace = "io.veloo.app.feature.devices"
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
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // AndroidNetworkTools — Apache-2.0 — ping nativo sem root + ARP lookup
    // Veja THIRD_PARTY_NOTICES.md para atribuição completa de licença.
    implementation("com.github.stealthcopter:AndroidNetworkTools:0.4.5.3")
    // jmDNS — Apache-2.0 — descoberta mDNS/Bonjour com suporte a TXT records
    // Substitui o parser mDNS artesanal por implementação robusta.
    // Veja THIRD_PARTY_NOTICES.md para atribuição completa de licença.
    implementation("org.jmdns:jmdns:3.6.3")
    // OkHttp — Apache-2.0 — fetch do XML de descrição UPnP/SSDP (LOCATION header)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation(project(":coreDatabase"))
    implementation(project(":coreDatastore"))
    implementation(project(":coreNetwork"))
    implementation(libs.timber)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
