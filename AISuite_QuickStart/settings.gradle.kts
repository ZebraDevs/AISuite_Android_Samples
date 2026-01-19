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
        maven{
            credentials {
                username = "username" //email id registered with zebra.com
                password  = "password" //artifactory token generated
            }
            url =uri("https://artifactory-apac.zebra.com/artifactory/emc-mvn-rel")
        }
        flatDir {
            dirs("libs")
        }
    }
}

rootProject.name = "AISuite_QuickStart"
include(":app")
 