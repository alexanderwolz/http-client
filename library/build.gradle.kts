plugins {
    id("java-library")
    kotlin("jvm")
    id("maven-publish")
    signing
}

base {
    archivesName.set("http-client")
}

java {
    withSourcesJar()
    withJavadocJar()
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    api("org.slf4j:slf4j-api:2.0.17")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

//see also https://github.com/gradle-nexus/publish-plugin/tree/v2.0.0
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "xsd-generator"
            pom {
                name.set("XSD Generator")
                description.set("Generates Java classes from XML Schema files (XSD) using Kotlin, JaxB and XJC")
                url.set("https://github.com/alexanderwolz/xsd-generator")
                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
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
                    connection.set("scm:git:https://github.com/alexanderwolz/xsd-generator.git")
                    developerConnection.set("scm:git:ssh://git@github.com/alexanderwolz/xsd-generator.git")
                    url.set("https://github.com/alexanderwolz/xsd-generator")
                }
            }
        }
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications["mavenJava"])
}
