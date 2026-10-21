# Better Industrial Heat

## Native source fixture safety

Each actual ForgeGradle GameTest RunConfig gets a fresh `build/gametest/<UUID>` working
location. `verifyFull` keeps its ordered 33-test Cold Sweat-present gate followed by the
33-test absent child invocation; each profile has its own UUID and verifies actual loaded
Cold Sweat state. Preparation creates only absent comment-only `server.properties`,
preserves existing settings, and copies authored structures. Both actual latest/debug logs
must prove the unchanged population, startup/shutdown and own structured status, with no
ERROR/FATAL or failed-operation WARN/stack records. Fast guards include negative controls.
Ordinary client/server directories and every previous fixture/build remain untouched; this
source mechanism does not itself claim runtime acceptance or authorize any producer.

## Playtest diagnostics

`bc.playtest.better_industrial_heat.coolant` reports completed fluid/heat conversions and
reason transitions for empty, unsupported, no-exchange and missing-target cases at DEBUG;
repeated identical deferrals stay silent. `food` reports actual settled stage/storage-rate/
frozen transitions and separately labels rotten result construction as caller-owned inventory
publication, not an observed inventory replacement. Food logging uses the already active
settlement state: it never initializes, scans or settles food just to observe it.

INFO startup includes effective enabled state, version, integration presence and exact
owner-artifact source metadata when available (UNKNOWN otherwise). All categories share
the owner-local Java17 helper's120 DEBUG/TRACE details/minute, burst20,256 keys and2048-char
limit; native END ticks summarize every30 seconds and server stop clears aggregation.
Standalone stays native INFO; the pack chooses scoped DEBUG/`bc.playtest.level`. No player
names, inventories or NBT are logged. Common six-case and coolant projection/privacy tests
run with the source gate; logs do not establish save durability or client presentation.

## Scope and authority

This repository owns its mod-specific behavior and authoring inputs. Read [local instructions](AGENTS.md)
and the [shared documentation/policy index](../../better-content-modpack/docs/README.md).


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
- Food spoilage runs on storage categories, not physical temperature. Each settled food stack records only its decay, a lazy timestamp, and the storage rate governing the pending interval. Ordinary food reaches harmful spoilage after 24,000 active ticks at ambient rates; cold storage (an adjacent thermal body at or below 5 °C) and `better_industrial_heat:dried_foods` or preserved entries age at 0.1×; frozen storage (ice, snow, or a thermal body at or below 0 °C) and shelf-stable foods do not age. A settle crosses at most one stage boundary and drops the remaining elapsed time, so a pause, an unloaded chunk, or a stale timestamp can never one-step food from fresh to ruined. Food carried by an offline player — inventory, ender chest, Curios slots, and carried bag item handlers — does not age at all. Frozen food stays uneatable until it has spent 1,200 ticks of settled non-frozen storage thawing. Block and machine inventories settle lazily when they change and never load chunks; a non-`Container` Forge machine that first writes tracked food activates this lazy reconciliation path, while untouched loot containers remain dormant.

Food freshness tooltips use capitalized, localized stage labels: Fresh (green), Stale
(yellow), Spoiled (gold), and Rotten (red). Frozen and shelf-stable notices remain separate;
this presentation does not change decay or preservation rates. At the Rotten threshold,
normal ticking converts food into **Rotten Meat** or **Rotten Produce** (their existing
`spoiled_meat` / `spoiled_produce` registry IDs are retained).

### Eating penalties

Penalties are guaranteed and are not cumulative stage packages:

| Food | Effects |
| --- | --- |
| Fresh | None |
| Stale | Thirst IV for 60 seconds |
| Spoiled | Thirst VIII and Malnourishment I for 60 seconds |
| Rotten, including converted meat/produce | Poison X for 5 seconds only |

Thirst directly drains `0.025 × level` hydration points per second, retaining fractional
losses; its 20-point pool empties in `800 / level` seconds, matching vanilla Hunger's
food-equivalent scale. Stale costs 6 hydration points; spoiled costs 12. Quenched reserve
is capped at remaining hydration. Malnourishment has its own scale: `level` percentage
points per second from **every** Diet nutrient group, clamped at zero. Spoiled food
therefore removes 60 percentage points from each group over its full duration. These
effects require their optional Thirst/Diet integrations to drain the corresponding pools.
Vanilla Hunger is unchanged; its exhaustion consumes saturation before food.

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
- Merge compatibility ignores only the root `better_industrial_heat_food` tag. Damage, capabilities, and all other NBT must still match, and partial transfers leave the source remainder unchanged. Merging never invents food state: two ordinary untracked stacks merge natively and stay untracked, and a merged stack keeps the later timestamp and the destination's storage rate.
- When Better Content Threads is present, an attempted use of observably frozen food begins one correlated episode; successfully consuming the same ordinary item identity after it is thawed and fresh completes that episode.
- Development dependencies are resolved from Forge, Create, Modrinth, Curse Maven, and Kotlin for Forge repositories declared in `build.gradle.kts`.

## Community and support

For modpack and mod discussion, playtest feedback, and bug reports, join the [Better Content Discord](https://discord.gg/EkRnZbzqS9).

## Identity

The clean-break canonical identity is repository/artifact `better-industrial-heat`, mod ID and resource namespace `better_industrial_heat`, and Maven group `com.bettercontent`. Legacy `heatsync` worlds and configs are not migrated.
