plugins {
    kotlin("jvm")
}

group = "cz.lukynka.intercom.common"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    api(libs.tide)
    api(libs.bindables)
    api(libs.bundles.logger)
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}