import java.util.Properties
import kotlin.apply

val properties = Properties().apply {
    file("local.properties").takeIf { it.exists() }?.reader()?.use(::load)
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
        exclusiveContent {
            forRepository {
                maven {
                    url = uri("https://software.mobile.pendo.io/artifactory/androidx-release")
                }
            }
            filter {
                includeGroup("sdk.pendo.io")
            }
        }
        maven{
            url =uri("https://zebratech.jfrog.io/artifactory/emc-mvn-ext")
        }
        flatDir {
            dirs("libs")
        }
    }
}

rootProject.name = "AI Proof of Delivery"
include(":app")
