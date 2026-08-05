import com.vanniktech.maven.publish.AndroidMultiVariantLibrary

plugins {
    id("com.vanniktech.maven.publish")
}

val POM_SCM_URL: String by project

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
            system.set("github")
            url.set("${POM_SCM_URL}/actions")
        }
    }
}
