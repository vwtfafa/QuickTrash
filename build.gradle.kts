plugins {
    java
    checkstyle
    id("com.github.spotbugs") version "6.5.10"
    id("com.gradleup.shadow") version "9.6.1"
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
    compileOnly("io.papermc.paper:paper-api:26.2.build.116-stable")

    implementation("org.bstats:bstats-bukkit:3.2.1")

    testImplementation("io.papermc.paper:paper-api:26.2.build.116-stable")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.mockito:mockito-core:5.23.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("-Dnet.bytebuddy.experimental=true")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:deprecation")
}

tasks.processResources {
    val props = mapOf("version" to project.version)

    inputs.properties(props)

    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.shadowJar {
    archiveBaseName.set("QuickTrash")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("")

    dependencies {
        exclude { it.moduleGroup != "org.bstats" }
    }

    relocate("org.bstats", "${project.group}.shaded.bstats")
}

tasks.build {
    dependsOn(tasks.shadowJar)
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