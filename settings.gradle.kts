rootProject.name = "phantazm"

val localSettings = file("local.settings.gradle.kts")
if (localSettings.exists()) {
    //apply from local settings too, if it exists
    //can be used to sideload Minestom for faster testing
    apply(localSettings)
}

//necessary for phantazm-zombies-mapeditor module which contains a Fabric mod
pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/") {
            name = "Fabric"
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

sequenceOf(
    "core",
    "commons",
    "messaging",
    "mob",
    "neuron",
    "neuron-minestom",
    "server",
    "velocity",
    "zombies",
    "zombies-mapdata",
    "zombies-mapeditor"
).forEach {
    include(":phantazm-$it")
    project(":phantazm-$it").projectDir = file(it)
}
