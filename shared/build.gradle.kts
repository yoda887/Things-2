// Общий код телефона и десктопа: правила списков, правок и повторов (domain) и модель данных (data/model).
// Чистый Kotlin/JVM без Android и без экранов.
plugins {
    kotlin("jvm") version "2.0.21"
}

group = "com.bgdlab.things"
version = "1.0"

// Байт-код Java 17, как у телефона; собирается тем JDK, которым запущен Gradle (отдельный JDK 17 не нужен)
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // @Inject у use case'ов: на телефоне их собирает Hilt, на десктопе — вручную
    api("javax.inject:javax.inject:1")
    // Аннотации Room на сущностях. Только для компиляции: на телефоне их приносит Room,
    // на десктопе (JDBC) их заменяют пустые аннотации-заглушки
    compileOnly("androidx.room:room-common:2.6.1")
    // Flow в интерфейсах репозиториев: версию задаёт приложение
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.room:room-common:2.6.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}
