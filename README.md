# GG-vs-Goons-plugin

Bukkit/Paper Plugin for GG vs Goons server

Built around a module system - each mechanic is a self-contaned class, so adding new commands doesn't mean touching existing ones

## Current Modules

`/warprisoner <player>` sends a clickable capture offer in chat.

- Accept → flagged as a prisoner, switched to adventure mode.
- Decline → nothing happnes; it's on the captors to kill or free them.

Commands:

- `/warprisoner <player>` - Sends a capture offer
- `/freeprisoner <player>` - Releases your prisoner, restores normal survival gamemode.

## Planned Modules

- Teams - The two top-level sides, GG and Goons. Likely backed by Bukkti Scoreboard Teams so nametaghs and friendly fire rules come for free. with a TeamManager other modules can query to validate captor/target are on opposing sides.
- Scoreboard - a sidebar showing live match state
- Factions
- Permissions



## Adding a new module

1. Create a new class under `src/main/kotlin/com/tyler/ggvsgoons/commands/`
  implementing `GGvGModule`:
  ```kotlin
       class TerritoryModule(plugin: GGvGPlugin) : GGvGModule {
       override fun register(plugin: GGvGPlugin) {
           // CommandAPICommand("claimterritory")...
       }
   }
  ```
2. In `GGvGPlugin.onEnable()`, instantiate it and add it to `modules`.
3. Build and drop the jar back in `plugins/`.

That's it — no shared state to wire up unless two modules need to talk to
each other (e.g. blocking territory claims while someone's a prisoner), in
which case just expose what you need as a public val, the way `WarPrisonerModule.manager` is exposed.

## Build

Requires JDK 21.

```bash
./gradlew build
```

On Windows: `gradlew.bat build`

Output jar: `build/libs/GGvGoons-1.0.0.jar` — drop it into your Paper server's `plugins/` folder and restart. CommandAPI is shaded directly in, nothing else to install.

## Requirements

- Paper 1.21.x server
- Java 21



## Notes / things worth adding

- **Permissions**: anyone can currently run any command. Add nodes
(`ggvgoons.warprisoner.capture`, etc.) to `plugin.yml` and check them in
each module's handlers if you want to restrict by rank — or lean on
LuckPerms for this and keep GGvGoons focused on game logic.
- **Server/team validation**: no check yet that captor and target are on
opposing servers. Once the Teams module exists, add that check before
`createOffer`.
- **Persistence**: all module state is in-memory, resets on restart. If
matches span restarts, persist to YAML/SQLite in `onDisable`/`onEnable`.
- **Movement/inventory limits for prisoners**: Adventure mode alone won't
cage someone. Consider a `PlayerMoveEvent` listener to bound them to a
radius, or `PlayerInteractEvent` for tighter control.
- **Offer expiry**: capture offers never time out. Add a `BukkitRunnable`
calling `manager.clearOffer(targetId)` after N seconds if you don't want
them open indefinitely.

