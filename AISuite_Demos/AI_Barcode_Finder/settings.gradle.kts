// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

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
        maven{
            url =uri("https://zebratech.jfrog.io/artifactory/emc-mvn-ext")
        }
        flatDir {
            dirs("libs")
        }
    }
}

rootProject.name = "AI_Barcode_Finder"
include(":app")
