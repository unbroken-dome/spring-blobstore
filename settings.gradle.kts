val kotlinVersion: String by settings

rootProject.name = "spring-blobstore-parent"

pluginManagement {
    repositories {
        gradlePluginPortal()
        jcenter()
    }

    resolutionStrategy.eachPlugin {
        if (requested.id.namespace!!.startsWith("org.jetbrains.kotlin")) {
            useVersion(kotlinVersion)
        }
    }
}


include(

    "spring-blobstore",
    "spring-blobstore-boot-test",
    "spring-blobstore-gcs"
)
