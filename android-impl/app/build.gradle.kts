import java.util.Properties

plugins {
    id("com.android.application") version "8.2.2"
    id("org.jetbrains.kotlin.android") version "1.9.22"
    id("com.google.devtools.ksp") version "1.9.22-1.0.17"
    id("com.google.dagger.hilt.android") version "2.51"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22"
}

android {
    namespace = "com.btsheng.erp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.btsheng.erp"
        minSdk = 26
        targetSdk = 34
        versionCode = 4   // 1=V1.3.7 · 2=V1.3.8 · 3=V1.3.8 Sprint 8-9 · 4=V1.3.9
        versionName = "1.3.9"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // V1.3.9 业务定制（V1.3.9 Sprint 12-14 整合）
        buildConfigField("String", "PRD_VERSION", "\"1.3.9\"")
        buildConfigField("String", "CONTRACT_ID", "\"XP-ZPF202606082405\"")
        buildConfigField("String", "CLIENT", "\"昆山佰泰胜精密机械有限公司\"")
        buildConfigField("String", "VENDOR", "\"河南晓评信息科技有限公司\"")
    }

    // V1.3.9 标准 Android Studio 项目结构 · keystore.properties 在 app/ 同级
    // 必须在 buildTypes 之前定义（否则 signingConfigs.getByName("release") 报错）
    signingConfigs {
        create("release") {
            val keystorePropertiesFile = file("keystore.properties")
            val keystoreProperties = Properties()
            if (keystorePropertiesFile.exists()) {
                keystorePropertiesFile.inputStream().use { input ->
                    keystoreProperties.load(input)
                }
            }
            storeFile = file(keystoreProperties.getProperty("RELEASE_STORE_FILE") ?: "release.keystore")
            storePassword = keystoreProperties.getProperty("RELEASE_STORE_PASSWORD")
            keyAlias = keystoreProperties.getProperty("RELEASE_KEY_ALIAS")
            keyPassword = keystoreProperties.getProperty("RELEASE_KEY_PASSWORD")
            enableV1Signing = true   // JAR 签名（Android < 7 兼容）
            enableV2Signing = true   // APK 签名 v2（Android 7+ 强制）
        }
    }

    buildTypes {
        val localProperties = Properties()
        rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use {
            localProperties.load(it)
        }
        val debugApiBaseUrl = localProperties.getProperty("debug.api.base.url", "https://bts.51xiaoping.com/")
            .let { url -> if (url.endsWith("/")) url else "$url/" }

        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            // 真机联调：local.properties 配置 debug.api.base.url（默认云端）
            buildConfigField("String", "API_BASE_URL", "\"$debugApiBaseUrl\"")
            buildConfigField("String", "NACOS_ENV", "\"dev\"")
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // V1.3.9 内网/云部署
            buildConfigField("String", "API_BASE_URL", "\"https://bts.51xiaoping.com/\"")
            buildConfigField("String", "NACOS_ENV", "\"prod\"")
            // V1.3.8 Sprint 11 Story 11.4：release 签名（V1/V2）
            signingConfig = signingConfigs.getByName("release")
        }
    }

    // V1.3.9 标准结构注释位置保留：signingConfigs 已在 L36-52 定义


    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.serialization.ExperimentalSerializationApi"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
        // V1.3.8 Sprint 8 Story 8.5：启用 ViewBinding（3 个 Fragment layout XML）
        viewBinding = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
            all { test ->
                test.useJUnitPlatform()
            }
        }
    }
}

dependencies {
    // Kotlin & Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    // AndroidX Core
    implementation("androidx.core:core-ktx:1.13.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.02.02"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Hilt DI
    implementation("com.google.dagger:hilt-android:2.51")
    ksp("com.google.dagger:hilt-android-compiler:2.51")

    // Network
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.moshi:moshi:1.15.1")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")
    ksp("com.squareup.moshi:moshi-kotlin-codegen:1.15.1")

    // Local DB (V1.3.7 离线扫码缓存 500 条)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // V1.3.7 Secure Storage (JWT 存 EncryptedSharedPreferences)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // V1.3.7 Scan: 工单码 GD- / 流转码 LZ- / 设备码 SB- / 物料码 WL- / 委外单码 WW-
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    // WorkManager (离线同步)
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // Chaquopy (可选 · 跑 PRD 内 Python 算法)
    // implementation("com.chaquo.python:gradle:14.0.0")

    // Tests
    testImplementation("junit:junit:4.13.2")
    // V1.3.7: JUnit 5 (Jupiter) for Kotlin tests
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
    testImplementation("io.mockk:mockk:1.13.10")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("app.cash.turbine:turbine:1.1.0")
    testImplementation("org.robolectric:robolectric:4.12.2")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.51")
    kspAndroidTest("com.google.dagger:hilt-android-compiler:2.51")
    androidTestImplementation("androidx.room:room-testing:2.6.1")
    androidTestImplementation("androidx.work:work-testing:2.9.0")
    // V1.3.9 Sprint 14 Story 13.6 · UiAutomator for ADB 扫码模拟 + Toast 检测
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.2.0")
    // V1.3.9 Sprint 14 Story 13.6 · Test orchestrator for connectedAndroidTest 状态隔离
    androidTestImplementation("androidx.test:orchestrator:1.4.2")
    // V1.3.9 Sprint 14 Story 13.6 · @FlakyTest 注解（重试机制）
    androidTestImplementation("androidx.test:flaky:1.0.0-alpha02")
}
