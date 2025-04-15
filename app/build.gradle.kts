plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.eee3457.petcare"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.eee3457.petcare"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.dotsindicator)
    implementation(libs.play.services.maps)
    implementation(libs.gms.play.services.location)
    implementation(libs.places)
    implementation(libs.okhttp)
    implementation (libs.json)
    implementation (libs.cardview)

}