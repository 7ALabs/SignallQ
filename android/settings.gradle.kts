pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "linkaAndroidKotlin"

include(
    ":app",
    ":coreNetwork",
    ":corePermissions",
    ":coreDatabase",
    ":coreDatastore",
    ":coreTelephony",
    ":coreRecommendation",
    ":featureHome",
    ":featureWifi",
    ":featureDevices",
    ":featureDns",
    ":featureSpeedtest",
    ":featureDiagnostico",
    ":featureFibra",
    ":featureHistory",
    ":featureSettings",
)

project(":coreNetwork").projectDir    = File("core/network")
project(":coreDatabase").projectDir   = File("core/database")
project(":coreDatastore").projectDir  = File("core/datastore")
project(":corePermissions").projectDir = File("core/permissions")
project(":coreTelephony").projectDir  = File("core/telephony")
project(":coreRecommendation").projectDir = File("core/recommendation")
project(":featureHome").projectDir        = File("feature/home")
project(":featureWifi").projectDir        = File("feature/wifi")
project(":featureDevices").projectDir     = File("feature/devices")
project(":featureDns").projectDir         = File("feature/dns")
project(":featureSpeedtest").projectDir   = File("feature/speedtest")
project(":featureDiagnostico").projectDir = File("feature/diagnostico")
project(":featureFibra").projectDir       = File("feature/fibra")
project(":featureHistory").projectDir     = File("feature/history")
project(":featureSettings").projectDir    = File("feature/settings")
