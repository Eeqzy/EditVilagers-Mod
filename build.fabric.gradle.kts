plugins {
    id("fabric-loom") version "1.17.2"
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
    archivesName.set("${property("mod.id")}-fabric")
}

repositories {
    mavenCentral()
    maven { url = uri("https://api.modrinth.com/maven") }
    maven { url = uri("https://maven.architectury.dev/") }
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft")
    mappings("net.fabricmc:yarn:${property("yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")
    modImplementation("maven.modrinth:architectury-api:${property("architectury_version")}+fabric")
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
                    rootProject.file("src/client/java"),
                ),
            )
            exclude("**/neoforge/**")
        }
        resources {
            setSrcDirs(
                listOf(
                    layout.buildDirectory.dir("generated/stonecutter/main/resources"),
                    rootProject.file("src/client/resources"),
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
    from(accessWidenerSource()) {
        into(".")
        rename { "editvillager.accesswidener" }
    }
    filesMatching("editvillager.mixins.json") {
        filter { line ->
            if (line.contains("__REFMAP__")) {
                "\"refmap\": \"editvillager.refmap.json\","
            } else {
                line
            }
        }
    }
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
    from(tasks.remapJar.flatMap { it.archiveFile }, tasks.remapSourcesJar.flatMap { it.archiveFile })
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
