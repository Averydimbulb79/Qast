plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
// A stable signing identity is essential for Android in-place updates.
// CI injects these from GitHub Actions secrets; never commit a keystore.
val qastKeystorePath = providers.environmentVariable("QAST_KEYSTORE_FILE").orNull
val qastKeystorePassword = providers.environmentVariable("QAST_KEYSTORE_PASSWORD").orNull
val hasStableSigning = !qastKeystorePath.isNullOrBlank() && !qastKeystorePassword.isNullOrBlank()

android {
    signingConfigs {
        if (hasStableSigning) {
            create("qastStable") {
                storeFile = file(qastKeystorePath!!)
                storePassword = qastKeystorePassword
                keyAlias = "qast"
                keyPassword = qastKeystorePassword
            }
        }
    }
    namespace = "sg.qast.player"
    compileSdk = 35
    defaultConfig {
        applicationId = "sg.qast.player"
        minSdk = 28
        targetSdk = 35
        versionCode = 6
        versionName = "0.1.5-alpha"
    }
    buildTypes {
        getByName("debug") {
            if (hasStableSigning) {
                signingConfig = signingConfigs.getByName("qastStable")
            }
        }
        getByName("release") { isMinifyEnabled = false }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-ui:1.5.1")
    implementation("com.google.android.gms:play-services-cast-framework:22.2.0")
    implementation("androidx.mediarouter:mediarouter:1.7.0")
}
