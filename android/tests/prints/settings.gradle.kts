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
