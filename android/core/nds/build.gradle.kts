import java.util.Properties

plugins {
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

// NDS-01 (#1744, ADR-017) — Bearer token estatico do Network Diagnostics Service.
// Lido de local.properties em dev (NUNCA commitado — arquivo esta no .gitignore),
// ou da variavel de ambiente NDS_API_TOKEN em CI/release. O valor real deve ser
// preenchido manualmente por quem tiver o token (Luiz ou delegado explicito) —
// nunca escrito em nenhum arquivo versionado deste repositorio. Em debug/testes, o placeholder
// vazio preserva builds locais sem acesso ao NDS. Em qualquer tarefa de release, a ausência do
// token interrompe o build para não distribuir um APK/AAB que sempre falhará ao abrir o Assist.
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

private val releaseTaskRequested =
    gradle.startParameter.taskNames.any { it.contains("Release", ignoreCase = true) }

if (releaseTaskRequested && ndsApiToken.isBlank()) {
    throw GradleException(
        "NDS_API_TOKEN ausente: configure android/local.properties ou a secret " +
            "NDS_API_TOKEN antes de gerar um build de release.",
    )
}

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
    // NDS-02a (#1747): ChannelScore (mapper de wifiScan) vem de :coreNetwork -- infra
    // de conectividade on-device, dependencia estavel, sem plano de remocao.
    implementation(project(":coreNetwork"))
    // NDS-02a (#1747): MetricStatus (vocabulario de severidade, MetricClassifier.kt)
    // vem de :core:diagnostico -- dependencia INTENCIONALMENTE TEMPORARIA. O ADR-017
    // marca core/diagnostico para remocao apos todos os consumidores migrarem (NDS-03),
    // mas MetricStatus e explicitamente promovido a vocabulario canonico de UI (decisao
    // registrada em #1746 secao 5) e deve sobreviver a remocao do motor -- so nao tem
    // ainda um lar definitivo fora de MetricClassifier.kt. Revisitar esta dependencia
    // quando NDS-03 decidir onde o enum fica (provavel candidato: este proprio modulo).
    implementation(project(":core:diagnostico"))
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
