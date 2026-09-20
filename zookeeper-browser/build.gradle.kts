plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.17.4"
}

group = "com.zookeeper.browser"
version = "1.0.8"

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation("org.apache.zookeeper:zookeeper:3.9.3") {
        exclude(group = "ch.qos.logback")
        exclude(group = "log4j")
        exclude(group = "org.slf4j")
    }
    implementation("com.google.code.gson:gson:2.11.0")
}

intellij {
    version.set("2023.3.8")
    type.set("IC")
    plugins.set(listOf())
    instrumentCode.set(false)
}

tasks {
    patchPluginXml {
        sinceBuild.set("233")
        untilBuild.set("262.*")
    }

    buildSearchableOptions {
        enabled = false
    }
}
