plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.kooo.evcam"
    compileSdk = 36

    // 缂佹稒鍎抽幃鏇㈡煀瀹ュ洨鏋?(濞达綀娉曢弫?AOSP 闁稿浚鍓欓崣鈥趁圭€ｎ厾妲哥紒娑欏劤閹?
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
            // 濞达綀娉曢弫銈囩驳閹勫€抽梺鏉跨Ф閻?            signingConfig = signingConfigs.getByName("release")
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

    // 闂佽棄顦甸幏銈団偓瑙勆戦弻?Stream SDK
    implementation("com.dingtalk.open:app-stream-client:1.3.12")

    // 妞嬬偘鍔熼敍姘▏閻劏浜ら柌蹇曢獓 OkHttp WebSocket 鐎圭偟骞囬敍灞肩瑝閸愬秳绶风挧鏍х暭閺?SDK

    // 缂冩垹绮剁拠閿嬬湴閸?WebSocket
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // JSON 瑙ｆ瀽
    implementation("com.google.code.gson:gson:2.10.1")

    // Glide 闁搞儱澧芥晶鏍礉閻樼儤绁伴幖瀛樻惈缁辨瑩鎮介妸銈囪壘缂傚倹鎸搁悺銊╁椽鐏炶偐鍠橀柛鏍ㄧ墱缂傚鎮鹃妷銉︾闁告梻濮惧ù鍥晬?
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // WorkManager 閻庤纰嶅鍌涚鐠囨彃顫ら柨娑樼墢閺併倖绂嶆惔婵堢婵炶弓绱槐?    implementation("androidx.work:work-runtime:2.9.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
