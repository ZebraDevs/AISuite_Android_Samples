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
            val user =  properties.getProperty("gpr.user") ?: System.getenv("GPR_USERNAME")
            val token = properties.getProperty("gpr.key") ?: System.getenv("GPR_TOKEN")
            credentials {
                username =user
                password = token
            }
            url =uri("https://artifactory-apac.zebra.com/artifactory/emc-mvn-rel")
        }
        flatDir {
            dirs("libs")
        }
    }
}

rootProject.name = "AI Proof of Delivery"
include(":app")
