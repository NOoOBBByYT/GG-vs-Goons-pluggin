# GG-vs-Goons-plugin

A modular Bukkit/Paper plugin for the GG vs Goons server, featuring a war prisoner system and more planned in the future.

Built around a module system - each mechanic is a self-contained class, so adding new commands doesn't mean touching existing ones.

## Compatibility

- **Server Types**: Paper 1.21.x, Arclight 1.21.x (Bukkit + Forge hybrid)
- **Java Version**: Java 21 or higher
- **Tested With**: Arclight 1.21.x with combat/PvP enhancement mods

### Arclight/Forge Compatibility

This plugin is designed to work on **Arclight** servers (Bukkit API on Forge). Key compatibility notes:

- ✅ **Bukkit Commands**: Fully compatible
- ✅ **GameMode Changes**: Works reliably with most mods
- ✅ **Chat Components**: Clickable messages work (may have formatting differences with some chat mods)
- ✅ **Scoreboard Teams**: Compatible for future Teams module
- ⚠️ **Event Handling**: Bukkit events work, but Forge mod combat may not trigger all Bukkit events
- ⚠️ **Combat Mods**: Some PvP mods may override Adventure mode restrictions - test with your specific mods

**Recommended**: Test thoroughly with your specific combat/PvP mods to ensure prisoner restrictions work as expected.

## Features

### Current Modules

#### War Prisoner System
Capture defeated enemies as prisoners with a consent-based system.

**Commands:**
- `/warprisoner <player>` - Send a capture offer to a player (requires `ggvgoons.warprisoner.capture`)
- `/freeprisoner <player>` - Release your prisoner, restores their original gamemode (requires `ggvgoons.warprisoner.free`)
- `/executeprisoner <player>` - End imprisonment without restoring gamemode (requires `ggvgoons.warprisoner.execute`)
- `/listprisoners` - View all active prisoners and their captors (requires `ggvgoons.warprisoner.list`)

**How it works:**
1. Captor uses `/warprisoner <target>` to send a clickable capture offer
2. Target sees: `[Accept]` or `[Decline]` buttons in chat
3. **Accept** → Target becomes a prisoner, switched to Adventure mode
4. **Decline** → Nothing happens; it's on the captor to kill or free them
5. Offers automatically expire after 60 seconds (configurable in `config.yml`)
6. Captor can later `/freeprisoner` to release or `/executeprisoner` to end the arrangement
7. Prisoner state persists across server restarts

**Current Limitations:**
- No team validation (anyone can capture anyone - Teams module will fix this)
- No movement restrictions (prisoners can walk away - future event listeners will add boundaries)

### Planned Modules

#### Teams Module (High Priority)
- Two main teams: **GG** and **Goons**
- Backed by Bukkit Scoreboard Teams for automatic nametag coloring and friendly fire prevention
- Team validation for captures (can only capture opposing team members)
- Team chat channels
- Team-based spawn points
- **Arclight Note**: Scoreboard teams work reliably on Arclight

#### Scoreboard Module (High Priority)
- Live match statistics sidebar
- Prisoner counts per team
- Territory control status
- Player kill/death ratios
- **Arclight Note**: Bukkit scoreboards compatible with most Forge HUD mods

#### Territory/Factions Module (Medium Priority)
- Claimable territory system
- Resource generation in controlled areas
- Territory capture mechanics
- Integration with prisoner system (can't claim while imprisoned)
- **Arclight Note**: May conflict with Forge claiming mods like FTB Chunks - needs coordination

#### Combat Integration Module (Arclight-Specific)
- Hooks for Forge combat mod events
- Custom damage tracking from modded weapons
- PvP protection system integration
- Compatibility layer for popular combat mods

#### Permissions Module (Low Priority)
- Rank-based command access
- Team leadership roles
- **Note**: Consider using LuckPerms instead - fully Arclight compatible and more feature-rich

## Installation

### Prerequisites
- Arclight 1.21.x server (or Paper 1.21.x)
- Java 21 or higher
- (Optional) LuckPerms for permission management

### Steps
1. Download the latest release or build from source (see Build section)
2. Place `GGvGoons-1.0.0.jar` in your server's `plugins/` folder
3. Restart the server
4. (Optional) Configure permissions using LuckPerms or your permission plugin
5. Test with `/warprisoner <player>` command

### Arclight-Specific Setup
- Ensure Arclight is fully loaded before testing the plugin
- Check console for any Bukkit API compatibility warnings
- Test prisoner capture with your combat mods active to verify event handling
- If clickable chat messages don't work, check for conflicting Forge chat mods

## Configuration

### Permission Nodes
The plugin now includes a full permission system. All permissions default to OP status but can be managed via LuckPerms or any permission plugin:

```yaml
permissions:
  ggvgoons.warprisoner.capture:
    description: Allows capturing players as prisoners
    default: op
  ggvgoons.warprisoner.free:
    description: Allows freeing prisoners
    default: op
  ggvgoons.warprisoner.execute:
    description: Allows executing prisoners
    default: op
  ggvgoons.warprisoner.list:
    description: Allows listing all active prisoners
    default: op
  ggvgoons.admin:
    description: Full administrative access (grants all permissions)
    default: op
```

**Note:** Permission enforcement can be disabled in `config.yml` by setting `permissions.enabled: false` for OP-only mode.

### Plugin Configuration
The plugin generates a `config.yml` on first run with the following options:

```yaml
# GGvGoons Configuration

# Prisoner System Settings
prisoner:
  # How long capture offers remain valid (seconds)
  # Set to 0 to disable expiry
  offer-expiry-timeout: 60
  
  # Whether to persist prisoner state across restarts
  enable-persistence: true

# Permission Settings
permissions:
  # Whether to enforce permissions (false = OP-only mode)
  enabled: true
```

**Configuration Options:**
- `prisoner.offer-expiry-timeout`: Time in seconds before capture offers expire (default: 60, set to 0 to disable)
- `prisoner.enable-persistence`: Whether to save prisoner state to `prisoners.yml` on shutdown (default: true)
- `permissions.enabled`: Whether to enforce permission nodes or use OP-only mode (default: true)

## For Developers

### Module System Architecture

```
graph TB
    A[GGvGPlugin] --> B[Module System]
    B --> C[WarPrisonerModule]
    B --> D[TeamsModule - Planned]
    B --> E[ScoreboardModule - Planned]
    B --> F[TerritoryModule - Planned]
    
    C --> G[PrisonerManager]
    G --> H[Pending Offers Map]
    G --> I[Active Prisoners Map]
    
    C --> J[Command Executors]
    J --> K[/warprisoner]
    J --> L[/freeprisoner]
    J --> M[/executeprisoner]
    J --> N[/warprisoneraccept]
    J --> O[/warprisonerdecline]
    
    D --> P[TeamManager - Planned]
    P --> Q[Scoreboard Teams]
    P --> R[Team Validation]
    
    style C fill:#90EE90
    style D fill:#FFB6C1
    style E fill:#FFB6C1
    style F fill:#FFB6C1
```

### Adding a New Module

1. Create a new class under `src/main/kotlin/com/tyler/ggvsgoons/commands/` implementing [`GGvGModule`](src/main/kotlin/com/tyler/ggvsgoons/GGvGPlugin.kt:11):

```kotlin
class TerritoryModule(private val plugin: GGvGPlugin) : GGvGModule {
    val manager = TerritoryManager(plugin)
    
    override fun register(plugin: GGvGPlugin) {
        plugin.getCommand("claimterritory")?.setExecutor(ClaimTerritoryCommand(this, plugin))
        // Register other commands...
    }
}
```

2. In [`GGvGPlugin.onEnable()`](src/main/kotlin/com/tyler/ggvsgoons/GGvGPlugin.kt:22), instantiate it and add it to `modules`:

```kotlin
val territory = TerritoryModule(this)
modules += territory
```

3. Add your commands to [`plugin.yml`](src/main/resources/plugin.yml:1)

4. Build and drop the jar back in `plugins/`

**That's it!** No shared state to wire up unless two modules need to talk to each other (e.g., blocking territory claims while someone's a prisoner), in which case just expose what you need as a public property, the way [`WarPrisonerModule.manager`](src/main/kotlin/com/tyler/ggvsgoons/commands/WarPrisonerModule.kt:22) is exposed.

### Testing on Arclight

1. Set up an Arclight 1.21.x test server
2. Install your combat/PvP mods
3. Build the plugin: `./gradlew build`
4. Copy `build/libs/GGvGoons-1.0.0.jar` to `plugins/` folder
5. Start server and test all commands
6. Verify prisoner capture works with mod combat
7. Check console for Bukkit API warnings
8. Test that Adventure mode restrictions work with your mods

### Event Handling Best Practices

For Arclight compatibility:
- Always check if events are cancellable before cancelling
- Use `@EventHandler(priority = EventPriority.HIGH)` for important listeners
- Add null checks for player objects
- Log warnings when Forge events might be needed instead
- Test with actual Forge mods to verify events fire correctly

## Build

Requires JDK 21.

```bash
./gradlew build
```

On Windows: `gradlew.bat build`

Output jar: `build/libs/GGvGoons-1.0.0.jar` — drop it into your Paper or Arclight server's `plugins/` folder and restart.

## Roadmap & TODO

### Current Limitations
- [x] ~~**No persistence**~~ - ✅ **IMPLEMENTED** - prisoner state now persists across restarts via YAML
- [ ] **No team validation** - anyone can capture anyone
- [ ] **No movement restrictions** - prisoners can walk away in Adventure mode
- [x] ~~**No offer expiry**~~ - ✅ **IMPLEMENTED** - capture offers now expire after configurable timeout
- [x] ~~**No permission system**~~ - ✅ **IMPLEMENTED** - all commands now have permission nodes

### Planned Features

#### Phase 1: Core Stability ✅ **COMPLETE**
- [x] Add YAML persistence for prisoner state across restarts
- [x] Implement offer expiry with configurable timeout
- [x] Add permission nodes to all commands
- [x] Create `config.yml` for customization
- [x] Add `/listprisoners` command to see all active prisoners

#### Phase 2: Teams System
- [ ] Implement Teams module with Bukkit Scoreboard integration
- [ ] Add team validation to capture system (only capture opposing team)
- [ ] Create team management commands (`/team join`, `/team leave`, etc.)
- [ ] Add team-based spawn points
- [ ] Implement team chat channels

#### Phase 3: Enhanced Prisoner System
- [ ] Add `PlayerMoveEvent` listener for movement restrictions
- [ ] Implement configurable prisoner "jail" radius
- [ ] Add inventory restrictions for prisoners (prevent item use)
- [ ] Create prisoner escape mechanics (break free after X time or with help)
- [ ] Add prisoner transfer command (trade prisoners between captors)

#### Phase 4: Arclight Integration
- [ ] Add Forge event listeners for mod combat detection
- [ ] Create compatibility layer for popular PvP mods
- [ ] Implement modded weapon tracking in combat logs
- [ ] Add Forge config integration for cross-mod settings
- [ ] Test and document compatibility with specific combat mods

#### Phase 5: Advanced Features
- [ ] Scoreboard module with live match statistics
- [ ] Territory control system with capture points
- [ ] Prisoner ransom/trading system
- [ ] Match/round system with win conditions
- [ ] Leaderboards and statistics tracking

## Troubleshooting

### Arclight-Specific Issues

**Problem**: Commands not registering on startup  
**Cause**: Arclight loaded plugin before Bukkit API was ready  
**Solution**: Restart the server. If issue persists, check Arclight version compatibility

**Problem**: Clickable chat messages not working  
**Cause**: Forge chat mod overriding Bukkit chat system  
**Solution**: Disable chat formatting in the conflicting mod, or the plugin will need a `/tellraw` fallback

**Problem**: GameMode changes not applying to prisoners  
**Cause**: Combat mod overriding gamemode changes  
**Solution**: Check your combat mod's config for gamemode protection settings and disable them

**Problem**: Prisoner state lost on server restart
**Cause**: Persistence is disabled in config.yml
**Solution**: Enable persistence by setting `prisoner.enable-persistence: true` in `config.yml`

**Problem**: Prisoners can still break blocks in Adventure mode  
**Cause**: Some Forge mods may bypass Adventure mode restrictions  
**Solution**: Test with your specific mods. May need additional event listeners (planned for Phase 3)

### General Issues

**Problem**: "Player not found" error  
**Solution**: Ensure the target player is online and the name is spelled correctly (case-sensitive)

**Problem**: "Permission denied" when running commands  
**Solution**: Ensure you have OP status (`/op <yourname>`) or proper permissions via LuckPerms

**Problem**: Can't capture someone who's already a prisoner  
**Solution**: This is intentional - use `/freeprisoner` first, or have their captor release them

**Problem**: Accepted capture offer but nothing happened  
**Solution**: The captor may have logged off. Offers are cleared when captors disconnect

## Notes / Future Improvements

- ✅ ~~**Permissions**~~ - **IMPLEMENTED**: Full permission system with configurable enforcement and LuckPerms integration support
- **Team validation**: No check yet that captor and target are on opposing teams. Once the Teams module exists, add that validation before `createOffer`.
- ✅ ~~**Persistence**~~ - **IMPLEMENTED**: Prisoner state persists across restarts via YAML storage in `plugins/GGvGoons/prisoners.yml`
- **Movement/inventory limits for prisoners**: Adventure mode alone won't fully cage someone. Consider a `PlayerMoveEvent` listener to bound them to a radius, or `PlayerInteractEvent` for tighter control.
- ✅ ~~**Offer expiry**~~ - **IMPLEMENTED**: Capture offers automatically expire after configurable timeout (default 60 seconds)
- **Forge event integration**: For full Arclight compatibility with combat mods, may need to listen to Forge events in addition to Bukkit events.

## Contributing

Contributions are welcome! When adding new modules:
1. Follow the existing module pattern
2. Keep modules self-contained
3. Document any cross-module dependencies
4. Test on both Paper and Arclight if possible
5. Update this README with new features

## AI
AI was used to contribute this project. This is only made for a short and fun server and not a serious project.
