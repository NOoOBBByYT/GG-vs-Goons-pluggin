# README Improvements & Arclight Compatibility Plan

## Overview
This plan addresses improvements to the README.md file and ensures the GG vs Goons plugin is compatible with Arclight 1.21.x (Bukkit + Forge hybrid server) with combat/PvP enhancement mods.

## Issues Identified

### Current README Problems
1. **Typos and Grammar**
   - "self-contaned" → "self-contained" (line 5)
   - "nametaghs" → "nametags" (line 21)
   - "happnes" → "happens" (line 12)
   - "Bukkti" → "Bukkit" (line 21)

2. **Misleading Information**
   - README mentions CommandAPI is "shaded directly in" but build.gradle.kts has no CommandAPI dependency
   - Code uses standard Bukkit CommandExecutor, not CommandAPI

3. **Missing Critical Sections**
   - No Arclight/hybrid server compatibility information
   - No installation instructions
   - No configuration file documentation
   - No troubleshooting guide
   - No version compatibility matrix
   - No TODO/roadmap section in README itself

4. **Incomplete Documentation**
   - Planned modules lack detail
   - No architecture diagram
   - Event listener patterns not documented
   - Persistence strategies mentioned but not detailed

### Arclight Compatibility Concerns

#### High Priority
1. **Bukkit Event Compatibility**
   - Bukkit events may not fire for Forge mod combat actions
   - Need to document which events work reliably on Arclight
   - May need Forge event listeners for full mod integration

2. **GameMode Synchronization**
   - Bukkit GameMode changes should work on Arclight
   - Combat mods may override or conflict with Adventure mode restrictions
   - Need testing with specific PvP mods

3. **Chat Component Rendering**
   - BungeeCord chat API (used for clickable messages) should work
   - Some Forge chat mods may interfere with formatting
   - Need fallback for incompatible chat systems

#### Medium Priority
4. **Player Data Persistence**
   - Current implementation is memory-only (resets on restart)
   - Arclight has both Bukkit and Forge player data systems
   - Need to ensure prisoner state survives restarts

5. **Permission System Integration**
   - Bukkit permissions should work via Arclight
   - LuckPerms is Arclight-compatible
   - Need to document permission nodes

6. **Cross-Mod Interactions**
   - Combat mods may have custom damage events
   - PvP mods may have protection systems that conflict
   - Need hooks to detect and respect mod-based protections

#### Low Priority
7. **Performance on Hybrid Servers**
   - Arclight has slight overhead vs pure Paper
   - Document any performance considerations

## Proposed README Structure

```markdown
# GG-vs-Goons-plugin

## Overview
Brief description, key features

## Features
- Current modules with details
- Planned modules with roadmap

## Compatibility
- Server types (Paper, Arclight, etc.)
- Minecraft versions
- Required dependencies
- Compatible mods

## Installation
Step-by-step setup instructions

## Configuration
- plugin.yml settings
- Permission nodes
- Config file options (if added)

## Usage
- Commands with examples
- Permissions
- Gameplay mechanics

## For Developers
- Module system architecture
- Adding new modules
- Build instructions
- Testing workflow

## Arclight/Forge Compatibility
- Known issues
- Mod interaction notes
- Troubleshooting hybrid servers

## Roadmap/TODO
- Planned features
- Known limitations
- Future improvements

## Troubleshooting
Common issues and solutions

## Contributing
Guidelines for contributors

## License
```

## Architecture Diagram

```mermaid
graph TB
    A[GGvGPlugin] --> B[Module System]
    B --> C[WarPrisonerModule]
    B --> D[TeamsModule - Planned]
    B --> E[ScoreboardModule - Planned]
    B --> F[FactionsModule - Planned]
    
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

## Detailed Improvements

### 1. Arclight Compatibility Section

Add comprehensive section covering:
- What Arclight is (Bukkit API on Forge)
- Tested Arclight versions
- Known working combat/PvP mods
- Known incompatibilities
- Workarounds for common issues
- Event handling differences
- Performance notes

### 2. Installation Guide

```markdown
## Installation

### Prerequisites
- Arclight 1.21.x server (or Paper 1.21.x)
- Java 21 or higher
- (Optional) LuckPerms for permission management

### Steps
1. Download latest release from [Releases](link)
2. Place `GGvGoons-1.0.0.jar` in `plugins/` folder
3. Restart server
4. Configure permissions (see Configuration section)
5. Test with `/warprisoner` command

### Arclight-Specific Setup
- Ensure Arclight is fully loaded before testing
- Check console for any Bukkit API compatibility warnings
- Test with your combat mods to verify event handling
```

### 3. Configuration Documentation

Document all permission nodes:
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
```

### 4. Expanded Planned Modules

```markdown
## Planned Modules

### Teams Module (High Priority)
- Two main teams: GG and Goons
- Backed by Bukkit Scoreboard Teams
- Automatic nametag coloring
- Friendly fire prevention
- Team chat channels
- Team-based spawn points
- **Arclight Note**: Scoreboard teams work reliably on Arclight

### Scoreboard Module (High Priority)
- Live match statistics sidebar
- Prisoner counts per team
- Territory control status
- Player kill/death ratios
- **Arclight Note**: Bukkit scoreboards compatible with most Forge HUDs

### Factions/Territory Module (Medium Priority)
- Claimable territory system
- Resource generation in controlled areas
- Territory capture mechanics
- Integration with prisoner system (can't claim while imprisoned)
- **Arclight Note**: May conflict with Forge claiming mods - needs coordination

### Permissions Module (Low Priority)
- Rank-based command access
- Team leadership roles
- Custom permission groups
- **Arclight Note**: Use LuckPerms instead - fully Arclight compatible

### Combat Integration Module (Arclight-Specific)
- Hooks for Forge combat mod events
- Custom damage tracking
- Mod weapon compatibility
- PvP protection system integration
```

### 5. Troubleshooting Section

```markdown
## Troubleshooting

### Arclight-Specific Issues

**Problem**: Commands not registering
- **Cause**: Arclight loaded plugin before Bukkit API ready
- **Solution**: Add `loadbefore: [arclight]` to plugin.yml or restart server

**Problem**: Clickable chat messages not working
- **Cause**: Forge chat mod overriding Bukkit chat
- **Solution**: Disable chat formatting in conflicting mod or use `/tellraw` fallback

**Problem**: GameMode changes not applying
- **Cause**: Combat mod overriding gamemode
- **Solution**: Check mod configs for gamemode protection settings

**Problem**: Prisoner state lost on restart
- **Cause**: No persistence implemented yet
- **Solution**: Implement YAML/SQLite persistence (see Roadmap)

### General Issues

**Problem**: Player not found error
- **Solution**: Ensure player is online and name is spelled correctly

**Problem**: Permission denied
- **Solution**: Check LuckPerms configuration or op status
```

### 6. TODO/Roadmap Section

```markdown
## Roadmap & TODO

### Current Limitations
- [ ] No persistence - prisoner state resets on server restart
- [ ] No team validation - anyone can capture anyone
- [ ] No movement restrictions - prisoners can walk away
- [ ] No offer expiry - capture offers never timeout
- [ ] No permission system - all commands require op

### Planned Features

#### Phase 1: Core Stability
- [ ] Add YAML persistence for prisoner state
- [ ] Implement offer expiry (configurable timeout)
- [ ] Add permission nodes to all commands
- [ ] Create config.yml for customization

#### Phase 2: Teams System
- [ ] Implement Teams module with Scoreboard integration
- [ ] Add team validation to capture system
- [ ] Create team management commands
- [ ] Add team-based spawn points

#### Phase 3: Enhanced Prisoner System
- [ ] Add PlayerMoveEvent listener for movement restrictions
- [ ] Implement prisoner "jail" radius
- [ ] Add inventory restrictions for prisoners
- [ ] Create prisoner escape mechanics

#### Phase 4: Arclight Integration
- [ ] Add Forge event listeners for mod combat
- [ ] Create compatibility layer for popular PvP mods
- [ ] Implement mod weapon tracking
- [ ] Add Forge config integration

#### Phase 5: Advanced Features
- [ ] Scoreboard module with live stats
- [ ] Territory control system
- [ ] Prisoner trading/ransom system
- [ ] Match/round system with win conditions
```

### 7. Build Configuration Updates

Current `build.gradle.kts` is correct - no CommandAPI needed. However, for Arclight compatibility, consider adding:

```kotlin
repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.izzel.io/releases/") // Arclight repository (optional)
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    // Optional: Add Arclight API for enhanced compatibility
    // compileOnly("io.izzel.arclight:arclight-api:1.21-1.0.0")
}
```

### 8. Developer Documentation Enhancement

```markdown
## For Developers

### Module System Architecture

The plugin uses a modular architecture where each feature is self-contained:

1. **GGvGModule Interface**: All modules implement this interface
2. **Registration**: Modules register commands in `register()` method
3. **State Management**: Each module manages its own state
4. **Inter-Module Communication**: Modules expose public properties for cross-module access

### Adding a New Module

1. Create class implementing `GGvGModule`:
```kotlin
class MyModule(private val plugin: GGvGPlugin) : GGvGModule {
    val manager = MyManager(plugin)
    
    override fun register(plugin: GGvGPlugin) {
        plugin.getCommand("mycommand")?.setExecutor(MyCommandExecutor(this, plugin))
    }
}
```

2. Register in `GGvGPlugin.onEnable()`:
```kotlin
val myModule = MyModule(this)
modules += myModule
```

3. Add commands to `plugin.yml`

### Testing on Arclight

1. Set up Arclight 1.21.x test server
2. Install your combat/PvP mods
3. Build plugin: `./gradlew build`
4. Copy jar to `plugins/` folder
5. Test all commands with mod interactions
6. Check console for Bukkit API warnings
7. Verify events fire correctly with mods active

### Event Handling Best Practices

For Arclight compatibility:
- Always check if events are cancellable before cancelling
- Use `@EventHandler(priority = EventPriority.HIGH)` for important listeners
- Add null checks for player objects
- Log warnings when Forge events might be needed
```

## Implementation Priority

1. **High Priority** (Do First)
   - Fix all typos
   - Add Arclight compatibility section
   - Add installation instructions
   - Add TODO/roadmap to README
   - Remove CommandAPI references

2. **Medium Priority** (Do Soon)
   - Add troubleshooting section
   - Expand planned modules documentation
   - Add architecture diagram
   - Document event listeners
   - Add configuration section

3. **Low Priority** (Nice to Have)
   - Add contributing guidelines
   - Add license information
   - Add badges (build status, version, etc.)
   - Add screenshots/examples
   - Create wiki pages

## Testing Checklist

After README updates, verify:
- [ ] All typos corrected
- [ ] Arclight compatibility clearly documented
- [ ] Installation steps are accurate
- [ ] Build instructions work on Windows
- [ ] All links are valid
- [ ] Code examples are syntactically correct
- [ ] Mermaid diagram renders properly
- [ ] TODO section is actionable and clear

## Notes

- Keep README focused and concise - move detailed docs to wiki if it gets too long
- Update README whenever new modules are added
- Maintain version compatibility matrix as Minecraft updates
- Document any breaking changes prominently
- Consider creating separate ARCLIGHT.md for detailed hybrid server docs
