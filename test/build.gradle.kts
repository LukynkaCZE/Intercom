plugins {
    kotlin("jvm")
}

group = "cz.lukynka.intercom.test"
version = "0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.slf4j:slf4j-nop:2.0.9")
    implementation(project(":client"))
    implementation(project(":server"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}