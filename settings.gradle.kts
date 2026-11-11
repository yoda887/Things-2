pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
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

rootProject.name = "Things"
include(":app")
// Съёмка профиля запуска (Baseline Profile) — нужна только для ./gradlew :app:generateBaselineProfile
include(":baselineprofile")
// Общий модуль правил (domain и data/model) — отдельная сборка, её же подключает десктоп
includeBuild("shared")
