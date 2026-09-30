package com.bettercontent.betterindustrialheat

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertFalse

class ReflectionBoundaryTest {
    private val sourceMarkers = listOf(
        "java.lang.reflect", "kotlin.reflect", "Class.forName(", ".getMethod(",
        ".getDeclaredMethod(", ".getField(", ".getDeclaredField(", ".getConstructor(",
        ".getDeclaredConstructor(", ".setAccessible(", ".trySetAccessible(",
        "Proxy.newProxyInstance(", "MethodHandles", "VarHandle", "sun.misc.Unsafe",
        "jdk.internal.misc.Unsafe",
    )
    private val binaryMarkers = listOf(
        "java/lang/reflect", "kotlin/reflect", "forName", "getMethod", "getDeclaredMethod",
        "getField", "getDeclaredField", "getConstructor", "getDeclaredConstructor",
        "setAccessible", "trySetAccessible", "newProxyInstance", "java/lang/invoke/VarHandle",
        "sun/misc/Unsafe", "jdk/internal/misc/Unsafe",
    )

    @Test
    fun `production source does not use reflection`() {
        Files.walk(Path.of("src")).use { files ->
            files.filter(Files::isRegularFile)
                .filter { it.toString().endsWith(".java") || it.toString().endsWith(".kt") }
                .filter { it.fileName.toString() != "ReflectionBoundaryTest.kt" }
                .forEach { file ->
                    val source = Files.readString(file)
                    sourceMarkers.forEach { marker -> assertFalse(source.contains(marker), "$file: $marker") }
                }
        }
    }

    @Test
    fun `compiled production classes do not reference reflection`() {
        Files.walk(Path.of("build/classes")).use { files ->
            files.filter(Files::isRegularFile)
                .filter { it.toString().endsWith(".class") && !it.toString().contains("/test/") }
                .forEach { file ->
                    val bytecode = Files.readAllBytes(file).toString(Charsets.ISO_8859_1)
                    binaryMarkers.forEach { marker -> assertFalse(bytecode.contains(marker), "$file: $marker") }
                }
        }
    }
}
