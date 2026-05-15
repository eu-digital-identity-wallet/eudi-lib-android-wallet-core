plugins {
    id("eudi.android-library")
    id("eudi.publishing")
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-parcelize")
}

android {
    // base opt-ins already in the convention plugin; this module only needs its
    // unique test resource location
    sourceSets.getByName("test").apply {
        res.setSrcDirs(files("resources"))
    }
}

dependencies {
    api(project(":document-manager"))
    api(project(":transfer-manager"))

    api(libs.eudi.lib.jvm.openid4vci.kt)
    api(libs.multipaz.android) {
        exclude(group = "org.bouncycastle")
        exclude(group = "io.ktor")
    }
    implementation(libs.multipaz.longfellow)

    implementation(libs.appcompat)
    implementation(libs.nimbus.oauth2.oidc.sdk)
    implementation(libs.eudi.lib.jvm.siop.openid4vp.kt) {
        exclude(group = "org.bouncycastle")
    }
    // SD-JWT VC library
    api(libs.eudi.lib.jvm.sdjwt.kt)

    api(libs.eudi.lib.kmp.statium)

    // ETSI Trusted Lists
    api(libs.eudi.lib.kmp.etsi1196x2.consultation)

    // Digital Credential API
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.registry.provider)

    implementation(libs.kotlinx.io.core)
    implementation(libs.kotlinx.io.bytestring)

    implementation(libs.cbor)
    implementation(libs.upokecenter.cbor)
    implementation(libs.cose.java)

    implementation(libs.ktor.client.logging)
    implementation(libs.bouncy.castle.prov)
    implementation(libs.bouncy.castle.pkix)

    runtimeOnly(libs.ktor.client.android)

    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.json)
    testImplementation(libs.kotlin.coroutines.test)
    testImplementation(libs.biometric.ktx)
    testImplementation(libs.robolectric)

    androidTestImplementation(libs.android.junit)
    androidTestImplementation(libs.mockito.android)
    androidTestImplementation(libs.kotlin.coroutines.test)
    androidTestImplementation(libs.test.core)
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.test.rules)
    androidTestImplementation(libs.test.coreKtx)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.espresso.contrib)
    androidTestImplementation(libs.espresso.intents)
}
