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

        // Mapbox repository with authentication
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            authentication {
                create<BasicAuthentication>("basic")
            }
            credentials {
                username = "mapbox"
                // Read from local.properties file
                password = providers.fileContents(layout.rootDirectory.file("local.properties"))
                    .asText
                    .orNull
                    ?.lines()
                    ?.find { it.startsWith("MAPBOX_DOWNLOADS_TOKEN=") }
                    ?.substringAfter("=")
                    ?: ""
            }
        }
    }
}

rootProject.name = "RideSharingApp"
include(":app")
 