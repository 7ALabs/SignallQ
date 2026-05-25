plugins {
    id("com.android.application") version "8.11.1" apply false
    id("com.android.library") version "8.11.1" apply false
    id("org.jetbrains.kotlin.android") version "2.2.20" apply false
    id("org.jetbrains.kotlin.kapt") version "2.2.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.20" apply false
    id("com.google.gms.google-services") version "4.4.4" apply false
    id("com.google.firebase.appdistribution") version "5.1.1" apply false
    id("com.google.firebase.crashlytics") version "3.0.2" apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ktlint) apply false
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}

val linkaVersionName = libs.versions.versionName.get()
val linkaVersionCode = libs.versions.versionCode.get()

fun registerApkArchiveTask(
    taskName: String,
    assembleTaskPath: String,
    buildType: String,
    rawApkName: String,
) {
    tasks.register<Copy>(taskName) {
        group = "distribution"
        description = "Gera e arquiva o APK $buildType com versao e timestamp em builds/apk/$buildType/$linkaVersionName."

        dependsOn(assembleTaskPath)

        val timestamp = providers.provider {
            java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
        }
        val apkFileName = timestamp.map {
            "linka-android-v$linkaVersionName+$linkaVersionCode-$buildType-$it.apk"
        }

        from(layout.projectDirectory.file("app/build/outputs/apk/$buildType/$rawApkName"))
        into(layout.projectDirectory.dir("builds/apk/$buildType/$linkaVersionName"))
        rename { apkFileName.get() }

        doFirst {
            val rawApk = layout.projectDirectory.file("app/build/outputs/apk/$buildType/$rawApkName").asFile
            if (!rawApk.exists()) {
                throw GradleException("APK bruto nao encontrado: ${rawApk.absolutePath}")
            }
        }
    }
}

registerApkArchiveTask(
    taskName = "archiveDebugApk",
    assembleTaskPath = ":app:assembleDebug",
    buildType = "debug",
    rawApkName = "app-debug.apk",
)

registerApkArchiveTask(
    taskName = "archiveReleaseApk",
    assembleTaskPath = ":app:assembleRelease",
    buildType = "release",
    rawApkName = "app-release.apk",
)
