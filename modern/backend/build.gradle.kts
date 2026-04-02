import org.gradle.api.tasks.SourceSetContainer

plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.spring") version "2.2.20"
    id("org.springframework.boot") version "4.0.0"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.example"
version = "0.1.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    runtimeOnly("org.apache.derby:derby:10.16.1.1")
    runtimeOnly("org.apache.derby:derbytools:10.16.1.1")
    runtimeOnly("org.postgresql:postgresql:42.7.8")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework:spring-webflux")
    testImplementation("com.h2database:h2")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

val sourceSets = the<SourceSetContainer>()

tasks.register("prepareParityRuntime") {
    dependsOn("classes")
    val outputFile = layout.buildDirectory.file("parity/main-runtime-classpath.txt")
    outputs.file(outputFile)
    doLast {
        val file = outputFile.get().asFile
        file.parentFile.mkdirs()
        file.writeText(
            sourceSets.named("main").get().runtimeClasspath.files
                .joinToString(System.lineSeparator()) { it.absolutePath },
        )
    }
}
