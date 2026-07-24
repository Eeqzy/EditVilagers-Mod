plugins {
    id("net.fabricmc.fabric-loom") version "1.17.2"
}

stonecutter {
    val (_, loader) = stonecutter.current.project.split("-", limit = 2)
    properties.tags(stonecutter.current.version, loader)
    constants.match(loader, "fabric", "neoforge")
}

val minecraft: String = stonecutter.current.version
val mcVersionProp: String = property("minecraft_version").toString()

version = "${property("mod.version")}+$minecraft"
group = property("mod.group").toString()

base {
    archivesName.set("${property("mod.id")}-fabric")
}

repositories {
    mavenCentral()
    maven { url = uri("https://api.modrinth.com/maven") }
    maven { url = uri("https://maven.architectury.dev/") }
    maven { url = uri("https://maven.fabricmc.net/") }
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft")
    implementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")
    implementation("maven.modrinth:architectury-api:${property("architectury_version")}+fabric")
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
            exclude("**/neoforge/**")
        }
        resources {
            setSrcDirs(listOf(rootProject.file("src-26/main/resources")))
            srcDir(rootProject.file("src/main/resources/assets"))
            srcDir(rootProject.file("src-26/client/resources"))
        }
        java {
            srcDir(rootProject.file("src-26/client/java"))
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
    options.release.set(21)
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
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
    into(rootProject.layout.buildDirectory.dir("libs/${property("mod.version")}/fabric"))
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
