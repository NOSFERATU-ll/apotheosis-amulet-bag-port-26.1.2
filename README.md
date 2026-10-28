# Apotheosis Amulet Bag — Minecraft 26.1.2

NeoForge port of [Apotheosis Amulet Bag for 1.21.1](https://github.com/NOSFERATU-ll/apotheosis-amulet-bag), by NOSFERATU.

[Русская инструкция](README_RU.md)

## Requirements

- Minecraft **26.1.2** and Java **25**.
- NeoForge **26.1.2.76 or newer for 26.1.2**.
- Apotheosis **9.1.x** and its required dependencies (including Placebo, Apothic Attributes and Patchouli).
- Optional: Curios **15.x** for 26.1.2; development checks use **15.0.0+26.1.2**.

Only Minecraft 26.1.2 is declared supported. Dependencies are installed separately; they are not embedded in the jar.

## Usage

- Right-click the bag to open its five slots.
- Sneak + right-click to unlock the next slot for **100 / 200 / 300 / 400 / 500 experience levels**. A new bag has zero unlocked slots. Creative players unlock slots for free.
- Place Apotheosis potion charms in unlocked slots. Stored charms are enabled automatically and consume durability through Apotheosis' own tick implementation.
- Carry the bag in the main inventory or offhand, or equip it in a Curios charm slot. Only the first nonempty eligible bag is active, in that order.
- The owning bag cannot be picked up, thrown, or swapped through its open menu.

```mcfunction
/give @p apoth_talisman_bag:talisman_bag
```

Install the mod on both client and server. The item ID and existing custom-data keys are retained, but migration of existing Minecraft worlds from 1.21.1 has not been tested.

## Build

Install JDK 25, then run:

```sh
./gradlew build
```

On Windows use `gradlew.bat`. The distributable is `build/libs/apoth_talisman_bag-26.1.2-1.0.0.jar`; the `-sources.jar` is for developers. The included wrapper uses Gradle 9.2.1.

## Verification

The separate development-only smoke-test mod starts a real dedicated server, creates a fake player, and checks XP costs, locked slots, rejection of unrelated items, full ItemStack serialization, all five slots, removal, actual potion effects and durability, durability persistence while the menu is open, and Curios equipment.

```sh
./gradlew runSmokeServer
./gradlew runSmokeServer -PwithoutCurios
```

Successful runs write `build/smoke-curios.txt` and `build/smoke-no-curios.txt`, starting with `PASS:`. Check these files as well as the Gradle exit status: Minecraft can exit successfully after a server-thread exception. The test mod is excluded from normal client/server runs and release jars. GitHub Actions checks both configurations.

Client rendering, real-player clicks, and multiplayer synchronization still require an in-game check. See [TESTING.md](TESTING.md).
