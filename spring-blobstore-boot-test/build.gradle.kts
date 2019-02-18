plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
}


dependencies {
    implementation(project(":spring-blobstore"))
    implementation(project(":spring-blobstore-gcs"))
    implementation("org.springframework.boot:spring-boot-starter")
}
