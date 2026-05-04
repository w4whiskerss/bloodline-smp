import java.net.URI
import java.net.URL

plugins {
    java
}

val versionFile = projectDir.resolve("version.properties")
val bumpScript = projectDir.resolve("tools/bump-version.ps1")

fun readProjectVersion(file: File): String {
    val content = file.readText(Charsets.UTF_8).replace("\uFEFF", "")
    return content.lineSequence()
        .map(String::trim)
        .firstOrNull { it.startsWith("project_version=") }
        ?.substringAfter('=')
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: error("Missing project_version in ${file.absolutePath}")
}

val noBumpTasks = setOf("help", "tasks", "properties", "projects", "wrapper")
val skipAutoVersionBump = gradle.startParameter.projectProperties.containsKey("skipAutoVersionBump")
val requestedTasks = gradle.startParameter.taskNames.map { it.substringAfterLast(':').lowercase() }
val shouldAutoBump = requestedTasks.isEmpty() || requestedTasks.any { it !in noBumpTasks }

if (!skipAutoVersionBump && shouldAutoBump) {
    val process = ProcessBuilder(
        "powershell",
        "-NoProfile",
        "-ExecutionPolicy",
        "Bypass",
        "-File",
        bumpScript.absolutePath,
        "-RepoRoot",
        projectDir.absolutePath
    ).inheritIO().start()
    check(process.waitFor() == 0) { "Automatic version bump failed." }
}

group = "dev.zahen"
version = readProjectVersion(versionFile)

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

tasks.named<Jar>("jar") {
    archiveBaseName.set("BloodLine Server")
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
