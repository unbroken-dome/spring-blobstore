import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") apply false
    id("org.unbroken-dome.test-sets") version "2.1.1" apply false
    id("io.spring.dependency-management") version "1.0.7.RELEASE" apply false
    id("com.jfrog.bintray") version "1.8.4" apply false
}


allprojects {
    repositories {
        jcenter()
    }
}


val release: Task by tasks.creating {
    doLast {
        println("Releasing $version")
    }
}


if ("release" !in gradle.startParameter.taskNames) {
    println("Not a release build, setting version to ${project.version}-SNAPSHOT")
    project.version = "${project.version}-SNAPSHOT"
}


subprojects {

    plugins.withId("base") {
        apply(plugin = "io.spring.dependency-management")
    }

    plugins.withId("java") {

        configure<JavaPluginConvention> {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }
        
        dependencies {
            "compileOnly"("com.google.code.findbugs:jsr305")
            "testImplementation"("org.junit.jupiter:junit-jupiter-api")
            "testRuntimeOnly"("org.junit.jupiter:junit-jupiter-engine")
        }

        tasks.withType<Test> {
            useJUnitPlatform()
        }
    }

    plugins.withId("org.jetbrains.kotlin.jvm") {
        dependencies {
            "implementation"(kotlin("stdlib-jdk8"))

            "testImplementation"("com.willowtreeapps.assertk:assertk-jvm")
            "testImplementation"("io.mockk:mockk")
        }

        tasks.withType<KotlinCompile> {
            kotlinOptions.jvmTarget = "1.8"
            kotlinOptions.freeCompilerArgs = listOf("-Xjvm-default=enable")
        }
    }

    plugins.withId("io.spring.dependency-management") {
        apply(from = "$rootDir/gradle/dependency-management.gradle")
    }

    plugins.withId("com.jfrog.bintray") {
        apply(from = "$rootDir/gradle/publishing.gradle")
    }
}
