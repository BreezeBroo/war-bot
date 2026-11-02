# Warbot Practice

A Fabric mod for PvP practice in singleplayer, LAN worlds, and dedicated test
servers. Bots are standalone fake `ServerPlayer` entities with randomized
16-character offline-profile names and use Minecraft's default Steve/Alex skins.
Vanilla clients render these default skins without installing the mod. On LAN
and dedicated servers, bots are labeled `[PRACTICE]`. Names are generated
locally and are not verified against Mojang's account registry.

## Requirements

- Minecraft Java Edition 26.2
- Fabric Loader 0.20.1 or newer
- Fabric API 0.161.0+26.2
- Java 25

## Build

Run `gradle build`. The mod jar is written to `build/libs/`.

## Use

Install the jar on a Fabric client or server. An operator can run:

```text
/warbot spawn <number>
```

The number must be between 1 and 8. Bots spawn near the command issuer, then
approach and attack nearby players using normal player movement and combat. They
are practice NPCs with basic pursuit, not human-level tactical opponents.
Remove them with:

```text
/kill @a[tag=warbot_practice]
```