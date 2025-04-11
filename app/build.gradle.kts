plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.appseguimiento"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.appseguimiento"
        minSdk = 29
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
    implementation(libs.preference)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // -------------------------------
    // Dependencias añadidas para Room, RecyclerView y CardView:
    // Para Room (en Java usamos annotationProcessor en lugar de kapt)
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

    // Para RecyclerView
    implementation (libs.recyclerview)

    // Para CardView
    implementation(libs.cardview)

    // Dependencias añadidas para la segunda entrega
    implementation(libs.gms.auth)
    implementation(libs.gms.maps)
    implementation(libs.gms.location)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)
    implementation(libs.json.simple)

}