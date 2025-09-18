plugins {
    kotlin("jvm") version "2.2.10"
}

group = "cz.lukynka.intercom"
version = properties["intercom.version"]!!

subprojects {
    repositories {
        mavenCentral()
        maven("https://mvn.devos.one/releases")
    }
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}