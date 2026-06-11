plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

fun buildValue(name: String, defaultValue: String? = null): String? {
    val value = providers.gradleProperty(name)
        .orElse(providers.environmentVariable(name))
        .orNull
        ?.trim()
        ?.takeIf(String::isNotEmpty)
    return value ?: defaultValue
}

val appUpdateUrl = buildValue(
    "APP_UPDATE_URL",
    "https://api.github.com/repos/KrelinnBios/YamiboReaderLite/releases/latest"
)!!
val appVersionName = buildValue("APP_VERSION_NAME", "1.0.0")!!
val appVersionCode = buildValue("APP_VERSION_CODE", "3")!!.toInt()
val releaseKeystorePath = buildValue("ANDROID_KEYSTORE_PATH")
val releaseKeyAlias = buildValue("ANDROID_KEY_ALIAS")
val releaseKeyPassword = buildValue("ANDROID_KEY_PASSWORD")
val releaseStorePassword = buildValue("ANDROID_STORE_PASSWORD")
val hasReleaseSigning = listOf(
    releaseKeystorePath,
    releaseKeyAlias,
    releaseKeyPassword,
    releaseStorePassword
).all { !it.isNullOrBlank() }

android {
    namespace = "org.shirakawatyu.yamibo.novel"
    compileSdk = 34

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseKeystorePath!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    defaultConfig {
        applicationId = "com.krelinnbios.yamiboreaderlite"
        minSdk = 24
        targetSdk = 34
        versionCode = appVersionCode
        versionName = appVersionName
        buildConfigField(
            "String",
            "APP_UPDATE_URL",
            "\"${appUpdateUrl.replace("\\", "\\\\").replace("\"", "\\\"")}\""
        )

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        ndk {
            abiFilters.add("arm64-v8a")
            abiFilters.add("armeabi-v7a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    applicationVariants.all {
        outputs.all {
            (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName =
                "300 Lite.apk"
        }
    }
}

dependencies {
    implementation("androidx.activity:activity-compose:1.9.3")

    implementation(platform("androidx.compose:compose-bom:2024.10.00"))

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation("androidx.compose.runtime:runtime-rxjava2")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.animation:animation-core")

    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.3")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("org.jsoup:jsoup:1.17.2")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:okhttp-brotli:4.12.0")
    implementation("com.alibaba.fastjson2:fastjson2:2.0.51.android5")
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("io.coil-kt:coil:2.6.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("sh.calvin.reorderable:reorderable:3.0.0")
    implementation("me.saket.telephoto:zoomable-image-coil:0.6.2")
    implementation("com.github.qichuan:android-opencc:1.2.0")
    implementation("com.airbnb.android:lottie-compose:6.7.1")
    implementation("com.squareup.okhttp3:okhttp-dnsoverhttps:4.12.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.10.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
