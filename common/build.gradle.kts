plugins {
    kotlin("jvm")
}

group = "cz.lukynka.intercom.common"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(libs.tide)
    implementation(libs.bindables)
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}