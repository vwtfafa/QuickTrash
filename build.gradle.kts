plugins {
    java
    checkstyle
    id("com.github.spotbugs") version "6.5.10"
}

group = "org.vwtfafa"
version = "1.0.0"

repositories {
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.112-stable")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:deprecation")
}

checkstyle {
    toolVersion = "10.26.0"
    configDirectory = file("config/checkstyle")
}

spotbugs {
    toolVersion = "4.10.3"
    ignoreFailures = false
    effort = com.github.spotbugs.snom.Effort.valueOf("MAX")
    reportLevel = com.github.spotbugs.snom.Confidence.valueOf("LOW")
    reportsDir = file("build/reports/spotbugs")
    includeFilter = file("config/spotbugs/spotbugs.xml")
}
