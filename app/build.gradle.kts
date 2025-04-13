
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.secrets)
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

        manifestPlaceholders["MAPS_API_KEY"] = project.findProperty("MAPS_API_KEY") ?: ""
    }
    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            buildConfigField("String", "MAPS_API_KEY", "\"${project.findProperty("MAPS_API_KEY")}\"")
        }
        release {
            buildConfigField("String", "MAPS_API_KEY", "\"${project.findProperty("MAPS_API_KEY")}\"")
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

configurations.all {
    resolutionStrategy.dependencySubstitution {
        substitute(module("org.hamcrest:hamcrest-core:1.1")).using(module("junit:junit:4.10"))
    }
}




dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.preference)
    implementation(libs.work.runtime)
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
    implementation(libs.firebase.analytics)
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)
    implementation(libs.json.simple)
    implementation(libs.google.places)
    implementation(libs.volley)

    testImplementation(libs.junit)


}