@file:Suppress("PropertyName")

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    // Add the Performance Monitoring Gradle plugin
    id("com.google.firebase.firebase-perf")
}
val VERSION = file(rootDir.resolve("VERSION")).readText().trim()
val VERSION_CODE = VERSION.replace(".", "").toInt()
project.logger.warn("Using versionName: $VERSION, versionCode: $VERSION_CODE")

android {
    signingConfigs {
        create("release") {
            storeFile = file("/Users/nemesisslin/Library/CloudStorage/OneDrive-Personal/SSHKey/Nemesiss.keystore")
            keyAlias = "Nemesiss"
            storePassword = project.properties["KEYSTORE_PASSWORD"].toString()
            keyPassword = project.properties["KEYSTORE_PASSWORD"].toString()
        }

        create("debugSign") {
            storeFile = file("/Users/nemesisslin/Library/CloudStorage/OneDrive-Personal/SSHKey/Nemesiss.keystore")
            keyAlias = "Nemesiss"
            storePassword = project.properties["KEYSTORE_PASSWORD"].toString()
            keyPassword = project.properties["KEYSTORE_PASSWORD"].toString()
        }
    }
    namespace = "moe.nemesiss.hostman"
    compileSdk = 34

    defaultConfig {
        applicationId = "moe.nemesiss.hostman"
        minSdk = 29
        targetSdk = 34
        versionCode = VERSION_CODE
        versionName = VERSION
        multiDexEnabled = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            buildConfigField("String", "APPLICATION_NAME", "\"Hostman\"")
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }

        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = ".debug"
            buildConfigField("String", "APPLICATION_NAME", "\"Hostman\"")
            signingConfig = signingConfigs.getByName("debugSign")
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        aidl = true
        buildConfig = true
        viewBinding = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/io.netty.versions.properties"
        }
    }
}

dependencies {

    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
    implementation(libs.hidden.api.bypass)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.netty.resolver)
    implementation(libs.fastjson)
    implementation(libs.ipaddress)
    implementation(libs.installreferrer)

    implementation(libs.androidx.material3)
    implementation(libs.androidx.material3.window.size.clazz)
    implementation(libs.androidx.material3.adaptive.navigation.suite)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-perf")

    // Barcode Scanner
    // implementation(libs.barcode.scanning)


    // CameraX
//    val cameraxVersion = "1.2.2"
//    implementation("androidx.camera:camera-core:${cameraxVersion}")
//    implementation("androidx.camera:camera-camera2:${cameraxVersion}")
//    implementation("androidx.camera:camera-lifecycle:${cameraxVersion}")
//    implementation("androidx.camera:camera-video:${cameraxVersion}")
//
//    implementation("androidx.camera:camera-view:${cameraxVersion}")
//    implementation("androidx.camera:camera-extensions:${cameraxVersion}")



    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.androidx.runtime.livedata)
    implementation(kotlin("reflect"))
}


tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + "-Xcontext-receivers"
    }
}