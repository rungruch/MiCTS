plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.lsplugin.jgit)
    alias(libs.plugins.kotlin.compose)
}

val repo = jgit.repo()
val commitCount = (repo?.commitCount("refs/remotes/origin/main") ?: 1)
val latestTag = repo?.latestTag?.removePrefix("v") ?: "1.0"
val releaseSigningProperties = listOf(
    "androidStoreFile",
    "androidStorePassword",
    "androidKeyAlias",
    "androidKeyPassword",
).associateWith(providers::gradleProperty)
val releaseSigningConfigured = releaseSigningProperties.values.all { it.isPresent }

android {
    namespace = "com.parallelc.micts"
    compileSdk = 37

    defaultConfig {
        minSdk = 28
        targetSdk = 37
        versionCode = commitCount
        versionName = latestTag

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    if (releaseSigningConfigured) {
        signingConfigs {
            create("release") {
                storeFile = file(releaseSigningProperties.getValue("androidStoreFile").get())
                storePassword = releaseSigningProperties.getValue("androidStorePassword").get()
                keyAlias = releaseSigningProperties.getValue("androidKeyAlias").get()
                keyPassword = releaseSigningProperties.getValue("androidKeyPassword").get()
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("sideload") {
            initWith(getByName("release"))
            isDebuggable = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    bundle {
        language {
            enableSplit = false
        }
    }

    flavorDimensions += "app"

    productFlavors {
        create("MiCTS") {
            dimension = "app"
            applicationId = "com.parallelc.micts"
            buildConfigField("String", "APP_NAME", "\"MiCTS\"")
            buildConfigField("boolean", "CAPTURE_AS_JPEG", "true")
        }

        create("VISTrigger") {
            dimension = "app"
            applicationId = "com.parallelc.vistrigger"
            resValue("string", "app_name", "VISTrigger")
            resValue("string", "tile_label", "VIS")
            resValue("string", "xposed_description", "Trigger Voice Interaction Service on any Android 9–16 device")
            buildConfigField("String", "APP_NAME", "\"VISTrigger\"")
            buildConfigField("boolean", "CAPTURE_AS_JPEG", "false")
            proguardFiles("src/VISTrigger/proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }

    lint {
        baseline = file("lint-baseline.xml")
        warningsAsErrors = true
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.material)
    implementation(libs.material3)
    implementation(libs.animation.core.android)
    implementation(libs.animation.android)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.accompanist.drawablepainter)
    implementation(libs.hiddenapibypass)
    add("VISTriggerCompileOnly", libs.libxposed.api)
    add("VISTriggerImplementation", libs.libxposed.service)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.core.ktx)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
