# NeoForge 1.21.1 Migration Guide - Sophisticated Backpacks & Etched Integration

This guide summarizes the changes made to port the mod from older versions to NeoForge 1.21.1, Sophisticated Core 1.21.1, and Etched 5.0.0.

## 1. Networking Overhaul (NeoForge 1.21.1)
NeoForge has moved from `SimpleChannel` to a `CustomPacketPayload` system. 

- **Sending Packets:** Use `PacketDistributor` static methods instead of `.send()`.
    - **Near a block:** `PacketDistributor.sendToPlayersNear(serverLevel, null, x, y, z, radius, payload)`
    - **Tracking an entity:** `PacketDistributor.sendToPlayersTrackingEntity(entity, payload)`
- **Payloads:** Instead of old `Message` classes, use `Payload` records (e.g., `PlayDiscPayload`, `StopDiscPlaybackPayload`).

## 2. Minecraft 1.21 Data Components
NBT access via `stack.getTag()` or `stack.getTagElement()` is replaced by **Data Components**.

- **Custom Data (Legacy NBT):**
    ```java
    CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
    if (customData != null) {
        CompoundTag tag = customData.copyTag();
        // Access tags here
    }
    ```
- **Jukebox Playable:** To get the song from a disc:
    ```java
    Optional<Holder<JukeboxSong>> song = JukeboxSong.fromStack(level.registryAccess(), stack);
    ```

## 3. Sophisticated Core API (1.21.1)
- **IDiscHandler:** Now generic. Implementation should use `IDiscHandler<Holder<JukeboxSong>>`.
- **Method Signatures:** Updated to use `Holder<JukeboxSong>` instead of `Item` or `ItemStack` for song identification in `ServerStorageSoundHandler`.

## 4. Etched API (5.0.0)
- **Item Identification:** Use `stack.is(EtchedItems.ETCHED_MUSIC_DISC.get())`.
- **Component Identification:** Use `stack.has(EtchedComponents.MUSIC.get())` to verify if an item is an Etched music disc.
- **TrackData:** Now an immutable record. Mixins to `save/load` are no longer applicable; logic should move to component access or item logic.

## 5. Gradle & ShadowJar (Split Package Fixes)
NeoForge 1.21.1 uses the Java Module System, which crashes if two JARs contain the same package (e.g., `org.apache.commons.io`).

- **Relocation (Crucial):** You MUST relocate any shaded library packages to a unique sub-package of your mod to avoid module conflicts.
- **Aggressive Exclusions:** You must exclude all Minecraft, NeoForge, and Mojang packages.
- **Metadata Preservation:** Ensure `META-INF/neoforge.mods.toml` from your own resources is kept, but excluded from dependencies.
- **JPMS Cleanup:** Exclude `module-info.class` from shaded libraries.

```gradle
shadowJar {
    configurations = [project.configurations.runtimeClasspath]
    dependencies {
        include(dependency('net.jthink:jaudiotagger:3.0.1'))
        include(dependency('commons-io:commons-io:2.15.1'))
    }

    relocate 'org.apache.commons.io', 'com.absolutebuddies.sophisticatedbackpacksetchedintegration.shadow.commonsio'
    relocate 'net.jthink.jaudiotagger', 'com.absolutebuddies.sophisticatedbackpacksetchedintegration.shadow.jaudiotagger'

    exclude 'net/minecraft/**'
    exclude 'net/neoforged/**'
    exclude 'com/mojang/**'
    exclude 'module-info.class'
    // ... other excludes
    exclude { it.path == 'META-INF/neoforge.mods.toml' && !it.file.path.contains('resources') }
}
```

## 6. Mixin Updates
- Ensure Mixin targets match the new 1.21.1 obfuscation/signatures.
- Access protected fields like `player.connection` via methods like `player.connection.send(packet)`.
