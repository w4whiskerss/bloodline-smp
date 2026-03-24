import java.net.URI
import java.net.URL

plugins {
    java
}

group = "dev.zahen"
version = "2.2.6"

val paperVersion = "1.21.11"
val paperBuild = "69"
val paperJarName = "paper-$paperVersion-$paperBuild.jar"
val serverDir = layout.projectDirectory.dir("run")
val serverJar = layout.buildDirectory.file("paper/$paperJarName")
val pluginJar = tasks.named<Jar>("jar").flatMap { it.archiveFile }

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
    options.compilerArgs.add("-Xlint:deprecation")
}

tasks.processResources {
    filteringCharset = "UTF-8"
}

val downloadServer by tasks.registering {
    outputs.file(serverJar)
    doLast {
        val output = serverJar.get().asFile
        if (output.exists()) {
            return@doLast
        }

        output.parentFile.mkdirs()
        val downloadUrl = "https://api.papermc.io/v2/projects/paper/versions/$paperVersion/builds/$paperBuild/downloads/$paperJarName"
        URI(downloadUrl).toURL().openStream().use { input ->
            output.outputStream().use { outputStream ->
                input.copyTo(outputStream)
            }
        }
    }
}

val prepareRunServer by tasks.registering(Copy::class) {
    dependsOn(downloadServer, tasks.named("jar"))
    into(serverDir.dir("plugins"))
    from(pluginJar)
    doFirst {
        val pluginsDir = serverDir.dir("plugins").asFile
        if (pluginsDir.exists()) {
            pluginsDir.listFiles { file -> file.name.startsWith("bloodline-smp-") && file.name.endsWith(".jar") }
                ?.forEach { it.delete() }
        }
    }
    doLast {
        val eulaFile = serverDir.file("eula.txt").asFile
        eulaFile.parentFile.mkdirs()
        eulaFile.writeText("eula=true\n")
    }
}

tasks.register<Exec>("runServer") {
    group = "application"
    description = "Downloads Paper, copies the plugin jar, and runs a local Paper test server."
    dependsOn(prepareRunServer)
    workingDir(serverDir.asFile)

    val javaLauncher = javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(21))
    }

    commandLine(
        javaLauncher.get().executablePath.asFile.absolutePath,
        "-Dcom.mojang.eula.agree=true",
        "-Xms1G",
        "-Xmx2G",
        "-jar",
        serverJar.get().asFile.absolutePath,
        "--nogui"
    )
}
