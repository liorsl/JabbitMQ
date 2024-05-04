plugins {
    id("java-library")
    id("maven-publish")
}

group = "dev.voigon"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            from(components["java"])
        }
    }
    repositories {
        if (project.findProperty("apartium.nexus.username") != null) {
            maven {
                if (version.toString().endsWith("-SNAPSHOT")) {
                    name = "VoigonSnapshots"
                    url = uri("https://nexus.voigon.dev/repository/voigon-snapshots/")
                } else {
                    name = "VoigonReleases"
                    url = uri("https://nexus.voigon.dev/repository/voigon-releases/")
                }

                credentials {
                    username = project.findProperty("apartium.nexus.username").toString()
                    password = project.findProperty("apartium.nexus.password").toString()
                }
            }
        }
    }
}


dependencies {
    val jacksonVersion = "2.15.2"
    compileOnly("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")

    implementation("com.rabbitmq:stream-client:0.15.0")
    implementation("com.rabbitmq:amqp-client:4.0.0")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}