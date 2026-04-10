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
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
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
        maven {
            url =uri("https://zebratech.jfrog.io/artifactory/emc-mvn-ext")
        }
        flatDir {
            dirs("libs")
        }
    }
}

rootProject.name = "AI Data Capture Demo"
include(":app")
 