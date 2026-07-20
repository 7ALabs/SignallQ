import com.google.firebase.appdistribution.gradle.firebaseAppDistribution
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.kapt")
    id("com.google.devtools.ksp")
    alias(libs.plugins.hilt)
    id("com.google.gms.google-services")
    id("com.google.firebase.appdistribution")
    id("com.google.firebase.crashlytics")
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.play.publisher)
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    filter {
        exclude("**/*.kts")
    }
}

private val keyPropertiesFile = rootProject.file("key.properties")
private val keyProperties =
    Properties().apply {
        if (keyPropertiesFile.exists()) load(keyPropertiesFile.inputStream())
    }

// Secrets de telemetria — lidos de local.properties em dev (nunca commitados).
// Em CI/release, injetar via variavel de ambiente: ADMIN_INGEST_KEY=xxx
private val localPropertiesFile = rootProject.file("local.properties")
private val localProperties =
    Properties().apply {
        if (localPropertiesFile.exists()) load(localPropertiesFile.inputStream())
    }
private val adminIngestKey: String =
    localProperties.getProperty("ADMIN_INGEST_KEY")
        ?: System.getenv("ADMIN_INGEST_KEY")
        ?: ""

// Publicacao na Play Console (gradle-play-publisher).
// Service account JSON lida de key.properties (playServiceAccountFile) ou env
// PLAY_SERVICE_ACCOUNT_JSON_FILE. NUNCA commitar o arquivo de credencial.
// Trilha configuravel via -PplayTrack=... (default: alpha = teste fechado).
play {
    val serviceAccountPath =
        (keyProperties["playServiceAccountFile"] as String?)
            ?: System.getenv("PLAY_SERVICE_ACCOUNT_JSON_FILE")
    if (serviceAccountPath != null) {
        serviceAccountCredentials.set(rootProject.file(serviceAccountPath))
    }
    track.set(providers.gradleProperty("playTrack").orElse("alpha").get())
    defaultToAppBundles.set(true)
}

android {
    namespace = "io.signallq.app"
    compileSdk = libs.versions.compileSdk
        .get()
        .toInt()

    defaultConfig {
        applicationId = "io.signallq.app"
        minSdk = libs.versions.minSdk
            .get()
            .toInt()
        targetSdk = libs.versions.targetSdk
            .get()
            .toInt()
        versionCode = libs.versions.versionCode
            .get()
            .toInt()
        versionName = libs.versions.versionName.get()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // URL base do signallq-admin-worker. Nao e segredo — apenas infraestrutura.
        buildConfigField(
            "String",
            "ADMIN_INGEST_URL",
            "\"https://signallq-admin.giammattey-luiz.workers.dev\"",
        )
        // Chave de ingest (scope limitado: POST /ingest/* apenas).
        // Lida de local.properties em dev, variavel de ambiente em CI.
        // NUNCA commitar o valor real aqui.
        buildConfigField(
            "String",
            "ADMIN_INGEST_KEY",
            "\"$adminIngestKey\"",
        )

        // GH#935 — sonda regional (TCP/HTTPS) usada pela tela Jogos (REGIONAL_ESTIMATE).
        // Worker leve, sem logica de jogo, so eco/latencia (integrations/cloudflare/game-latency-probe-worker).
        // Nao e segredo — apenas infraestrutura.
        buildConfigField(
            "String",
            "GAME_LATENCY_PROBE_URL",
            "\"https://signallq-game-latency-probe.giammattey-luiz.workers.dev/probe\"",
        )
    }

    signingConfigs {
        create("release") {
            if (keyPropertiesFile.exists()) {
                keyAlias = keyProperties["keyAlias"] as String
                keyPassword = keyProperties["keyPassword"] as String
                storeFile = keyProperties["storeFile"]?.let { rootProject.file(it as String) }
                storePassword = keyProperties["storePassword"] as String
            }
        }
    }

    buildTypes {
        debug {
            firebaseAppDistribution {
                appId = "1:741421457740:android:a8658a91308fba058fefe9"
                artifactType = "APK"
                testers = "giammattey.luiz@gmail.com"
                releaseNotes = "SignallQ ${libs.versions.versionName.get()} (build ${libs.versions.versionCode.get()}) — DEBUG"
            }
            // ─── MVP — ativos em debug E release ──────────────────────
            buildConfigField("Boolean", "FEATURE_SPEEDTEST", "true")
            buildConfigField("Boolean", "FEATURE_DIAGNOSTICO_LOCAL", "true")
            buildConfigField("Boolean", "FEATURE_DIAGNOSTICO_IA", "true")
            buildConfigField("Boolean", "FEATURE_WIFI_ANALISE", "true")
            buildConfigField("Boolean", "FEATURE_REDE_MOVEL_ANALISE", "true")
            buildConfigField("Boolean", "FEATURE_HISTORICO", "true")
            buildConfigField("Boolean", "FEATURE_LAUDO_PDF", "true")
            buildConfigField("Boolean", "FEATURE_ONBOARDING", "true")
            buildConfigField("Boolean", "FEATURE_PERMISSOES_CONTEXTO", "true")
            buildConfigField("Boolean", "FEATURE_ESTADO_OFFLINE", "true")
            buildConfigField("Boolean", "FEATURE_SETTINGS_MVP", "true")
            buildConfigField("Boolean", "FEATURE_PRIVACIDADE_TELA", "true")
            buildConfigField("Boolean", "FEATURE_NOVIDADES_TELA", "true")
            // ─── Pós-MVP — ativos APENAS em debug (para testar) ───────
            buildConfigField("Boolean", "FEATURE_LINKPULSE_ATIVO", "true")
            buildConfigField("Boolean", "FEATURE_NOTIFICACAO_INLINE", "true")
            buildConfigField("Boolean", "FEATURE_WIDGET", "true")
            buildConfigField("Boolean", "FEATURE_QUICK_SETTINGS_TILE", "true")
            buildConfigField("Boolean", "FEATURE_PROVA_REAL_COMPLETO", "true")
            buildConfigField("Boolean", "FEATURE_DIAGNOSTICO_ITERATIVO", "true")
            buildConfigField("Boolean", "FEATURE_TRACEROUTE", "true")
            buildConfigField("Boolean", "FEATURE_FIBRA_SCREEN", "true")
            buildConfigField("Boolean", "FEATURE_DEVICES_SCREEN_V2", "true")
            buildConfigField("Boolean", "FEATURE_TELEPHONY_AVANCADO", "true")
            buildConfigField("Boolean", "FEATURE_MAPA_CALOR_WIFI", "true")
            buildConfigField("Boolean", "FEATURE_AGENDAMENTO_TESTES", "true")
            buildConfigField("Boolean", "FEATURE_LINKPULSE_CHAT", "true")
            buildConfigField("Boolean", "FEATURE_LINKASYNC", "true")
            buildConfigField("Boolean", "FEATURE_BACKUP_LOCAL", "true")
            buildConfigField("Boolean", "FEATURE_CONTRIBUICAO_ANONIMA", "true")
            buildConfigField("Boolean", "FEATURE_RATE_US", "true")
            buildConfigField("Boolean", "FEATURE_ACESSIBILIDADE", "true")
        }
        release {
            firebaseAppDistribution {
                appId = "1:741421457740:android:a8658a91308fba058fefe9"
                artifactType = "APK"
                testers = "giammattey.luiz@gmail.com"
                releaseNotes = "SignallQ ${libs.versions.versionName.get()} (build ${libs.versions.versionCode.get()})"
            }
            // Upload automático do mapping.txt para Crashlytics acontece como dependência
            // do bundleRelease/assembleRelease quando mappingFileUploadEnabled = true.
            firebaseCrashlytics {
                mappingFileUploadEnabled = true
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (keyPropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            // ─── ATIVO NO RELEASE ─────────────────────────────────────────
            // MVP core
            buildConfigField("Boolean", "FEATURE_SPEEDTEST", "true")
            buildConfigField("Boolean", "FEATURE_DIAGNOSTICO_LOCAL", "true")
            buildConfigField("Boolean", "FEATURE_DIAGNOSTICO_IA", "true") // card + laudo
            buildConfigField("Boolean", "FEATURE_WIFI_ANALISE", "true")
            buildConfigField("Boolean", "FEATURE_REDE_MOVEL_ANALISE", "true")
            buildConfigField("Boolean", "FEATURE_HISTORICO", "true")
            buildConfigField("Boolean", "FEATURE_LAUDO_PDF", "true")
            buildConfigField("Boolean", "FEATURE_ONBOARDING", "true")
            buildConfigField("Boolean", "FEATURE_PERMISSOES_CONTEXTO", "true")
            buildConfigField("Boolean", "FEATURE_ESTADO_OFFLINE", "true")
            buildConfigField("Boolean", "FEATURE_SETTINGS_MVP", "true")
            buildConfigField("Boolean", "FEATURE_PRIVACIDADE_TELA", "true")
            buildConfigField("Boolean", "FEATURE_NOVIDADES_TELA", "true")
            // Features adicionais aprovadas para release
            buildConfigField("Boolean", "FEATURE_FIBRA_SCREEN", "true")
            // ─── FORA DO RELEASE ──────────────────────────────────────────
            // Dispositivos (limitação de hostname conhecida)
            buildConfigField("Boolean", "FEATURE_DEVICES_SCREEN_V2", "false")
            // Monitoramento passivo e dependentes
            buildConfigField("Boolean", "FEATURE_LINKPULSE_ATIVO", "false")
            buildConfigField("Boolean", "FEATURE_NOTIFICACAO_INLINE", "false")
            buildConfigField("Boolean", "FEATURE_LINKPULSE_CHAT", "false")
            // Pós-MVP Sprint 1
            buildConfigField("Boolean", "FEATURE_WIDGET", "false")
            buildConfigField("Boolean", "FEATURE_QUICK_SETTINGS_TILE", "false")
            // Pós-MVP Sprint 2
            buildConfigField("Boolean", "FEATURE_PROVA_REAL_COMPLETO", "false")
            buildConfigField("Boolean", "FEATURE_DIAGNOSTICO_ITERATIVO", "false")
            buildConfigField("Boolean", "FEATURE_TRACEROUTE", "false")
            // Pós-MVP Sprint 3+
            buildConfigField("Boolean", "FEATURE_TELEPHONY_AVANCADO", "false")
            buildConfigField("Boolean", "FEATURE_MAPA_CALOR_WIFI", "false")
            buildConfigField("Boolean", "FEATURE_AGENDAMENTO_TESTES", "false")
            buildConfigField("Boolean", "FEATURE_LINKASYNC", "false")
            buildConfigField("Boolean", "FEATURE_BACKUP_LOCAL", "false")
            buildConfigField("Boolean", "FEATURE_CONTRIBUICAO_ANONIMA", "false")
            buildConfigField("Boolean", "FEATURE_RATE_US", "false")
            buildConfigField("Boolean", "FEATURE_ACESSIBILIDADE", "false")
        }
    }

    bundle {
        language { enableSplit = true }
        density { enableSplit = true }
        abi { enableSplit = true }
    }

    // Limita recursos a pt e pt-BR: elimina strings de todas as outras linguas
    // que vem de dependencias (appcompat, material, etc.). Reducao estimada: 0.5-2 MB.
    // Substitui defaultConfig.resourceConfigurations (deprecated no AGP 9).
    androidResources {
        localeFilters += listOf("pt", "pt-rBR")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        encoding = "UTF-8"
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

kapt {
    correctErrorTypes = true
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
    implementation(project(":coreNetwork"))
    implementation(project(":corePermissions"))
    implementation(project(":coreDatabase"))
    implementation(project(":coreDatastore"))
    implementation(project(":coreTelephony"))
    implementation(project(":coreRecommendation"))
    implementation(project(":featureHome"))
    implementation(project(":featureWifi"))
    implementation(project(":featureDevices"))
    implementation(project(":featureDns"))
    implementation(project(":featureSpeedtest"))
    implementation(project(":featureDiagnostico"))
    implementation(project(":featureFibra"))
    implementation(project(":featureHistory"))
    implementation(project(":featureSettings"))
    // Dominio de causa-raiz extraido de :featureDiagnostico (issue #1157 Fase 1a) — DiagnosticReport/
    // DiagnosticInput/DiagnosticStatus etc sao consumidos direto por telas e ViewModels do :app.
    implementation(project(":core:diagnostico"))
    // GH#1219 — motor generico HTML->PDF (WebView.createPrintDocumentAdapter), ja usado pelo
    // Pro (:pro:feature:laudo) e por :featureHistory. Unifica ResultadoPdfGenerator/LaudoScreen
    // no mesmo renderer, com paginacao real em vez de Canvas manual.
    implementation(project(":core:relatorio"))

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.google.material)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.work.runtime.ktx)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)
    implementation(libs.timber)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.okhttp)
    // GH#970 — carrega logo remota de operadora de cauda longa (ProviderLogo.url,
    // diretorio do worker signallq-diagnostic). So usado quando o catalogo local
    // (OperadoraLogoCatalog) nao tem a operadora.
    implementation(libs.coil.compose)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.config)

    // Avaliacao nativa Google Play sem atrito (SIG-173/#664)
    implementation(libs.play.review)

    // Monetizacao nativa AdMob (issue #555) -- Google Mobile Ads SDK + UMP (gate de
    // consentimento obrigatorio antes de qualquer AdRequest, mesmo so contextual).
    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.mockk)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
    releaseImplementation(libs.compose.ui.test.manifest)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
