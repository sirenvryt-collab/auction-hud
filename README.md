# Donut Auction HUD

A **fully client-side** Fabric mod for Minecraft **1.21.11** that adds an auction HUD and item
browser built specifically for **DonutSMP**. It does nothing at all on any other server or in
singleplayer.

## Features

- **Live HUD panel** (top-right corner) showing:
  - A countdown timer until the current auction ends (colour-coded: green → yellow → red as
    time runs out, with an "anti-snipe" extension if a new top bid comes in near the end).
  - The current highest bidder and their bid amount, formatted DonutSMP-style (`$47M`, `$1.5B`, etc).
- **Auction item browser** (press `B`):
  - Scrollable, mouse-wheel-driven list of auction items with real item icons.
  - Live search/filter bar — type to instantly filter the list by name.
  - Hover highlighting, click-to-select, and a scrollbar when the list overflows.
- **DonutSMP chat detection**: parses messages in the exact form `Name paid you $ Amount`
  (e.g. `Steve paid you $ 15000` or `Steve paid you $ 47M`) and uses them to update the
  highest bidder in real time.
- **Server-gated**: every feature checks that you're actually connected to a `donutsmp.net`
  address before doing anything, so the mod is invisible everywhere else.
- Toggle the HUD on/off with `H`.

> **Note on the timer/data:** DonutSMP doesn't currently expose a public real-time auction
> feed, so the countdown uses sensible placeholder logic (a 5-minute timer that resets/extends
> based on parsed bids) and the item browser ships with an example item list. Both are designed
> to be trivial to swap for real data later — see `AuctionState.java` for the single place to
> plug that in.

## Project structure

```
donut-auction-hud/
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradle/wrapper/gradle-wrapper.properties
├── src/main/resources/
│   ├── fabric.mod.json
│   └── assets/donutauctionhud/lang/en_us.json
└── src/main/java/com/donutauction/
    ├── DonutAuctionHudClient.java   # entrypoint: keybinds, HUD + chat listener registration
    ├── AuctionState.java            # timer, highest bid, example item list (singleton)
    ├── AuctionItem.java             # record: display name, icon item, starting bid
    ├── ServerDetector.java          # detects donutsmp.net connections
    ├── ChatBidListener.java         # regex-parses "Name paid you $ Amount"
    ├── hud/AuctionHudRenderer.java  # draws the countdown/bidder panel
    ├── gui/AuctionBrowserScreen.java# scrollable searchable item browser screen
    └── util/MoneyFormat.java        # "47M"/"1.5B" ⇄ double conversion
```

## Requirements

- **JDK 21** (Fabric 1.21.x targets Java 21 — make sure `JAVA_HOME` points at a JDK 21 install).
- **Gradle** (only needed once, to generate the wrapper — see below) or an IDE with built-in
  Gradle support (IntelliJ IDEA works out of the box).

## Opening the project

1. Download/clone this project folder.
2. Open it in **IntelliJ IDEA** (recommended — has the best Fabric/Loom support) via
   *File → Open* and select the `donut-auction-hud` folder. IDEA will detect the Gradle
   project and import it automatically, generating the wrapper for you.
   - Alternatively, any editor works as long as you can run Gradle from the command line
     (see below).

## Generating the Gradle wrapper (first time only)

This project intentionally ships without a pre-built `gradlew`/`gradle-wrapper.jar` binary. If
you're not using an IDE that generates it for you, create it once with a locally installed
Gradle (8.x or newer):

```bash
gradle wrapper --gradle-version 8.14
```

That reads `gradle/wrapper/gradle-wrapper.properties` (already included) and produces
`gradlew`, `gradlew.bat`, and `gradle/wrapper/gradle-wrapper.jar`. After that, you can always
use `./gradlew` (or `gradlew.bat` on Windows) exactly as normal Fabric mods do.

## Building the jar

From the project root, once the wrapper exists:

```bash
./gradlew build
```

On Windows:

```powershell
.\gradlew.bat build
```

The first run downloads Minecraft, mappings, Fabric Loader and Fabric API — this can take a
few minutes. When it finishes, the built mod jar is at:

```
build/libs/donut-auction-hud-1.0.0.jar
```

(`build/libs/donut-auction-hud-1.0.0-sources.jar` is also produced — that one is just the
source jar and is **not** what you install.)

### If the build fails to resolve dependencies

Minecraft/Yarn/Loader/Fabric API versions for very recent Minecraft releases update
frequently. If Gradle can't resolve `minecraft_version`, `yarn_mappings`, `loader_version` or
`fabric_version` in `gradle.properties`, check the current values for **1.21.11** at
<https://fabricmc.net/develop> and update those four lines — nothing else in the build needs
to change.

## Installing the mod

1. Install the **Fabric Loader** for Minecraft 1.21.11 using the
   [Fabric installer](https://fabricmc.net/use/installer/) if you haven't already.
2. Download **Fabric API** for 1.21.11 from
   [Modrinth](https://modrinth.com/mod/fabric-api) or
   [CurseForge](https://www.curseforge.com/minecraft/mc-mods/fabric-api) and place the jar in
   your `.minecraft/mods` folder — this mod depends on it.
3. Copy `donut-auction-hud-1.0.0.jar` (from `build/libs/`) into the same `.minecraft/mods`
   folder.
4. Launch Minecraft with the **Fabric** profile.

## Using it in-game

- Join a `donutsmp.net` server. The mod stays completely inactive everywhere else.
- The auction HUD panel appears automatically in the top-right corner.
- Press **B** to open the searchable item browser. Press **Esc** to close it.
- Press **H** to show/hide the HUD panel.
- Whenever chat shows a message like `SomePlayer paid you $ 2.4M`, the HUD's "Top Bidder" and
  "Bid" fields update instantly if that amount beats the current highest bid.

## Extending it

- **Real auction data**: replace the placeholder logic in `AuctionState.resetTimer()` /
  `registerBid()` with whatever DonutSMP actually exposes (scoreboard, boss bar, plugin
  message, etc.) once you've identified the real signal.
- **Real item list**: replace `AuctionState.seedExampleItems()` with a live-fetched or
  config-driven list.
- **Config screen**: the mod currently has no config file; HUD position/keybinds are the
  easiest next additions if you want them configurable in-game.
