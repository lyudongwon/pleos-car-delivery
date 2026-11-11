plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "ai.cardeli.starter"
    compileSdk = 36   // Compose 1.9·activity-compose 1.13이 요구. 실행 대상은 targetSdk 34 그대로

    // AAOS 표준 Car API (CarPropertyManager, CarUxRestrictionsManager)
    useLibrary("android.car")

    defaultConfig {
        applicationId = "ai.cardeli.starter"
        minSdk = 28          // Pleos 요구사항: API 28 이상
        targetSdk = 34
        versionCode = 1
        versionName = "0.1"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("ai.pleos.playground:Vehicle:2.3.2")
    implementation("ai.pleos.playground:NaviHelper:3.0.5")
    implementation("ai.pleos.playground:SpeechToText:3.3.1")
    implementation("ai.pleos.playground:TextToSpeech:3.3.1")
    implementation("ai.pleos.playground:FusedLocation:1.1.3")
    // 시험용: 에뮬레이터 v2.1.0 릴리즈 노트상 "LLM SDK 제공 종료"지만 저장소와 서비스가 남아 있어 동작 여부만 확인
    implementation("ai.pleos.playground:LLM:2.1.3.2")

    // UI: Jetpack Compose
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui:1.9.2")
    implementation("androidx.compose.foundation:foundation:1.9.2")
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.9.2")
    debugImplementation("androidx.compose.ui:ui-tooling:1.9.2")

    // ViewModel · StateFlow 수집
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")

    // 단위 테스트 (순수 계산 로직)
    testImplementation("junit:junit:4.13.2")
}
