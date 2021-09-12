/**
 * Copyright (c) 2021 Noel
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties

buildscript {
    repositories {
        mavenCentral()
        jcenter()
        maven { url = uri("https://plugins.gradle.org/m2/") }
    }

    dependencies {
        classpath("com.diffplug.spotless:spotless-plugin-gradle:5.15.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.30")
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:1.4.30")
    }
}

plugins {
    id("com.diffplug.spotless") version "5.15.0"
    id("org.jetbrains.dokka") version "1.4.30"
    kotlin("jvm") version "1.5.30"
    `maven-publish`
}

group = "dev.floofy"
version = "1.0.0"

repositories {
    mavenCentral()
    mavenLocal()
}

subprojects {
    group = "dev.floofy.kairi"
    version = rootProject.version as String

    apply(plugin = "kotlin")
    apply(plugin = "maven-publish")
    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "com.diffplug.spotless")

    repositories {
        mavenCentral()
        mavenLocal()
        jcenter()
    }

    dependencies {
        // Add important Kotlin libraries
        implementation("org.jetbrains.kotlin:kotlin-stdlib")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.2")
        api("org.jetbrains.kotlinx:kotlinx-serialization-core:1.2.2")
    }

    spotless {
        kotlin {
            trimTrailingWhitespace()
            licenseHeaderFile("${rootProject.projectDir}/assets/HEADER")
            endWithNewline()

            // We can't use the .editorconfig file, so we'll have to specify it here
            // issue: https://github.com/diffplug/spotless/issues/142
            ktlint()
                .userData(mapOf(
                    "no-consecutive-blank-lines" to "true",
                    "no-unit-return" to "true",
                    "disabled_rules" to "no-wildcard-imports,colon-spacing",
                    "indent_size" to "4"
                ))
        }
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_11.toString()
        kotlinOptions.javaParameters = true
        kotlinOptions.freeCompilerArgs += listOf(
            "-Xopt-in=kotlin.RequiresOptIn"
        )
    }

    val publishingProps = try {
        Properties().apply { load(file("${rootProject.projectDir}/gradle/publishing.properties").reader()) }
    } catch(e: Exception) {
        Properties()
    }

    val sourcesJar by tasks.registering(Jar::class) {
        archiveClassifier.set("sources")
        from(sourceSets.main.get().allSource)
    }

    val dokkaJar by tasks.registering(Jar::class) {
        group = JavaBasePlugin.DOCUMENTATION_GROUP
        description = "Assemble Kotlin documentation with Dokka"

        archiveClassifier.set("javadoc")
        from(tasks.dokkaHtml)
        dependsOn(tasks.dokkaHtml)
    }

    tasks.dokkaHtml.configure {
        outputDirectory.set(file("${project.projectDir}/docs"))
        dokkaSourceSets {
            configureEach {
                platform.set(org.jetbrains.dokka.Platform.jvm)
                sourceLink {
                    localDirectory.set(file("src/main/kotlin"))
                    remoteUrl.set(
                        uri("https://github.com/auguwu/Kairi/tree/master/${project.name.replace("-", "/")}").toURL()
                    )

                    remoteLineSuffix.set("#L")
                }

                jdkVersion.set(11)
            }
        }
    }

    publishing {
        publications {
            create<MavenPublication>("Kairi") {
                from(components["kotlin"])
                groupId = "dev.floofy.kairi"
                artifactId = "kairi-${project.displayName}"
                version = rootProject.version as String

                artifact(sourcesJar.get())
                artifact(dokkaJar.get())

                pom {
                    description.set("Experimental Kotlin library for Revolt.")
                    name.set("kairi-${project.displayName}")
                    url.set("https://kairi.floofy.dev")

                    organization {
                        name.set("Noel")
                        url.set("https://floofy.dev")
                    }

                    developers {
                        developer {
                            name.set("Noel")
                        }
                    }

                    issueManagement {
                        system.set("GitHub")
                        url.set("https://github.com/auguwu/Kairi/issues")
                    }

                    licenses {
                        license {
                            name.set("MIT")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }

                    scm {
                        connection.set("scm:git:ssh://github.com/auguwu/Kairi.git")
                        developerConnection.set("scm:git:ssh://git@github.com:auguwu/Kairi.git")
                        url.set("https://github.com/auguwu/Kairi")
                    }
                }
            }
        }

        repositories {
            maven(url = "s3://maven.floofy.dev/repo/releases") {
                credentials(AwsCredentials::class.java) {
                    accessKey = publishingProps.getProperty("s3.accessKey") ?: System.getProperty("dev.floofy.s3.accessKey") ?: ""
                    secretKey = publishingProps.getProperty("s3.secretKey") ?: System.getProperty("dev.floofy.s3.secretKey") ?: ""
                }
            }
        }
    }
}
