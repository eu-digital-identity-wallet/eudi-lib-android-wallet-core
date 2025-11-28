/*
 * Copyright (c) 2024-2025 European Commission
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.android.build.api.dsl.LibraryExtension
import com.android.build.gradle.internal.tasks.factory.dependsOn

import com.github.jk1.license.filter.DependencyFilter
import com.github.jk1.license.filter.ExcludeTransitiveDependenciesFilter
import com.github.jk1.license.filter.LicenseBundleNormalizer
import com.github.jk1.license.filter.ReduceDuplicateLicensesFilter
import com.github.jk1.license.render.InventoryMarkdownReportRenderer

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

import java.util.Locale

import kotlin.String

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    // alias(libs.plugins.builtin.kotlin)
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-parcelize")
    alias(libs.plugins.gradle.publish)
    alias(libs.plugins.dependency.license.report)
    alias(libs.plugins.dependencycheck)
    alias(libs.plugins.sonarqube)
    alias(libs.plugins.kover)
    alias(libs.plugins.dokka.javadoc)
    alias(libs.plugins.dokka.html)
}

@Suppress("PropertyName") val GHR_URL: String by project
@Suppress("PropertyName") val GROUP: String by project
@Suppress("PropertyName") val NAMESPACE: String by project
@Suppress("PropertyName") val VERSION_NAME: String by project
@Suppress("PropertyName") val POM_ARTIFACT_ID: String by project
@Suppress("PropertyName") val POM_NAME: String by project
@Suppress("PropertyName") val POM_DESCRIPTION: String by project
@Suppress("PropertyName") val POM_URL: String by project
@Suppress("PropertyName") val POM_LICENSE_NAME: String by project
@Suppress("PropertyName") val POM_LICENSE_URL: String by project
@Suppress("PropertyName") val POM_SCM_URL: String by project
@Suppress("PropertyName") val POM_SCM_CONNECTION: String by project
@Suppress("PropertyName") val POM_SCM_DEV_CONNECTION: String by project
@Suppress("PropertyName") val POM_ISSUE_SYSTEM: String by project
@Suppress("PropertyName") val POM_ISSUE_URL: String by project
@Suppress("PropertyName") val POM_DEVELOPER_URL: String by project
@Suppress("PropertyName") val GITHUB_DEV: String by project
@Suppress("PropertyName") val GITHUB_EMAIL: String by project
@Suppress("PropertyName") val GITHUB_HANDLE: String by project

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        optIn.add("kotlin.RequiresOptIn")
        optIn.add("kotlin.time.ExperimentalTime")
        freeCompilerArgs = listOf("-XXLanguage:+PropertyParamAnnotationDefaultTargetMode")
    }
}

android {
    namespace = NAMESPACE
    group = GROUP
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testApplicationId = "$NAMESPACE.test"
        testHandleProfiling = true
        testFunctionalTest = true
    }

    @Suppress("UnstableApiUsage")
    sourceSets {
        named("main") {
            java.directories.add("src/main/java")
        }
        named("test") {
            res.directories.add("resources")
        }
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = false
        }
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    packaging {
        resources {
            excludes += listOf("/META-INF/{AL2.0,LGPL2.1}")
            excludes += listOf("/META-INF/versions/9/OSGI-INF/MANIFEST.MF")
        }
    }
}

dependencies {

    implementation(libs.androidx.appcompat)

    // EUDI libs
    api(libs.eudi.document.manager)
    api(libs.eudi.iso18013.data.transfer)
    api(libs.eudi.lib.jvm.openid4vci.ktx)

    // multipaz library
    api(libs.multipaz.android) {
        exclude(group = "org.bouncycastle")
        exclude(group = "io.ktor")
    }

    // OpenID4VCI
    implementation(libs.nimbus.oauth2.oidc.sdk)

    // Siop-Openid4VP library
    implementation(libs.eudi.lib.jvm.siop.openid4vp.ktx) {
        exclude(group = "org.bouncycastle")
    }
    // SD-JWT VC library
    implementation(libs.eudi.lib.jvm.sdjwt.ktx)

    // Document status
    api(libs.eudi.lib.kmp.statium)

    // Digital Credential API
    implementation(libs.androidx.credentials.registry.provider)
    implementation(libs.play.services.identity.credentials)

    implementation(libs.kotlinx.io.core)
    implementation(libs.kotlinx.io.bytestring)

    // CBOR
    implementation(libs.cbor)
    implementation(libs.upokecenter.cbor)
    implementation(libs.cose.java)

    // Ktor Android Engine
    implementation(libs.ktor.client.logging)
    runtimeOnly(libs.ktor.client.android)

    // Bouncy Castle
    implementation(libs.bundles.bouncy.castle)

    testImplementation(kotlin("test"))
    testImplementation(libs.robolectric)
    testImplementation(libs.mockk)
    testImplementation(libs.json)
    testImplementation(libs.kotlin.coroutines.test)
    testImplementation(libs.androidx.biometric.ktx)

    debugImplementation(libs.androidx.test.core.ktx)
    debugImplementation(libs.androidx.test.ext.junit)
    debugImplementation(libs.androidx.test.monitor)

    androidTestImplementation(libs.bundles.androidx.test)
    androidTestImplementation(libs.bundles.androidx.test.espresso)
    androidTestImplementation(libs.mockito.android)

    // Dokka Android Documentation Plugin
    dokkaPlugin(libs.dokka.android.documentation.plugin)
}

// Dokka generation
dokka {
    dokkaSourceSets.named("main") {
        val sdkComponents = androidComponents::sdkComponents.get()
        val sdkDirectory: Directory? = sdkComponents.sdkDirectory.get()
        val compileSdk = project.extensions.getByType<LibraryExtension>().compileSdk
        sourceRoots.from(
            files(File("${sdkDirectory}/platforms/${compileSdk}/android.jar")),
            "${projectDir.absolutePath}/src/main/java"
        )
        sourceLink {
            localDirectory.set(file("${projectDir.absolutePath}/src/main/java"))
            remoteUrl("https://github.com/eu-digital-identity-wallet/tree/master/wallet-core/src/main/java")
            remoteLineSuffix.set("#L")
        }
    }
    dokkaSourceSets.configureEach {
        enableJdkDocumentationLink.set(true)
        enableKotlinStdLibDocumentationLink.set(true)
        enableAndroidDocumentationLink.set(true)
        jdkVersion.set(17)
        dokkaPublications.javadoc {
            moduleName.set(project.name)
            moduleVersion.set(project.version.toString())
            outputDirectory.set(layout.buildDirectory.dir("dokka/javadoc"))
        }
        dokkaPublications.html {
            moduleName.set(project.name)
            moduleVersion.set(project.version.toString())
            outputDirectory.set(layout.buildDirectory.dir("dokka/html"))
        }
    }
}

val dokkaGenerateJavadocJar by tasks.registering(Jar::class) {
    group = "dokka"
    dependsOn(tasks.dokkaGeneratePublicationJavadoc)
    from(tasks.dokkaGeneratePublicationJavadoc.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
    description = "Assembles a JAR containing the Javadoc-style documentation generated by Dokka."
}

val dokkaGenerateHtmlJar by tasks.registering(Jar::class) {
    group = "dokka"
    dependsOn(tasks.dokkaGeneratePublicationHtml)
    from(tasks.dokkaGeneratePublicationHtml.flatMap { it.outputDirectory })
    archiveClassifier.set("html-docs")
    description = "Assembles a JAR containing the HTML documentation generated by Dokka."
}

val dokkaCleanJavadoc by tasks.registering(Delete::class) {
    group = "dokka"
    delete = setOf(rootProject.file("docs/javadoc"))
    description = "It removes the documentation generated by Dokka."
}

val dokkaCleanHtml by tasks.registering(Delete::class) {
    group = "dokka"
    delete = setOf(rootProject.file("docs/html"))
    description = "It removes the documentation generated by Dokka."
}

tasks.dokkaGeneratePublicationJavadoc.dependsOn(dokkaCleanJavadoc)
tasks.dokkaGeneratePublicationHtml.dependsOn(dokkaCleanHtml)

val dokkaClean by tasks.registering {
    group = "dokka"
    dependsOn(dokkaCleanJavadoc, dokkaCleanHtml)
}

tasks.withType<Jar>().configureEach {
    archiveBaseName.set(POM_ARTIFACT_ID)
    archiveVersion.set(VERSION_NAME)
}

// Dependency check
dependencyCheck {
    formats = listOf("XML", "HTML")
    nvd.apiKey = System.getenv("NVD_API_KEY") ?: properties["nvdApiKey"]?.toString() ?: ""
    nvd.delay = 10000
    nvd.maxRetryCount = 2
}

// Third-party licenses report
licenseReport {
    unionParentPomLicenses = false
    filters = arrayOf<DependencyFilter>(
        LicenseBundleNormalizer(),
        ReduceDuplicateLicensesFilter(),
        ExcludeTransitiveDependenciesFilter()
    )
    configurations = arrayOf("releaseRuntimeClasspath")
    excludeBoms = true
    excludeOwnGroup = true
    renderers = arrayOf(InventoryMarkdownReportRenderer("licenses.md", POM_DESCRIPTION))
}

tasks.generateLicenseReport.configure {
    doLast {
        copy {
            from(layout.buildDirectory.file("reports/dependency-license/licenses.md"))
            into(rootDir)
        }
    }
}

// Build documentation and license report
tasks.register<Task>("buildDocumentation") {
    group = "documentation"
    dependsOn("dokkaGenerate", "generateLicenseReport")
    description = "Aggregates all documentation tasks (Dokka, License Report)."
}

tasks.assemble.configure {
    finalizedBy("buildDocumentation")
}

val sourcesJar by tasks.registering(Jar::class) {
    from("${projectDir.absolutePath}/src/main/java")
    archiveClassifier.set("sources")
}

// Gradle 9.1 deprecation fix
configurations {
    @Suppress("UnstableApiUsage")
    consumable("jars") {
        outgoing.artifact(dokkaGenerateJavadocJar)
        outgoing.artifact(sourcesJar)
    }
}

tasks.named("assemble") {
    dependsOn(dokkaGenerateJavadocJar)
    dependsOn(sourcesJar)
}

// Publish
afterEvaluate {
    configure<PublishingExtension> {
        repositories {
            maven {
                name = "GitHubPackages"
                url = uri(GHR_URL)
                credentials {
                    username = System.getenv("GITHUB_ACTOR")
                    password = System.getenv("GITHUB_TOKEN")
                }
            }
        }
        publications {
            register<MavenPublication>("ReleaseAar") {
                groupId = GROUP
                artifactId = POM_ARTIFACT_ID
                version = VERSION_NAME
                artifact(tasks.getByName("bundleReleaseAar"))
                pom {
                    name = POM_NAME
                    description = POM_DESCRIPTION
                    url = POM_URL
                    scm {
                        connection = POM_SCM_CONNECTION
                        developerConnection = POM_SCM_DEV_CONNECTION
                        url = POM_SCM_URL
                    }
                    developers {
                        developer {
                            name = GITHUB_DEV
                            email = GITHUB_EMAIL
                            id = GITHUB_HANDLE
                        }
                    }
                    licenses {
                        license {
                            name = POM_LICENSE_NAME
                            url = POM_LICENSE_URL
                        }
                    }
                }
            }
        }
    }
}

val coverageExclusions = listOf(
    "**/databinding/*Binding.*",
    "**/R.class",
    "**/R$*.class",
    "**/BuildConfig.*",
    "**/Manifest*.*",
    "**/*Test*.*",
    "android/**/*.*",
    // butterKnife
    $$"**/*$ViewInjector*.*",
    $$"**/*$ViewBinder*.*",
    "**/Lambda$*.class",
    "**/Lambda.class",
    "**/*Lambda.class",
    "**/*Lambda*.class",
    "**/*_MembersInjector.class",
    "**/Dagger*Component*.*",
    "**/*Module_*Factory.class",
    "**/di/module/*",
    "**/*_Factory*.*",
    "**/*Module*.*",
    "**/*Dagger*.*",
    "**/*Hilt*.*",
    // kotlin
    "**/*MapperImpl*.*",
    $$"**/*$ViewInjector*.*",
    $$"**/*$ViewBinder*.*",
    "**/BuildConfig.*",
    "**/*Component*.*",
    "**/*BR*.*",
    "**/Manifest*.*",
    $$"**/*$Lambda$*.*",
    "**/*Companion*.*",
    "**/*Module*.*",
    "**/*Dagger*.*",
    "**/*Hilt*.*",
    "**/*MembersInjector*.*",
    "**/*_MembersInjector.class",
    "**/*_Factory*.*",
    "**/*_Provide*Factory*.*",
    "**/*Extensions*.*"
)

fun String.capitalize() =
    replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
