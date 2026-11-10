pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Delibra"

// Cada funcionalidade é um módulo independente.
// Para remover uma funcionalidade: tire a linha daqui e a dependência no :app.
include(":app")
include(":core")
include(":feature:auth")
include(":feature:tracking")
include(":feature:corridas")
include(":feature:perfil")
include(":feature:gestor")
