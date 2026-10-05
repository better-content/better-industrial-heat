import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import groovy.json.JsonSlurper
import org.gradle.api.tasks.Sync
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.util.zip.ZipFile
import javax.imageio.ImageIO

plugins {
    idea
    eclipse
    `maven-publish`
    jacoco
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("net.minecraftforge.gradle") version "6.0.54"
    id("org.spongepowered.mixin") version "0.7.38"
}

val minecraftVersion = property("minecraft_version") as String
val forgeVersion = property("forge_version") as String
val kotlinForForgeVersion = property("kotlinforforge_version") as String
val createReleaseVersion = property("create_release_version") as String
val createMavenVersion = property("create_maven_version") as String
val ponderVersion = property("ponder_version") as String
val flywheelVersion = property("flywheel_version") as String
val registrateVersion = property("registrate_version") as String
val chemlibVersion = property("chemlib_version") as String
val chemlibCurseFileId = property("chemlib_curse_file_id") as String
val coldSweatVersion = property("cold_sweat_version") as String
val coldSweatModrinthVersionId = property("cold_sweat_modrinth_version_id") as String
val coldSweatRuntimeVersion = property("cold_sweat_runtime_version") as String
val coldSweatRuntimeCurseFileId = property("cold_sweat_runtime_curse_file_id") as String
val includeColdSweatRuntime = providers.gradleProperty("include_cold_sweat_runtime")
    .map { it.toBoolean() }
    .orElse(true)
val gameTestRunDirectory = layout.buildDirectory.dir(
    includeColdSweatRuntime.map { includeColdSweat ->
        if (includeColdSweat) "run-gametest-with-cold-sweat" else "run-gametest-without-cold-sweat"
    }
)
val emiVersion = property("emi_version") as String
val emiCurseFileId = property("emi_curse_file_id") as String
val powerGridVersion = property("powergrid_version") as String
val pneumaticCraftCurseFileId = property("pneumaticcraft_curse_file_id") as String
val ae2CurseFileId = property("ae2_curse_file_id") as String
val hexereiCurseFileId = property("hexerei_curse_file_id") as String
val curiosCurseFileId = property("curios_curse_file_id") as String
val modId = property("mod_id") as String
val modName = property("mod_name") as String
val modVersion = property("mod_version") as String
val modAuthors = property("mod_authors") as String
val modDescription = property("mod_description") as String
val modLicense = property("mod_license") as String
group = property("mod_group") as String
version = modVersion

base {
    archivesName.set("better-industrial-heat")
}

fun deobf(notation: String): Any =
    requireNotNull(extensions.getByName("fg").withGroovyBuilder { "deobf"(notation) })

fun transformHotFluidTexture(source: BufferedImage): BufferedImage {
    val output = BufferedImage(source.width, source.height, BufferedImage.TYPE_INT_ARGB)
    for (x in 0 until source.width) {
        for (y in 0 until source.height) {
            val argb = source.getRGB(x, y)
            val alpha = argb ushr 24 and 0xFF
            if (alpha == 0) {
                output.setRGB(x, y, 0)
                continue
            }

            val red = argb ushr 16 and 0xFF
            val green = argb ushr 8 and 0xFF
            val blue = argb and 0xFF
            val hsb = Color.RGBtoHSB(red, green, blue, null)
            val shiftedHue = (hsb[0] + 0.025f) % 1.0f
            val boostedSaturation = (hsb[1] * 1.06f).coerceIn(0f, 1f)
            val boostedBrightness = (hsb[2] * 1.18f + 0.02f).coerceIn(0f, 1f)
            val hotRgb = Color.HSBtoRGB(shiftedHue, boostedSaturation, boostedBrightness)
            output.setRGB(x, y, (alpha shl 24) or (hotRgb and 0x00FF_FFFF))
        }
    }
    return output
}

data class BuiltinCoolantAssetDefinition(
    val coldFluid: String,
    val hotFluidPath: String,
)

fun builtinCoolantAssetDefinitions(coolantDir: File, modId: String): List<BuiltinCoolantAssetDefinition> {
    if (!coolantDir.isDirectory) {
        return emptyList()
    }

    return coolantDir.listFiles { file -> file.extension == "json" }
        .orEmpty()
        .sortedBy { it.name }
        .map { file ->
            val json = JsonSlurper().parse(file) as Map<*, *>
            val hotFluid = json["hot_fluid"] as String
            val separatorIndex = hotFluid.indexOf(':')
            require(separatorIndex in 1 until hotFluid.lastIndex) { "Bundled hot fluid $hotFluid must be namespaced" }
            val namespace = hotFluid.substring(0, separatorIndex)
            val hotFluidPath = hotFluid.substring(separatorIndex + 1)
            require(namespace == modId) { "Bundled hot fluid $hotFluid must use namespace $modId" }
            BuiltinCoolantAssetDefinition(json["cold_fluid"] as String, hotFluidPath)
        }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    withSourcesJar()
}

kotlin {
    jvmToolchain(17)
}

minecraft {
    mappings("official", minecraftVersion)
    copyIdeResources = true

    runs {
        configureEach {
            workingDirectory(project.file("run"))
            property("forge.logging.markers", "REGISTRIES")
            property("forge.logging.console.level", "info")
            property("forge.enabledGameTestNamespaces", modId)
            property("mixin.env.remapRefMap", "true")
            property("mixin.env.refMapRemappingFile", file("build/createSrgToMcp/output.srg").absolutePath)

            mods {
                create(modId) {
                    source(sourceSets.main.get())
                }
            }
        }

        create("client")

        create("server") {
            arg("--nogui")
        }

        create("gameTestServer") {
            workingDirectory(gameTestRunDirectory.get().asFile)
        }

        create("data") {
            args(
                "--mod", modId,
                "--all",
                "--output", file("src/generated/resources").absolutePath,
                "--existing", file("src/main/resources").absolutePath
            )
        }
    }
}

sourceSets.main {
    resources.srcDir("src/generated/resources")
}

val generatedFluidTextureDir = layout.projectDirectory.dir("src/generated/resources/assets/$modId/textures/block")
val bundledCoolantDir = layout.projectDirectory.dir("src/main/resources/data/$modId/liquid_coolants")
val generatedBundledCoolantDir = layout.projectDirectory.dir("src/generated/resources/data/$modId/liquid_coolants")
val bundledCoolantIndexFile = generatedBundledCoolantDir.file("_index.txt")
val clientJar = gradle.gradleUserHomeDir.resolve("caches/forge_gradle/minecraft_repo/versions/$minecraftVersion/client.jar")

val generateHotFluidTextures by tasks.registering {
    inputs.file(clientJar)
    inputs.dir(bundledCoolantDir)
    outputs.dir(generatedFluidTextureDir)

    doLast {
        val outputDir = generatedFluidTextureDir.asFile
        outputDir.deleteRecursively()
        outputDir.mkdirs()
        val definitions = builtinCoolantAssetDefinitions(bundledCoolantDir.asFile, modId)

        ZipFile(clientJar).use { zip ->
            fun readVanillaTexture(name: String): BufferedImage {
                val entry = requireNotNull(zip.getEntry(name)) { "Missing client asset $name in $clientJar" }
                return zip.getInputStream(entry).use(ImageIO::read)
            }

            fun copyMeta(inputPath: String, outputName: String) {
                val entry = requireNotNull(zip.getEntry(inputPath)) { "Missing client asset $inputPath in $clientJar" }
                outputDir.resolve(outputName).writeBytes(zip.getInputStream(entry).use { it.readBytes() })
            }

            definitions.forEach { definition ->
                val (stillSource, flowSource) = when (definition.coldFluid) {
                    "minecraft:water" -> "assets/minecraft/textures/block/water_still.png" to
                        "assets/minecraft/textures/block/water_flow.png"
                    else -> error("Hot-fluid texture generation needs a source-texture rule for ${definition.coldFluid}.")
                }

                val still = transformHotFluidTexture(readVanillaTexture(stillSource))
                val flow = transformHotFluidTexture(readVanillaTexture(flowSource))
                ImageIO.write(still, "png", outputDir.resolve("${definition.hotFluidPath}_still.png"))
                ImageIO.write(flow, "png", outputDir.resolve("${definition.hotFluidPath}_flow.png"))
                copyMeta("${stillSource}.mcmeta", "${definition.hotFluidPath}_still.png.mcmeta")
                copyMeta("${flowSource}.mcmeta", "${definition.hotFluidPath}_flow.png.mcmeta")
            }
        }
    }
}

val generateBundledCoolantIndex by tasks.registering {
    inputs.dir(bundledCoolantDir)
    outputs.file(bundledCoolantIndexFile)

    doLast {
        val outputFile = bundledCoolantIndexFile.asFile
        outputFile.parentFile.mkdirs()
        val entries = fileTree(bundledCoolantDir) {
            include("**/*.json")
        }.files
            .map { file ->
                val relativePath = bundledCoolantDir.asFile.toPath().relativize(file.toPath()).toString()
                    .replace(File.separatorChar, '/')
                "data/$modId/liquid_coolants/$relativePath"
            }
            .sorted()

        outputFile.writeText(entries.joinToString(separator = System.lineSeparator(), postfix = System.lineSeparator()))
    }
}

repositories {
    mavenCentral()
    maven("https://maven.minecraftforge.net")
    maven("https://thedarkcolour.github.io/KotlinForForge/")
    maven("https://maven.createmod.net")
    maven("https://maven.ithundxr.dev/mirror")
    maven("https://api.modrinth.com/maven") {
        content {
            includeGroup("maven.modrinth")
        }
    }
    maven("https://www.cursemaven.com") {
        content {
            includeGroup("curse.maven")
        }
    }
}

dependencies {
    minecraft("net.minecraftforge:forge:$minecraftVersion-$forgeVersion")

    implementation("thedarkcolour:kotlinforforge:$kotlinForForgeVersion")

    implementation(deobf("com.simibubi.create:create-$minecraftVersion:$createMavenVersion:slim"))
    implementation(deobf("net.createmod.ponder:Ponder-Forge-$minecraftVersion:$ponderVersion"))
    implementation(deobf("io.github.llamalad7:mixinextras-forge:0.3.6"))
    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")
    compileOnly(deobf("dev.engine-room.flywheel:flywheel-forge-api-$minecraftVersion:$flywheelVersion"))
    runtimeOnly(deobf("dev.engine-room.flywheel:flywheel-forge-$minecraftVersion:$flywheelVersion"))
    implementation(deobf("com.tterrag.registrate:Registrate:$registrateVersion"))

    implementation(deobf("curse.maven:chemlib-340666:$chemlibCurseFileId"))
    compileOnly(deobf("maven.modrinth:cold-sweat:$coldSweatModrinthVersionId"))
    if (includeColdSweatRuntime.get()) {
        runtimeOnly(deobf("curse.maven:cold-sweat-506194:$coldSweatRuntimeCurseFileId"))
    }
    compileOnly(deobf("curse.maven:emi-580555:$emiCurseFileId"))
    runtimeOnly(deobf("curse.maven:emi-580555:$emiCurseFileId"))
    compileOnly(deobf("maven.modrinth:power-grid:$powerGridVersion"))
    compileOnly(deobf("curse.maven:pneumaticcraft-repressurized-281849:$pneumaticCraftCurseFileId"))
    runtimeOnly(deobf("curse.maven:pneumaticcraft-repressurized-281849:$pneumaticCraftCurseFileId"))
    compileOnly(deobf("curse.maven:applied-energistics-2-223794:$ae2CurseFileId"))
    compileOnly(deobf("curse.maven:thirst-was-taken-679270:6660408"))
    compileOnly(deobf("curse.maven:diet-443570:4813904"))
    compileOnly(deobf("curse.maven:hexerei-548599:$hexereiCurseFileId"))
    // Optional: carried-food walk covers Curios slots when the pack runs Curios.
    compileOnly(deobf("curse.maven:curios-api-309927:$curiosCurseFileId"))

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation(deobf("curse.maven:pneumaticcraft-repressurized-281849:$pneumaticCraftCurseFileId"))
}

tasks.processResources {
    val props = mapOf(
        "minecraftVersion" to minecraftVersion,
        "forgeVersion" to forgeVersion,
        "kotlinForForgeVersion" to kotlinForForgeVersion,
        "createReleaseVersion" to createReleaseVersion,
        "chemlibVersion" to chemlibVersion,
        "coldSweatVersion" to coldSweatVersion,
        "emiVersion" to emiVersion,
        "powerGridVersion" to powerGridVersion,
        "modId" to modId,
        "modName" to modName,
        "modVersion" to modVersion,
        "modAuthors" to modAuthors,
        "modDescription" to modDescription,
        "modLicense" to modLicense
    )

    inputs.properties(props)
    filesMatching(listOf("META-INF/mods.toml", "pack.mcmeta")) {
        expand(props)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "17"
}

mixin {
    add(sourceSets.main.get(), "better_industrial_heat.refmap.json")
    config("better_industrial_heat.mixins.json")
}

tasks.named("processResources") {
    dependsOn(generateHotFluidTextures, generateBundledCoolantIndex)
}

tasks.named("sourcesJar") {
    dependsOn(generateHotFluidTextures, generateBundledCoolantIndex)
}

tasks.named<Jar>("jar") {
    finalizedBy("reobfJar")
}

val stageRuntimeJar by tasks.registering(Copy::class) {
    group = "build"
    description = "Stages the reobfuscated runtime jar into build/libs using the canonical release filename."
    dependsOn(tasks.named("reobfJar"))
    from(layout.buildDirectory.file("reobfJar/output.jar"))
    into(layout.buildDirectory.dir("libs"))
    rename { "${base.archivesName.get()}-$version.jar" }
}

tasks.named("assemble") {
    dependsOn(stageRuntimeJar)
}

val syncGameTestStructures by tasks.registering(Sync::class) {
    from(layout.projectDirectory.dir("gameteststructures"))
    into(gameTestRunDirectory.map { it.dir("gameteststructures") })
}

tasks.matching { it.name == "prepareRunGameTestServer" }.configureEach {
    dependsOn(syncGameTestStructures)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.register("headlessGameTest") {
    group = "verification"
    description = "Runs Forge game tests with Cold Sweat $coldSweatRuntimeVersion in a headless dedicated server."
    dependsOn(tasks.named("runGameTestServer"))
}

val headlessGameTestNoColdSweat by tasks.registering(Exec::class) {
    group = "verification"
    description = "Runs Forge game tests without the optional Cold Sweat runtime."
    workingDir(project.projectDir)
    commandLine(
        project.layout.projectDirectory.file("gradlew").asFile.absolutePath,
        "runGameTestServer",
        "-Pinclude_cold_sweat_runtime=false",
        "--no-daemon",
    )
}

headlessGameTestNoColdSweat.configure {
    mustRunAfter(tasks.named("headlessGameTest"))
}

jacoco {
    toolVersion = "0.8.12"
}

val coverageClassPatterns = listOf(
    "com/bettercontent/betterindustrialheat/HeatMappingMath.class",
    "com/bettercontent/betterindustrialheat/PipeThermalStepMath.class",
    "com/bettercontent/betterindustrialheat/compat/latent/RadiogenicHeatDistribution.class",
    "com/bettercontent/betterindustrialheat/compat/powergrid/PowerGridHeatMappingMath.class",
    "com/bettercontent/betterindustrialheat/content/coolant/CoolantExchangeLogic.class",
    "com/bettercontent/betterindustrialheat/content/energy/EnergyLadderMath.class",
    "com/bettercontent/betterindustrialheat/content/heat/BoilerHeaterLogic.class"
)

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    classDirectories.setFrom(
        fileTree(layout.buildDirectory.dir("classes/kotlin/main")) {
            coverageClassPatterns.forEach(::include)
        }
    )
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.test)
    violationRules {
        rule {
            element = "CLASS"
            includes = listOf(
                "com.bettercontent.betterindustrialheat.HeatMappingMath",
                "com.bettercontent.betterindustrialheat.PipeThermalStepMath",
                "com.bettercontent.betterindustrialheat.compat.latent.RadiogenicHeatDistribution",
                "com.bettercontent.betterindustrialheat.compat.powergrid.PowerGridHeatMappingMath",
                "com.bettercontent.betterindustrialheat.content.coolant.CoolantExchangeLogic",
                "com.bettercontent.betterindustrialheat.content.energy.EnergyLadderMath",
                "com.bettercontent.betterindustrialheat.content.heat.BoilerHeaterLogic"
            )
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.95".toBigDecimal()
            }
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = "0.90".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestReport, tasks.jacocoTestCoverageVerification)
}

tasks.register("verifyFast") {
    group = "verification"
    description = "Runs the fast deterministic verification lane."
    dependsOn(tasks.named("check"))
}

tasks.register("verifyFull") {
    group = "verification"
    description = "Runs fast checks and Forge GameTests with and without Cold Sweat."
    dependsOn(tasks.named("verifyFast"))
    dependsOn(tasks.named("headlessGameTest"))
    dependsOn(headlessGameTestNoColdSweat)
}

// Mixin's reobfuscation mappings are produced by the Java annotation processor.
// A partial compile can discard that file without revisiting the mixin source.
tasks.withType<JavaCompile>().configureEach { options.isIncremental = false }
