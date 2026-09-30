package com.bettercontent.betterindustrialheat.content.coolant

import com.bettercontent.betterindustrialheat.HeatSyncMod
import com.google.gson.JsonParser
import net.minecraft.resources.ResourceLocation
import java.io.File
import java.nio.file.Files
import kotlin.io.path.pathString

object BundledCoolantDefinitions {
    private const val DIRECTORY = "data/${HeatSyncMod.MOD_ID}/liquid_coolants"
    private const val INDEX = "$DIRECTORY/_index.txt"

    val definitions: List<LiquidCoolantDefinition> by lazy(::loadDefinitions)

    private fun loadDefinitions(): List<LiquidCoolantDefinition> {
        val definitions = resourcePaths().map(::loadDefinition)

        return definitions
            .onEach { definition ->
                require(definition.hotFluid.namespace == HeatSyncMod.MOD_ID) {
                    "Bundled hot fluid ${definition.hotFluid} must be in namespace ${HeatSyncMod.MOD_ID}"
                }
            }
            .sortedBy { it.id.toString() }
    }

    private fun resourcePaths(): List<String> {
        loadIndexedResourcePaths()?.let { return it }
        return discoverResourcePaths()
    }

    private fun loadIndexedResourcePaths(): List<String>? {
        val indexStream = javaClass.classLoader.getResourceAsStream(INDEX) ?: return null
        return indexStream.bufferedReader().useLines { lines ->
            lines
                .map(String::trim)
                .filter(String::isNotEmpty)
                .toList()
        }
    }

    private fun discoverResourcePaths(): List<String> {
        val directoryUrl = requireNotNull(javaClass.classLoader.getResource(DIRECTORY)) {
            "Missing bundled coolant directory $DIRECTORY"
        }

        return when (directoryUrl.protocol) {
            "file" -> {
                val root = File(directoryUrl.toURI()).toPath()
                Files.walk(root).use { paths ->
                    paths
                        .filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".json") }
                        .map { path ->
                            val relative = root.relativize(path).pathString.replace(File.separatorChar, '/')
                            "$DIRECTORY/$relative"
                        }
                        .toList()
                }
            }

            "jar" -> {
                val marker = "!/$DIRECTORY/"
                val separatorIndex = directoryUrl.path.indexOf(marker)
                require(separatorIndex >= 0) {
                    "Unable to resolve bundled coolant jar path from $directoryUrl"
                }

                val jarUrl = java.net.URL(directoryUrl.path.substring(0, separatorIndex))
                java.util.jar.JarFile(File(jarUrl.toURI())).use { jar ->
                    jar.entries()
                        .asSequence()
                        .filter { !it.isDirectory && it.name.startsWith("$DIRECTORY/") && it.name.endsWith(".json") }
                        .map { it.name }
                        .toList()
                }
            }

            else -> error(
                "Unsupported resource protocol for bundled coolants: ${directoryUrl.protocol}. " +
                    "Expected generated index $INDEX to be present.",
            )
        }
    }

    private fun loadDefinition(resourcePath: String): LiquidCoolantDefinition {
        require(resourcePath.startsWith("$DIRECTORY/") && resourcePath.endsWith(".json")) {
            "Invalid bundled coolant resource path $resourcePath"
        }

        val relative = resourcePath.removePrefix("$DIRECTORY/").removeSuffix(".json")
        val id = ResourceLocation.fromNamespaceAndPath(HeatSyncMod.MOD_ID, relative)
        val stream = requireNotNull(javaClass.classLoader.getResourceAsStream(resourcePath)) {
            "Missing bundled coolant resource $resourcePath"
        }

        return stream.use {
            LiquidCoolantDefinition.fromJson(id, JsonParser.parseReader(it.reader()).asJsonObject)
        }
    }
}
