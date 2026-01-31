plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.kooo.evcam"
    compileSdk = 36

    // 缁涙儳鎮曢柊宥囩枂 (娴ｈ法鏁?AOSP 閸忣剙鍙″ù瀣槸缁涙儳鎮?
    signingConfigs {
        create("release") {
            storeFile = file("../keystore/release.jks")
            storePassword = "android"
            keyAlias = "apkeasytool"
            keyPassword = "android"
        }
    }

    defaultConfig {
        applicationId = "com.kooo.evcam"
        minSdk = 28
        targetSdk = 36
        versionCode = 14
        versionName = "0.9.9.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // 娴ｈ法鏁ょ粵鎯ф倳闁板秶鐤?            signingConfig = signingConfigs.getByName("release")
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
    implementation(libs.recyclerview)
    implementation(libs.cardview)

    // 闁藉鎷ょ€规ɑ鏌?Stream SDK
    implementation("com.dingtalk.open:app-stream-client:1.3.12")

    // 椋炰功锛氫娇鐢ㄨ交閲忕骇 OkHttp WebSocket 瀹炵幇锛屼笉鍐嶄緷璧栧畼鏂?SDK

    // 缃戠粶璇锋眰鍜?WebSocket
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // JSON 鐟欙絾鐎?    implementation("com.google.code.gson:gson:2.10.1")

    // Glide 閸ュ墽澧栭崝鐘烘祰鎼存搫绱欓悽銊ょ艾缂傛挸鐡ㄩ崪灞肩喘閸栨牜缂夐悾銉ユ禈閸旂姾娴囬敍?
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // WorkManager 鐎规碍妞傛禒璇插閿涘牏鏁ゆ禍搴濈箽濞蹭紮绱?    implementation("androidx.work:work-runtime:2.9.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
