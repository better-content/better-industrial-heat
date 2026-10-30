# Commit-time culinary age transfer (source-only)

`com.bettercontent.betterindustrialheat.api.CookingAgeApi` exposes Java-facing typed inputs:

```java
record Input(ItemStack stack, FoodThermalService.Storage storage)
static ItemStack transfer(List<Input> inputs, ItemStack output, long gameTime)
```

On actual successful transform publication, supply every consumed ingredient and its actual storage category. The API copies each input and settles that copy using the existing FoodThermalService policy and current game time; matching/preflight must not mutate inputs. It carries the complete oldest settled decay record to the output, including clocks, storage rate and thaw state. An already older output record also participates, preventing rejuvenation. The result is the same output object. Inputs are neither consumed nor mutated.

Age is **not averaged, divided by output yield or quality-scaled**. Fresh ingredients cannot hide an older ingredient. Actual per-ingredient storage is required so warm elapsed time is settled at its previously persisted rate before the new storage interval. No provider registration is needed: this is an explicit typed transform adapter owned by Heat, with no cooking dependency.

Existing single-input `carryCookingState`, drying transfer, FoodAgePolicy, profiles, storage rates, thaw/decay clocks, dried-food properties and recipes are unchanged. This API does not enable new preservation or drying mechanics.

## Nonedible paid-ingredient carriers

Prepared raw batches may intentionally be nonedible. They are **not** food and receive no new food profile or root age clock. Register their owned nested payload through `CookingAgeApi.registerCarrier(ResourceLocation owner, Carrier carrier)` during setup, before first resolution:

```java
interface Carrier {
    boolean handles(ItemStack stack);
    List<ItemStack> read(ItemStack stack);
    void write(ItemStack stack, List<ItemStack> children);
}
```

The adapter reads the actual paid ingredient stacks and writes only its own payload, preserving item identity/count and unrelated NBT. `read`/`handles` must be pure; `write` must reliably replace the supplied payload. It must not match ordinary edible items. Duplicate/late registrations, overlapping matches and edible matches fail closed.

`transfer` to a registered nonedible output writes settled copies of all paid inputs through the adapter. `transfer` from a carrier to an edible output flattens nested edible children and carries their oldest settled record, irrespective of yield. Heat inventory tracking recognizes carriers, and `settleCarrier(stack, storage, gameTime)` delegates settlement of child copies to the **same existing** FoodThermalService policy. It never converts rotten children: their real identity/decay records remain available for the final cooking transform. `rebaseCarrier(stack, gameTime)` applies existing carried-food logout/login clock rebasing recursively, so reconnect cannot charge offline time. The carrier root has no food state.

Traversal rejects identity cycles, depth greater than 32 (root depth zero) and more than 4096 nodes per operation. All children are processed on copies before the original root's payload is published; traversal failures leave the original untouched. Without a registered carrier, nonfood behavior remains unchanged.

`CookingAgeCarrierTest` adds seven direct Minecraft-bootstrap JVM controls for actual paid-input aging/final transfer, nested rotten identity, cycles, depth32/33, reconnect rebasing, registration failures and no-provider baseline. Direct real-mapped Java/Kotlin compilation and these seven controls pass. The earlier `3a4063fc` Heat JAR predates this extension and is retained as an intermediate artifact; native batch lifecycle acceptance remains root-owned.

The first carrier Gradle attempt compiled/staged but **failed** its 99-test run because the existing reflection gate also scans test source and rejected a test-only reflective registry-reset helper. That failed attempt and its `536b8886` candidate remain retained, not promoted. With explicit coordinator approval, registry fields became package-private and a same-package **test-source-only** helper now isolates registry state directly. No public reset, reflection workaround or gate weakening was added. A fresh bounded Heat `compileKotlin compileJava test stageRuntimeJar` passed in 28s: **99 tests, zero failures/errors/skips**. Final staged JAR SHA-256 is `393f42d5bae79ad4ab1362882bdf82d88efeb6410a5af084f6c1c521850246bc`; sorted source-manifest hash is `075efa1bb4baff2aeb32b16870ddeb46554fd44940aada1a8dec0b0b4d29cb3a`. Exact logs, source manifest and build report are under `/home/dev/.tmp/cooking-providers-20261028/carriers/final-build.json`. No native run or deployment occurred.

`CookingAgeApiTest` exercises two inputs whose ordering reverses after actual-storage settlement, exact oldest-record preservation, nonmutation, older outputs, equal-decay output clock retention and nonfood inputs. Four Minecraft-bootstrap JVM tests and direct real-mapped Kotlin/Java compilation passed in `/home/dev/.tmp/cooking-providers-20261028/`. These are not a native cooking transaction or pack acceptance. No runtime JAR was deployed.

Serialized Java17 Gradle `compileKotlin compileJava test stageRuntimeJar` subsequently passed in 3m25s (92 tests, 0 failures/errors/skips), with Kotlin compilation in-process and a bounded 2GiB/CPU2/two-worker Gradle JVM. Source manifests, build log and staged JAR hash are retained in `/home/dev/.tmp/cooking-providers-20261028/gradle/provider-builds.json`. This is source-build qualification only.
