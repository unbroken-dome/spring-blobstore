import groovy.lang.Closure

plugins {
    `java-library`
    kotlin("jvm")
    kotlin("plugin.spring")
    id("com.jfrog.bintray")
}

val optional: Closure<*> by extra

dependencies {
    api("org.springframework:spring-core")
    api("io.projectreactor:reactor-core")

    compileOnly("org.springframework.boot:spring-boot-autoconfigure")

    testImplementation("io.projectreactor:reactor-test")
}
