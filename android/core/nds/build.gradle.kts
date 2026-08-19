import java.util.Properties

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

// NDS-01 (#1744, ADR-017) — Bearer token estatico do Network Diagnostics Service.
// Lido de local.properties em dev (NUNCA commitado — arquivo esta no .gitignore),
// ou da variavel de ambiente NDS_API_TOKEN em CI/release. O valor real deve ser
// preenchido manualmente por quem tiver o token (Luiz ou delegado explicito) —
// nunca escrito em nenhum arquivo versionado deste repositorio. Placeholder vazio
// nao quebra o build: requests reais sem token recebem 401 do NDS, com shape
// conhecido ({"error","message"}) tratado defensivamente por NdsClient.
//
// Para preencher localmente, adicione a `android/local.properties` (arquivo
// gitignorado, nao versionado):
//   NDS_API_TOKEN=<valor real fornecido por quem tem acesso>
private val localPropertiesFile = rootProject.file("local.properties")
private val localProperties =
    Properties().apply {
        if (localPropertiesFile.exists()) load(localPropertiesFile.inputStream())
    }
private val ndsApiToken: String =
    localProperties.getProperty("NDS_API_TOKEN")
        ?: System.getenv("NDS_API_TOKEN")
        ?: ""

android {
    namespace = "io.signallq.app.core.nds"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "NDS_BASE_URL",
            "\"https://network-diagnostics-service.buildealabs.workers.dev\"",
        )
        buildConfigField(
            "String",
            "NDS_API_TOKEN",
            "\"$ndsApiToken\"",
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)
    implementation(libs.timber)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    // org.json.JSONObject vem do Android SDK em runtime, mas nao esta disponivel
    // nos unit tests JVM (testDebugUnitTest) sem esta dependencia — mesmo padrao
    // usado em :core:diagnostico e :featureDiagnostico.
    testImplementation("org.json:json:20260719")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
