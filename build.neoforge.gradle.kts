plugins {
    id("dev.architectury.loom") version "1.17.487"
}

stonecutter {
    val (_, loader) = stonecutter.current.project.split("-", limit = 2)
    properties.tags(stonecutter.current.version, loader)
    constants.match(loader, "fabric", "neoforge")
}

val minecraft: String = stonecutter.current.version
val mcVersionProp: String = property("minecraft_version").toString()

fun accessWidenerSource(): java.io.File = rootProject.file(
    when (minecraft) {
        "1.21.1" -> "gradle/accesswideners/editvillager-1.21.1.accesswidener"
        "1.21.11" -> "gradle/accesswideners/editvillager-1.21.11.accesswidener"
        else -> "gradle/accesswideners/editvillager-1.21.10.accesswidener"
    },
)

version = "${property("mod.version")}+$minecraft"
group = property("mod.group").toString()

base {
    archivesName.set("${property("mod.id")}-neoforge")
}

repositories {
    mavenCentral()
    maven { url = uri("https://api.modrinth.com/maven") }
    maven { url = uri("https://maven.neoforged.net/releases/") }
    maven { url = uri("https://maven.fabricmc.net/") }
    maven { url = uri("https://maven.architectury.dev/") }
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft")
    mappings(
        loom.layered {
            mappings("net.fabricmc:yarn:${property("yarn_mappings")}:v2")
            mappings("dev.architectury:yarn-mappings-patch-neoforge:${property("neoforge_patch")}")
        }
    )
    neoForge("net.neoforged:neoforge:${property("neoforge_version")}")
    modImplementation("maven.modrinth:architectury-api:${property("architectury_version")}+neoforge")
}

loom {
    mixin {
        defaultRefmapName.set("editvillager.refmap.json")
        useLegacyMixinAp.set(true)
    }
    accessWidenerPath.set(accessWidenerSource())
    runs {
        named("client") {
            client()
            ideConfigGenerated(true)
            runDir("run/")
        }
        named("server") {
            server()
            ideConfigGenerated(true)
            runDir("run/")
        }
    }
}

tasks.named("validateAccessWidener") { enabled = false }

tasks.named<JavaCompile>("compileJava") {
    dependsOn("stonecutterGenerate")
}

tasks.named<ProcessResources>("processResources") {
    dependsOn("stonecutterGenerate")
}

tasks.withType<Jar>().configureEach {
    if (name == "sourcesJar") {
        dependsOn("stonecutterGenerate")
    }
}

sourceSets {
    named("main") {
        java {
            setSrcDirs(
                listOf(
                    layout.buildDirectory.dir("generated/stonecutter/main/java"),
                ),
            )
            exclude("**/fabric/**")
        }
        resources {
            setSrcDirs(
                listOf(
                    layout.buildDirectory.dir("generated/stonecutter/main/resources"),
                    rootProject.file("src/main/resources/assets"),
                ),
            )
        }
    }
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("editvillager.mixins.json") {
        filter { line ->
            if (line.contains("__REFMAP__")) {
                "\"refmap\": \"editvillager.refmap.json\","
            } else {
                val trimmed = line.trim()
                when {
                    trimmed.startsWith("//?") -> ""
                    else -> line
                }
            }
        }
    }
    from(accessWidenerSource()) {
        into(".")
        rename { "editvillager.accesswidener" }
    }
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(
            mapOf(
                "id" to rootProject.property("mod.id"),
                "name" to rootProject.property("mod.name"),
                "version" to rootProject.property("mod.version"),
                "minecraft" to mcVersionProp
            )
        )
    }
}

tasks.register<Copy>("stageNeoForgeRefmap") {
    val staticRefmap = rootProject.file("gradle/refmaps/editvillager-$minecraft-neoforge.refmap.json")
    onlyIf { staticRefmap.exists() }
    from(staticRefmap)
    into(layout.buildDirectory.dir("refmap-staging"))
    rename { "editvillager.refmap.json" }
}

tasks.register<Copy>("injectNeoForgeRefmap") {
    dependsOn("classes")
    val staticRefmap = rootProject.file("gradle/refmaps/editvillager-$minecraft-neoforge.refmap.json")
    onlyIf { staticRefmap.exists() }
    from(staticRefmap)
    into(layout.buildDirectory.dir("classes/java/main"))
    rename { "editvillager.refmap.json" }
}

tasks.jar {
    dependsOn("injectNeoForgeRefmap")
    from("LICENSE") {
        rename { "${it}_${project.base.archivesName.get()}" }
    }
}

tasks.register("injectRefmapIntoRemapJar") {
    dependsOn("remapJar", "stageNeoForgeRefmap")
    val staticRefmap = rootProject.file("gradle/refmaps/editvillager-$minecraft-neoforge.refmap.json")
    onlyIf { staticRefmap.exists() }
    doLast {
        val jarFile = tasks.remapJar.get().archiveFile.get().asFile
        val stagingDir = layout.buildDirectory.dir("refmap-staging").get().asFile
        val refmapFile = stagingDir.resolve("editvillager.refmap.json")
        if (!refmapFile.exists()) {
            throw GradleException("Missing staged refmap at ${refmapFile.absolutePath}")
        }
        val process = ProcessBuilder(
            "jar",
            "uf",
            jarFile.absolutePath,
            "-C",
            stagingDir.absolutePath,
            "editvillager.refmap.json",
        ).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        if (process.waitFor() != 0) {
            throw GradleException("Failed to inject refmap into ${jarFile.name}: $output")
        }
    }
}

tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar") {
    finalizedBy("injectRefmapIntoRemapJar")
}

val buildAndCollect = tasks.register<Copy>("buildAndCollect") {
    group = "versioned"
    from(tasks.remapJar.flatMap { it.archiveFile }, tasks.remapSourcesJar.flatMap { it.archiveFile })
    into(rootProject.layout.buildDirectory.dir("libs/${property("mod.version")}/neoforge"))
    dependsOn("build")
}

tasks.build {
    group = "versioned"
}

if (stonecutter.current?.isActive == true) {
    rootProject.tasks.register("buildActive") {
        group = "project"
        dependsOn(buildAndCollect)
    }
}
