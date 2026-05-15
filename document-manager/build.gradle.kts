plugins {
    id("eudi.android-library")
    id("eudi.publishing")
    alias(libs.plugins.kotlin.serialization)
}

// base opt-ins already in the convention plugin; document-manager adds one extra
kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-opt-in=kotlin.ExperimentalApi")
    }
}

dependencies {
    api(libs.multipaz) {
        exclude(group = "org.bouncycastle")
        exclude(group = "io.ktor")
    }

    implementation(libs.kotlinx.io.core)
    implementation(libs.kotlinx.io.bytestring)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.upokecenter.cbor)
    implementation(libs.cose.java)

    api(libs.eudi.lib.jvm.sdjwt.kt)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.serialization)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.nimbus.jose.jwt)

    implementation(libs.bouncy.castle.prov)
    implementation(libs.bouncy.castle.pkix)

    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.json)
    testImplementation(libs.kotlin.coroutines.test)
    testImplementation(libs.ktor.client.cio)
}
