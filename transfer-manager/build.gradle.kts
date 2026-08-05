plugins {
    id("eudi.android-library")
    id("eudi.publishing")
    id("kotlin-parcelize")
}

dependencies {
    implementation(project(":document-manager"))

    implementation(libs.appcompat)
    implementation(libs.multipaz.android) {
        exclude(group = "org.bouncycastle")
        exclude(group = "io.ktor")
    }
    implementation(libs.multipaz.android.legacy) {
        exclude(group = "org.bouncycastle")
        exclude(group = "io.ktor")
    }

    implementation(libs.kotlinx.io.core)
    implementation(libs.kotlinx.io.bytestring)

    implementation(libs.zxing.core)

    implementation(libs.bouncy.castle.prov)
    implementation(libs.bouncy.castle.pkix)

    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.json)
    testImplementation(libs.mockito.inline)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.robolectric)
    testImplementation(libs.upokecenter.cbor)
    testImplementation(libs.cose.java)

    androidTestImplementation(libs.android.junit)
    androidTestImplementation(libs.espresso.core)
}
