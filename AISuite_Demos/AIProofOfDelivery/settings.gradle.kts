import java.io.FileInputStream
import java.util.Properties

val properties = Properties().apply {
    val localPropertiesFile = rootDir.resolve("local.properties")
    if (localPropertiesFile.exists()) {
        load(FileInputStream(localPropertiesFile))
    }
}
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        flatDir {
            dirs("libs")
        }
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url =uri("https://zebratech.jfrog.io/artifactory/emc-mvn-ext")
        }
        flatDir {
            dirs("libs")
        }
    }
}

rootProject.name = "AI Proof of Delivery"
include(":app")
