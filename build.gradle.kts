plugins {
    kotlin("jvm") version "2.2.10"
    id("java-library")
    id("io.github.gradle-nexus.publish-plugin") version "1.3.0"
    id("maven-publish")
    signing
}

group = "de.alexanderwolz"
version = "1.3.4"

repositories {
    mavenCentral()
}

java {
    withSourcesJar()
    withJavadocJar()
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("de.alexanderwolz:commons-log:1.0.0")
    implementation("de.alexanderwolz:commons-util:1.2.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.81")
    implementation("com.google.code.gson:gson:2.13.1")

    testImplementation(kotlin("test"))
    testImplementation("org.slf4j:slf4j-simple:2.0.17")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("com.auth0:java-jwt:4.4.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to "Alexander Wolz",
            "Built-By" to System.getProperty("user.name"),
            "Built-JDK" to System.getProperty("java.version"),
            "Created-By" to "Gradle ${gradle.gradleVersion}"
        )
    }
}

//see also https://github.com/gradle-nexus/publish-plugin/tree/v2.0.0
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name.set("HTTP Client")
                description.set("Sophisticated http client wrapper")
                url.set("https://github.com/alexanderwolz/http-client")
                licenses {
                    license {
                        name.set("AGPL-3.0")
                        url.set("https://www.gnu.org/licenses/agpl-3.0.html")
                    }
                }
                developers {
                    developer {
                        id.set("alexanderwolz")
                        name.set("Alexander Wolz")
                        url.set("https://www.alexanderwolz.de")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/alexanderwolz/http-client.git")
                    developerConnection.set("scm:git:ssh://git@github.com/alexanderwolz/http-client.git")
                    url.set("https://github.com/alexanderwolz/http-client")
                }
            }
        }
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications["mavenJava"])
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
        }
    }
}

