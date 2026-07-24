plugins {
    id("dev.architectury.loom") version "1.17.487"
}

stonecutter {
    val (_, loader) = stonecutter.current.project.split("-", limit = 2)
    properties.tags(stonecutter.current.version, loader)
    constants.match(loader, "fabric", "neoforge")
}

val minecraft: String = property("minecraft_version").toString()
val mcVersionProp: String = minecraft

version = "${property("mod.version")}+$minecraft"
group = property("mod.group").toString()

base {
    archivesName.set("${property("mod.id")}-neoforge")
}

repositories {
    mavenCentral()
    maven { url = uri("https://api.modrinth.com/maven") }
    maven { url = uri("https://maven.neoforged.net/releases/") }
    maven { url = uri("https://maven.architectury.dev/") }
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft")
    neoForge("net.neoforged:neoforge:${property("neoforge_version")}")
    compileOnly("maven.modrinth:architectury-api:${property("architectury_version")}+neoforge")
    localRuntime("maven.modrinth:architectury-api:${property("architectury_version")}+neoforge")
}

loom {
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

sourceSets {
    named("main") {
        java {
            setSrcDirs(listOf(rootProject.file("src-26/main/java")))
            exclude("**/fabric/**")
        }
        resources {
            setSrcDirs(listOf(rootProject.file("src-26/main/resources")))
            srcDir(rootProject.file("src/main/resources/assets"))
        }
    }
}

tasks.named("stonecutterPrepare") { enabled = false }
tasks.named("stonecutterGenerate") { enabled = false }

java {
    withSourcesJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
}

tasks.processResources {
    inputs.property("version", project.version)
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

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${project.base.archivesName.get()}" }
    }
}

val buildAndCollect = tasks.register<Copy>("buildAndCollect") {
    group = "versioned"
    from(tasks.jar.flatMap { it.archiveFile })
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
