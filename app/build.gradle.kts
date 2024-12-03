plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.limpialoya"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.limpialoya"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "1.1"

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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    //BIBLIOTECAS PARA GIF
    implementation ("com.github.bumptech.glide:glide:4.15.1");
    annotationProcessor ("com.github.bumptech.glide:compiler:4.15.1");
    //MAPA
    implementation ("com.google.android.gms:play-services-maps:18.1.0");
    //VOLLEY
    implementation ("com.android.volley:volley:1.2.1");
    //
    // Dependencia de OkHttp
    implementation ("com.squareup.okhttp3:okhttp:4.9.3");
    //
    implementation ("com.squareup.picasso:picasso:2.71828");
    //
    implementation ("androidx.core:core:1.6.0");
    //
    implementation ("com.google.android.gms:play-services-location:21.0.1")
    //
    implementation ("androidx.appcompat:appcompat:1.6.1");
    implementation ("com.squareup.picasso:picasso:2.71828");
    //
    implementation ("androidx.camera:camera-core:1.4.0")
    implementation ("androidx.camera:camera-camera2:1.4.0")
    implementation ("androidx.camera:camera-lifecycle:1.4.0")
    implementation ("androidx.camera:camera-view:1.4.0")

}