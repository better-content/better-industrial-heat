# Better Industrial Heat

Better Industrial Heat is a Minecraft `1.20.1` Forge mod built with Kotlin. It owns native heat pipes, thermal machinery, liquid coolant conversion, and the optional Cold Sweat ambient bridge.

## Behavior

- Registers a Cold Sweat `BlockTemp` handler when Cold Sweat is installed.
- Maps pipe heat values onto Cold Sweat world temperature with a configurable offset/scale model.
- Rebalances loaded heat pipes every second using Cold Sweat ambient temperature, neighboring pipe heat, and nearby tagged cold sources.
- Uses the configured neutral heat baseline as ambient when Cold Sweat is absent; native heat transport remains available.
- Ships default tags under `data/better_industrial_heat/tags/blocks` for radiator and cold-source classification.
- Provides native heat pipes, coolant exchangers, thermal fireboxes, boiler heaters, and creative heat sources without a Create: New Age dependency.
- Requires active Create heat: passive heater blocks do not power boilers, and bulk blasting uses a fueled Blaze Burner instead of lava.
- Allows otherwise-identical food stacks with different thermal/spoilage state to merge across vanilla inventories, item entities, hoppers, and standard Forge item handlers. Temperature and decay are weighted by the destination count plus the number actually moved; the destination item identity and every non-thermal tag remain unchanged.
- Food age advances only while simulation time advances: ordinary tracked food reaches harmful spoilage after 24,000 active ticks, refrigerated or `better_industrial_heat:dried_foods` catalogue entries advance at 0.1×, and frozen or explicitly shelf-stable food does not age. Unloaded containers settle their elapsed age on their stored prior category when next reconciled, then persist the selected target category for the next lazy interval; this is a bounded event-driven approximation and never loads chunks to tick food. A non-`Container` Forge machine that first writes tracked food activates this lazy reconciliation path, while untouched loot containers remain dormant.

## Configuration

The common config defines:

- Heat-to-temperature mapping bounds and scale
- Ambient blending and pipe equalization rates
- Cold-source target heat values
- Pipe-emitted Cold Sweat range and maximum effect

## Commands

```bash
./gradlew runClient
./gradlew runServer
./gradlew runData
./gradlew verifyFast
./gradlew verifyFull
```

## Dependencies

Required at runtime:

- Minecraft Forge `47.4.13` (`1.20.1`)
- Kotlin for Forge `4.11.0`
- Create `6.0.8`
- ChemLib `2.0.19`

Optional integrations:

- Applied Energistics 2 and PneumaticCraft: Repressurized
- Cold Sweat `2.3.13+` (enables ambient/world temperature bridge; verification is pinned to the pack's `2.4` runtime)
- EMI `1.1.3+1.20.1+forge` (client-side compatibility)

## Release

```bash
./gradlew verifyFull
```

Build outputs:

- `build/libs/better-industrial-heat-<version>.jar`
- `build/libs/better-industrial-heat-<version>-sources.jar`

Coverage outputs:

- `build/reports/jacoco/test/html/index.html`

## Notes

- Cold Sweat is optional. `verifyFull` runs the headless GameTests once with Cold Sweat `2.4` and once without Cold Sweat.
- Merge compatibility ignores only the root `better_industrial_heat_food` tag. Damage, capabilities, and all other NBT must still match, and partial transfers leave the source remainder unchanged.
- When Better Content Threads is present, an attempted use of observably frozen food begins one correlated episode; successfully consuming the same ordinary item identity after it is thawed and fresh completes that episode.
- Development dependencies are resolved from Forge, Create, Modrinth, Curse Maven, and Kotlin for Forge repositories declared in `build.gradle.kts`.

## Community and support

For modpack and mod discussion, playtest feedback, and bug reports, join the [Better Content Discord](https://discord.gg/EkRnZbzqS9).

## Identity

The clean-break canonical identity is repository/artifact `better-industrial-heat`, mod ID and resource namespace `better_industrial_heat`, and Maven group `com.bettercontent`. Legacy `heatsync` worlds and configs are not migrated.
