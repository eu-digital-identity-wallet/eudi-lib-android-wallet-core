/*
 * Copyright (c) 2024 European Commission
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

import com.android.build.gradle.api.LibraryVariant
import com.github.jk1.license.filter.ExcludeTransitiveDependenciesFilter
import com.github.jk1.license.filter.LicenseBundleNormalizer
import com.github.jk1.license.filter.ReduceDuplicateLicensesFilter
import com.github.jk1.license.render.InventoryMarkdownReportRenderer
import com.vanniktech.maven.publish.AndroidMultiVariantLibrary
import java.util.Locale

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-parcelize")
    alias(libs.plugins.dokka)
    alias(libs.plugins.dependency.license.report)
    alias(libs.plugins.dependencycheck)
    alias(libs.plugins.sonarqube)
    alias(libs.plugins.maven.publish)
    jacoco
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

val NAMESPACE: String by project
val GROUP: String by project
val POM_SCM_URL: String by project
val POM_DESCRIPTION: String by project

android {
    namespace = NAMESPACE
    group = GROUP
    compileSdk = 34

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testApplicationId = "$NAMESPACE.test"
        testHandleProfiling = true
        testFunctionalTest = true

        consumerProguardFiles("consumer-rules.pro")

    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.java.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.java.get())
    }
    kotlinOptions {
        jvmTarget = libs.versions.java.get()
    }

    sourceSets.getByName("test").apply {
        res.setSrcDirs(files("resources"))
    }

    packaging {
        resources {
            excludes += listOf("/META-INF/{AL2.0,LGPL2.1}")
            excludes += listOf("/META-INF/versions/9/OSGI-INF/MANIFEST.MF")
        }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }

    afterEvaluate {
        libraryVariants.forEach { createJacocoTasks(it) }
    }
}

dependencies {

    // EUDI libs
    api(libs.eudi.document.manager)
    api(libs.eudi.iso18013.data.transfer)
    // Identity android library
    api(libs.google.identity.android) {
        exclude(group = "org.bouncycastle")
    }

    implementation(libs.appcompat)
    // OpenID4VCI
    implementation(libs.eudi.lib.jvm.openid4vci.kt)
    implementation(libs.nimbus.oauth2.oidc.sdk)
    // Siop-Openid4VP library
    implementation(libs.eudi.lib.jvm.siop.openid4vp.kt) {
        exclude(group = "org.bouncycastle")
    }
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.io.core)
    implementation(libs.kotlinx.io.bytestring)
    // CBOR
    implementation(libs.cbor)
    implementation(libs.upokecenter.cbor)
    implementation(libs.cose.java)
    // Ktor Android Engine
    implementation(libs.ktor.client.logging)
    // Bouncy Castle
    implementation(libs.bouncy.castle.prov)
    implementation(libs.bouncy.castle.pkix)

    runtimeOnly(libs.ktor.client.android)

    testImplementation(kotlin("test"))
    testImplementation(libs.mockk)
    testImplementation(libs.json)
    testImplementation(libs.kotlin.coroutines.test)
    testImplementation(libs.biometric.ktx)

    androidTestImplementation(libs.android.junit)
    androidTestImplementation(libs.mockito.android)
    androidTestImplementation(libs.test.core)
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.test.rules)
    androidTestImplementation(libs.test.coreKtx)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.espresso.contrib)
    androidTestImplementation(libs.espresso.intents)
}

// Dependency check

dependencyCheck {
    formats = listOf("XML", "HTML")
    nvd.apiKey = System.getenv("NVD_API_KEY") ?: properties["nvdApiKey"]?.toString() ?: ""
    nvd.delay = 10000
    nvd.maxRetryCount = 2
}

// Dokka generation

tasks.dokkaGfm.configure {
    val outputDir = file("$rootDir/docs")
    doFirst { delete(outputDir) }
    outputDirectory.set(outputDir)
}

tasks.register<Jar>("dokkaHtmlJar") {
    group = "documentation"
    dependsOn(tasks.dokkaHtml)
    from(tasks.dokkaHtml.flatMap { it.outputDirectory })
    archiveClassifier.set("html-docs")
}

tasks.register<Jar>("dokkaJavadocJar") {
    group = "documentation"
    dependsOn(tasks.dokkaJavadoc)
    from(tasks.dokkaJavadoc.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
}

// Third-party licenses report

licenseReport {
    unionParentPomLicenses = false
    filters = arrayOf(
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
    dependsOn("dokkaGfm", "generateLicenseReport")
}
tasks.assemble.configure {
    finalizedBy("buildDocumentation")
}

// Publish

mavenPublishing {
    configure(
        AndroidMultiVariantLibrary(
            sourcesJar = true,
            publishJavadocJar = true,
            setOf("release")
        )
    )
    pom {
        ciManagement {
            system = "github"
            url = "${POM_SCM_URL}/actions"
        }
    }
}
// handle java.lang.UnsupportedOperationException: PermittedSubclasses requires ASM9
// when publishing module
afterEvaluate {
    tasks.named("javaDocReleaseGeneration").configure {
        enabled = false
    }
}

// Jacoco Tasks

val coverageExclusions = listOf(
    "**/databinding/*Binding.*",
    "**/R.class",
    "**/R$*.class",
    "**/BuildConfig.*",
    "**/Manifest*.*",
    "**/*Test*.*",
    "android/**/*.*",
    // butterKnife
    "**/*\$ViewInjector*.*",
    "**/*\$ViewBinder*.*",
    "**/Lambda\$*.class",
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
    "**/*\$ViewInjector*.*",
    "**/*\$ViewBinder*.*",
    "**/BuildConfig.*",
    "**/*Component*.*",
    "**/*BR*.*",
    "**/Manifest*.*",
    "**/*\$Lambda\$*.*",
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

fun createJacocoTasks(variant: LibraryVariant) {
    val testTaskName = "test${variant.name.capitalize()}UnitTest"
    val taskName = "${testTaskName}Coverage"
    val javaClasses = layout.buildDirectory.dir("intermediates/javac/${variant.name}")
        .get().asFileTree.matching {
            exclude(coverageExclusions)
        }
    val kotlinClasses = layout.buildDirectory.dir("tmp/kotlin-classes/${variant.name}")
        .get().asFileTree.matching {
            exclude(coverageExclusions)
        }
    val sourceDirs = files(
        "$projectDir/src/main/java",
        "$projectDir/src/main/kotlin",
        "$projectDir/src/${variant.name}/java",
        "$projectDir/src/${variant.name}/kotlin"
    )
    val executionDataVariant =
        layout.buildDirectory.file("/outputs/unit_test_code_coverage/${variant.name}UnitTest/${testTaskName}.exec")
            .get().asFile

    val reportTask = tasks.register<JacocoReport>(taskName) {
        group = "reporting"
        description = "Generate Jacoco coverage reports for the ${variant.name} build."
        dependsOn(testTaskName)
        reports {
            xml.required = true
            html.required = true
        }
        sourceDirectories.setFrom(sourceDirs)
        classDirectories.setFrom(javaClasses, kotlinClasses)
        executionData.setFrom(executionDataVariant)

        doLast {
            layout.buildDirectory.file("reports/jacoco/${taskName}/html/index.html")
                .get()
                .asFile
                .readText()
                .let { Regex("Total[^%]*>(\\d?\\d?\\d?%)").find(it) }
                ?.let { println("Test coverage: ${it.groupValues[1]}") }
        }
    }
    tasks.register<JacocoCoverageVerification>("${testTaskName}CoverageVerification") {
        group = "reporting"
        description = "Verifies Jacoco coverage for the ${variant.name} build."
        dependsOn(reportTask.name)

        violationRules {
            rule {
                limit {
                    minimum = 80.toBigDecimal()
                }
            }
        }

        classDirectories.setFrom(kotlinClasses, javaClasses)
        sourceDirectories.setFrom(sourceDirs)
        executionData.setFrom("${layout.buildDirectory.get()}$executionDataVariant")
    }
}