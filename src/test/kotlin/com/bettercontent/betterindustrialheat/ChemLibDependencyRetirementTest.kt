package com.bettercontent.betterindustrialheat

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChemLibDependencyRetirementTest {
    @Test
    fun `native heat integration remains without mandatory ChemLib dependency`() {
        val build = Files.readString(Path.of("build.gradle.kts"))
        val metadata = Files.readString(Path.of("src/main/resources/META-INF/mods.toml"))
        val properties = Files.readString(Path.of("gradle.properties"))
        assertFalse(build.contains("chemlib"), "no ChemLib Gradle artifact")
        assertFalse(metadata.contains("chemlib"), "no required ChemLib mod ID")
        assertFalse(properties.contains("chemlib"), "no obsolete dependency pin")
        assertTrue(build.contains("pneumaticcraft"), "keep native pressure/heat integration")
        assertTrue(build.contains("create"), "keep native boiler integration")
    }
}
