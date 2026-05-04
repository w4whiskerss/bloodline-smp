plugins {
    id("fabric-loom") version "1.13.3"
    `maven-publish`
}

val repoRoot = projectDir.parentFile
val versionFile = repoRoot.resolve("version.properties")
val bumpScript = repoRoot.resolve("tools/bump-version.ps1")

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
        repoRoot.absolutePath
    ).inheritIO().start()
    check(process.waitFor() == 0) { "Automatic version bump failed." }
}

version = readProjectVersion(versionFile)
group = property("maven_group") as String

base {
    archivesName.set(property("archives_base_name") as String)
}

repositories {
    maven("https://maven.fabricmc.net/") {
        name = "Fabric"
    }
    mavenCentral()
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings("net.fabricmc:yarn:${property("yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-key-binding-api-v1:1.1.7+4fc5413f3e")
    modImplementation("net.fabricmc.fabric-api:fabric-lifecycle-events-v1:2.6.15+4ebb5c083e")
    modImplementation("net.fabricmc.fabric-api:fabric-networking-api-v1:5.1.5+ae1e07683e")
    modImplementation("net.fabricmc.fabric-api:fabric-rendering-v1:16.2.9+7edacff13e")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}
