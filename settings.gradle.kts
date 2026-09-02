/*
 * Desarrollado por RelajoSoft · https://relajosoft.com
 * Autor: Yoel Enrique Estevez Gonzalez
 * Repositorios oficiales y módulos incluidos en el proyecto.
 */
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // Impide que un módulo añada repositorios no revisados por su cuenta.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "RelajoSoftFloatingScanner"
include(":app")
