// Общий модуль правил Things: его подключают и телефон (Things 2), и десктоп (Things Desktop) как вложенную
// сборку (includeBuild) — без копирования файлов между проектами.
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "shared"
