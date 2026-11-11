// Съёмка профиля запуска (Baseline Profile) для приложения.
//
// Модуль в обычную сборку не входит. Он нужен одной команде — с подключённым телефоном (Android 13+):
//     ./gradlew :app:generateBaselineProfile
// Она ставит на телефон тестовую сборку «Things QA» (свои данные, рабочее приложение не трогается), проходит
// сценарий из BaselineProfileGenerator и записывает, какие классы и методы при этом понадобились, в
// app/src/main/generated/baselineProfiles/baseline-prof.txt. Этот файл хранится в репозитории и попадает
// во все сборки приложения. Переснимать — после заметных изменений экранов или обновления Compose.
//
// На телефоне Xiaomi перед съёмкой включить «Установка через USB» (настройки разработчика): ставятся два новых
// пакета. Если тест прошёл, а сборка упала с «Failed to receive the UTP test results» (так бывает на этом
// компьютере), профиль всё равно записан — его нужно положить руками: файл
// build/outputs/connected_android_test_additional_output/nonMinifiedQa/connected/<телефон>/*-startup-prof.txt
// скопировать в app/src/main/generated/baselineProfiles/ под именами baseline-prof.txt и startup-prof.txt.
plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.bgdlab.things.baselineprofile"
    compileSdk = 35

    defaultConfig {
        // Профиль снимается на Android 9+ (без root — на Android 13+)
        minSdk = 28
        targetSdk = 35
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Те же типы сборки, что у приложения: профиль снимается с «qa»
    buildTypes {
        create("qa") {
            matchingFallbacks += "release"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    targetProjectPath = ":app"
}

baselineProfile {
    // Снимаем на подключённом телефоне, а не на эмуляторе
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.junit)
    implementation(libs.androidx.espresso.core)
    implementation(libs.androidx.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}
