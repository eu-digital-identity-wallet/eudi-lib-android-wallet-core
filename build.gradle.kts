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

import com.github.jk1.license.filter.ExcludeTransitiveDependenciesFilter
import com.github.jk1.license.filter.LicenseBundleNormalizer
import com.github.jk1.license.filter.ReduceDuplicateLicensesFilter
import com.github.jk1.license.render.InventoryMarkdownReportRenderer

plugins {
    alias(libs.plugins.dependency.license.report)
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.dependencycheck) apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.maven.publish) apply false
}

licenseReport {
    projects = project.subprojects.toTypedArray()
    unionParentPomLicenses = false
    excludeBoms = true
    excludeOwnGroup = true
    filters = arrayOf(
        LicenseBundleNormalizer(),
        ReduceDuplicateLicensesFilter(),
        ExcludeTransitiveDependenciesFilter()
    )
    configurations = arrayOf("releaseRuntimeClasspath")
    renderers = arrayOf(InventoryMarkdownReportRenderer("licenses.md", "EUDI Wallet Libraries"))
}

tasks.generateLicenseReport.configure {
    doLast {
        copy {
            from(layout.buildDirectory.file("reports/dependency-license/licenses.md"))
            into(rootDir)
        }
    }
}
