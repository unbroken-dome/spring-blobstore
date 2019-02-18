plugins {
    `java-library`
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.unbroken-dome.test-sets")
    id("com.jfrog.bintray")
}


testSets {
    "autoConfTest" { dirName = "autoconf-test" }
}


dependencies {
    api(project(":spring-blobstore"))

    implementation("org.springframework:spring-webflux")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.unbroken-dome.jsonwebtoken:jwt")

    implementation("org.bouncycastle:bcpkix-jdk15on")

    compileOnly("org.springframework.boot:spring-boot-autoconfigure")
    compileOnly("javax.validation:validation-api")


    testImplementation("io.projectreactor:reactor-test")

    "autoConfTestImplementation"("org.springframework.boot:spring-boot-starter-test")
    "autoConfTestRuntimeOnly"("io.projectreactor.netty:reactor-netty")
}
