plugins {
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "io.signallq.app.feature.home"
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

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    filter {
        exclude("**/*.kts")
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom("$rootDir/config/detekt.yml")
    baseline = file("$rootDir/config/detekt-baseline.xml")
}

ktlint {
    version = "1.3.1"
    android = true
    ignoreFailures = false
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}