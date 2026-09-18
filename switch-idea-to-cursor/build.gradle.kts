plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.17.4"
}

group = "com.gameale.massive.tools"
version = "1.0.1"

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
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
