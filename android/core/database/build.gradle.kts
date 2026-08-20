plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "io.signallq.app.core.database"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // GH#1707 (Task 2.0.09e) — sem isso, MigrationTestHelper nunca encontra os schemas
    // exportados (`$projectDir/schemas`, ver bloco `kapt` abaixo) na APK de teste instrumentado,
    // e TODO Migration*Test deste módulo falha com FileNotFoundException ao rodar de verdade
    // (`connectedDebugAndroidTest`) — achado ao validar esta fatia, pré-existente, nenhum dos
    // Migration*Test anteriores tinha rodado com sucesso contra um dispositivo/emulador real
    // (não há job de CI que execute `connectedAndroidTest`).
    sourceSets {
        getByName("androidTest") {
            assets.srcDirs("$projectDir/schemas")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    api(libs.androidx.room.runtime)
    api(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.room.testing)
    // Dependencia pre-existente faltante (`runTest`/Flow.first() usados por
    // ChatSessionDaoTest.kt, ja em main, nunca compilavam antes deste ajuste) —
    // corrigido de passagem ao validar Migration15Para16Test.kt (GH#1228 Fase 3),
    // mesmo pacote/source-set desta fatia.
    androidTestImplementation(libs.kotlinx.coroutines.test)
}

kapt {
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.incremental", "true")
    }
}
