import com.vanniktech.maven.publish.SonatypeHost

plugins {
    kotlin("jvm") version "2.1.10"
    alias(libs.plugins.detekt)
    jacoco
    id("com.github.ben-manes.versions") version "0.51.0"
    id("com.vanniktech.maven.publish") version "0.30.0"
}

group = "me.haroldmartin.detektrules"
version = "0.1.7"

dependencies {
    compileOnly(libs.detekt.api)

    testImplementation(libs.detekt.test)
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.8")

    detektPlugins(libs.detekt.formatting)
    detektPlugins(libs.detekt.ruleauthors)
    detektPlugins(rootProject)
}

kotlin {
    jvmToolchain(11)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    systemProperty("junit.jupiter.testinstance.lifecycle.default", "per_class")
    systemProperty("compile-snippet-tests", project.hasProperty("compile-test-snippets"))
}

tasks.jacocoTestReport {
    dependsOn(tasks.test) // tests are required to run before generating the report
    reports {
        xml.required.set(true)
        csv.required.set(false)
        html.required.set(false)
    }
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    if (properties["jitpack"] != "true") {
        signAllPublications()
        coordinates("me.haroldmartin", "hbmartin-detekt-rules", version as String)
    } else {
        coordinates("me.haroldmartin.detektrules", "hbmartin-detekt-rules", version as String)
    }


    pom {
        name.set("Hbmartin's Detekt Rules")
        description.set("A somewhat opinionated ruleset for Detekt, primarily intended to avoid crashes and bugs related to mutability.")
        inceptionYear.set("2023")
        url.set("https://github.com/hbmartin/hbmartin-detekt-rules")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("hmartin")
                name.set("Harold Martin")
                url.set("https://github.com/hbmartin/")
            }
        }
        scm {
            url.set("https://github.com/hbmartin/hbmartin-detekt-rules/")
            connection.set("scm:git:git://github.com/hbmartin/hbmartin-detekt-rules.git")
            developerConnection.set("scm:git:ssh://git@github.com/hbmartin/hbmartin-detekt-rules.git")
        }
    }
}


detekt {
    allRules = true
}

tasks.withType<com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask> {
    checkForGradleUpdate = true
    rejectVersionIf {
        listOf("-RC", "-Beta", "-M1", "-M2").any { word ->
            candidate.version.contains(word)
        }
    }
}

task("printVersion") {
    doLast {
        print(version)
    }
}
