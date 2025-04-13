// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}
buildscript {
    dependencies {
        classpath(libs.google.services)
    }
}

val secretsFile = rootProject.file("secrets.properties")
if (secretsFile.exists()) {
    secretsFile.forEachLine { line ->
        val (key, value) = line.split("=")
        project.extensions.extraProperties[key.trim()] = value.trim()
    }
}
