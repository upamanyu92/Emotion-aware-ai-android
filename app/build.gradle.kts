plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.emotionawareai"
    compileSdk = 35

    val keystoreFile = System.getenv("KEYSTORE_FILE")
    val keystorePassword = System.getenv("KEYSTORE_PASSWORD")
    val keystoreKeyAlias = System.getenv("KEY_ALIAS")
    val keystoreKeyPassword = System.getenv("KEY_PASSWORD")
    val keystoreType = System.getenv("KEYSTORE_TYPE")
    val hasReleaseSigning = keystoreFile != null && keystorePassword != null &&
        keystoreKeyAlias != null && keystoreKeyPassword != null

    defaultConfig {
        applicationId = "com.example.emotionawareai"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val inAppPremiumSku = System.getenv("BILLING_INAPP_PREMIUM_SKU") ?: "premium_unlock"
        val monthlyPremiumSku = System.getenv("BILLING_SUBS_MONTHLY_SKU") ?: "premium_monthly"
        buildConfigField("String", "BILLING_INAPP_PREMIUM_SKU", "\"$inAppPremiumSku\"")
        buildConfigField("String", "BILLING_SUBS_MONTHLY_SKU", "\"$monthlyPremiumSku\"")

        externalNativeBuild {
            cmake {
                cppFlags += listOf("-std=c++17", "-O3", "-DANDROID")
                abiFilters += listOf("arm64-v8a", "x86_64")
            }
        }

        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    signingConfigs {
        create("release") {
            if (hasReleaseSigning) {
                storeFile = file(keystoreFile)
                storePassword = keystorePassword
                keyAlias = keystoreKeyAlias
                keyPassword = keystoreKeyPassword
                // Allow CI to specify the keystore format explicitly (e.g. "JKS" after
                // converting a modern PKCS12 keystore to a BouncyCastle-compatible format).
                if (keystoreType != null) storeType = keystoreType
            }
        }
    }

    buildTypes {
        release {
            // Keep release stable while we triage startup crash on installed APK.
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            // Remove applicationIdSuffix for Play Store compatibility
            // Play Store packageName must match the base applicationId
            versionNameSuffix = "-alpha"
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/LICENSE*"
            excludes += "/META-INF/NOTICE*"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    // Generate a separate APK per ABI so each architecture download is smaller.
    // A universal APK (all ABIs combined) is also produced as a fallback.
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "x86_64")
            isUniversalApk = true
        }
    }
}

// Assign a unique versionCode per ABI split so the Play Store can serve the
// right APK to each device.  Multiplier keeps plenty of room for future
// versionCode increments within each ABI bucket.
// Also renames every APK output to: moodmitraAI-{buildType}-{abi-}{versionName}.apk
val abiVersionCode = mapOf("arm64-v8a" to 2, "x86_64" to 1)
android.applicationVariants.configureEach {
    val buildTypeName = buildType.name          // "debug" or "release"
    val fullVersion   = versionName             // includes versionNameSuffix, e.g. "1.0.0-alpha"
    outputs.configureEach {
        val output = this as? com.android.build.gradle.internal.api.ApkVariantOutputImpl
            ?: return@configureEach
        val abi = output.getFilter(com.android.build.OutputFile.ABI)
        if (abi != null) {
            output.versionCodeOverride =
                (abiVersionCode[abi] ?: 0) * 1000 + android.defaultConfig.versionCode!!
        }
        // Rename: moodmitraAI-{buildType}-{abi-}{versionName}.apk
        val abiSuffix = if (abi != null) "-$abi" else ""
        output.outputFileName = "moodmitraAI-$buildTypeName$abiSuffix-$fullVersion.apk"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // MediaPipe
    implementation(libs.mediapipe.tasks.vision)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    // Billing
    implementation(libs.play.billing)
    implementation(libs.play.services.auth)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.arch.core.testing)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.arch.core.testing)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.mockk.android)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
