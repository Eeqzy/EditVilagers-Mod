plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "1.21.10-fabric"

// Loom fails when obfuscated (1.21.x + Yarn + useLegacyMixinAp) and unobfuscated
// (26.x Mojmap) subprojects share one Gradle invocation. Root aggregate tasks also
// configure extra Stonecutter nodes, so batch builds spawn separate Gradle processes.
private val obfuscatedVariants = listOf(
    "1.21.1-fabric",
    "1.21.10-fabric",
    "1.21.11-fabric",
    "1.21.1-neoforge",
    "1.21.10-neoforge",
    "1.21.11-neoforge",
)

private val unobfuscatedVariants = listOf(
    "26.1-fabric",
    "26.2-fabric",
    "26.1-neoforge",
)

private val gradlew = rootProject.file(if (System.getProperty("os.name").startsWith("Windows")) "gradlew.bat" else "gradlew")

fun Project.runIsolatedGradleBuild(variants: List<String>) {
    val args = buildList {
        add(gradlew.absolutePath)
        addAll(variants.map { ":$it:build" })
        add("--configure-on-demand")
    }
    logger.lifecycle("Running isolated build: ${args.drop(1).joinToString(" ")}")
    val process = ProcessBuilder(args)
        .directory(rootDir)
        .redirectErrorStream(true)
        .start()
    val output = process.inputStream.bufferedReader().readText()
    if (output.isNotBlank()) {
        logger.lifecycle(output)
    }
    if (process.waitFor() != 0) {
        throw GradleException("Isolated Gradle build failed (exit ${process.exitValue()})")
    }
}

tasks.register("chiseledBuild") {
    group = "project"
    description = "Build all loader variants in two isolated Gradle processes"
    doLast {
        runIsolatedGradleBuild(obfuscatedVariants)
        runIsolatedGradleBuild(unobfuscatedVariants)
    }
}

tasks.register("buildNeoForge") {
    group = "project"
    description = "Build every NeoForge JAR in two isolated Gradle processes"
    doLast {
        runIsolatedGradleBuild(obfuscatedVariants.filter { it.endsWith("-neoforge") })
        runIsolatedGradleBuild(unobfuscatedVariants.filter { it.endsWith("-neoforge") })
    }
}

tasks.register("chiseledJar") {
    group = "project"
    dependsOn("chiseledBuild")
}

