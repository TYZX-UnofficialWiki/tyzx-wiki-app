plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "org.tyzx.wikiapp"
    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }

    defaultConfig {
        applicationId = "org.tyzx.wikiapp"
        minSdk = 33
        targetSdk = 36
        // CI 用 run number 当补丁号：0.3.<build>，本地构建是 0.3.0-dev
        val build = System.getenv("BUILD_NUMBER")?.toIntOrNull() ?: 0
        versionCode = if (build > 0) 1000 + build else 1
        versionName = if (build > 0) "1.0.$build" else "1.0.0-dev"
    }

    signingConfigs {
        // 仓库里这份是公开的 debug 签名，只为了让每次 CI 产出的 APK 能覆盖安装
        getByName("debug") {
            storeFile = file("debug.p12")
            storeType = "PKCS12"
            storePassword = "tyzxdebug"
            keyAlias = "tyzxdebug"
            keyPassword = "tyzxdebug"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui:1.12.0")
    implementation("androidx.compose.ui:ui-graphics:1.12.0")
    implementation("androidx.compose.foundation:foundation:1.12.0")
    implementation("androidx.compose.animation:animation:1.12.0")
    implementation("androidx.compose.material3:material3:1.5.0-rc01")
    implementation("androidx.compose.material:material-icons-core:1.7.8")
    // 液态玻璃：Kyant0/AndroidLiquidGlass（Apache-2.0），BiliTools 用的就是它
    implementation("io.github.kyant0:backdrop:2.0.1")
    implementation("io.coil-kt.coil3:coil-compose:3.5.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.5.0")
    implementation("org.jsoup:jsoup:1.18.3")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
