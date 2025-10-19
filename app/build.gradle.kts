import java.util.Properties
import java.io.FileInputStream
import java.io.File

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.ridesharingapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.ridesharingapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Read token from local.properties
        val localProperties = File(rootProject.projectDir, "local.properties")
        val properties = Properties()  // Remove java.util prefix
        if (localProperties.exists()) {
            properties.load(FileInputStream(localProperties))  // Remove java.io prefix
        }

        val mapboxToken = properties.getProperty("MAPBOX_ACCESS_TOKEN") ?: ""

        // Added Mapbox token from local.properties
        buildConfigField(
            "String",
            "MAPBOX_ACCESS_TOKEN",
            "\"$mapboxToken\""
        )

        // Inject token into string resources
        resValue(
            "string",
            "mapbox_access_token",
            mapboxToken
        )
    }

    buildFeatures {
        buildConfig = true
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
    // AndroidX + Material (Defualt)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
    implementation("com.google.firebase:firebase-database:20.3.0")
    implementation("com.google.firebase:firebase-auth:22.3.0")
    implementation("com.google.firebase:firebase-storage:20.3.0")
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
    implementation("com.google.firebase:firebase-appcheck-debug")

    // Mapbox
    implementation("com.mapbox.maps:android:10.17.0")
    implementation("com.mapbox.navigation:android:2.17.1")
    implementation("com.mapbox.navigation:ui-dropin:2.17.1")

    // Google Location Services
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // RecyclerView + CardView
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")

    // Image loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // Circle ImageView
    implementation("de.hdodenhof:circleimageview:3.1.0")
//    implementation("de.hdodenhof.circleimageview:circleimageview:3.1.0")

    // library to handle HTTP requests (e.g., Volley or OkHttp). this lets Android app communicate with the server
    implementation("com.android.volley:volley:1.2.1")
//    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}