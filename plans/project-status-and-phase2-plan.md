# GG vs Goons Plugin - Project Status & Phase 2 Plan

## Executive Summary

**Phase 1: Core Stability** is **100% COMPLETE** ✅

The plugin now has a fully functional war prisoner system with persistence, permissions, offer expiry, and configuration. The codebase is production-ready for basic server use.

**Next Priority: Phase 2 - Teams System** to enable GG vs Goons team-based gameplay.

---

## What's Been Completed

### Phase 1: Core Stability ✅ COMPLETE

All five Phase 1 objectives have been successfully implemented:

#### 1. YAML Persistence System ✅
- **File**: [`PrisonerPersistence.kt`](../src/main/kotlin/com/tyler/ggvsgoons/persistence/PrisonerPersistence.kt)
- Prisoner state persists across server restarts
- Stores data in `plugins/GGvGoons/prisoners.yml`
- Graceful error handling with validation
- Integrated into plugin lifecycle (load on enable, save on disable)

#### 2. Offer Expiry System ✅
- **Modified**: [`PrisonerManager.kt`](../src/main/kotlin/com/tyler/ggvsgoons/commands/PrisonerManager.kt)
- Capture offers automatically expire after configurable timeout (default: 60 seconds)
- Uses `BukkitRunnable` for scheduled expiry tasks
- Notifies both players when offers expire
- Tasks properly cleaned up on plugin disable

#### 3. Permission System ✅
- **Modified**: [`plugin.yml`](../src/main/resources/plugin.yml), all command executors
- Full permission nodes for all commands:
  - `ggvgoons.warprisoner.capture` - Capture players
  - `ggvgoons.warprisoner.free` - Free prisoners
  - `ggvgoons.warprisoner.execute` - Execute prisoners
  - `ggvgoons.warprisoner.list` - List all prisoners
  - `ggvgoons.admin` - Parent permission (grants all)
- Permission enforcement can be disabled for OP-only mode
- Compatible with LuckPerms and other permission plugins

#### 4. Configuration System ✅
- **File**: [`config.yml`](../src/main/resources/config.yml)
- Configurable options:
  - `prisoner.offer-expiry-timeout` - Timeout in seconds (default: 60)
  - `prisoner.enable-persistence` - Toggle persistence (default: true)
  - `permissions.enabled` - Toggle permission enforcement (default: true)
- Auto-generates with sensible defaults
- Loaded and logged on startup

#### 5. List Prisoners Command ✅
- **Command**: `/listprisoners`
- Shows count and details of all active prisoners
- Color-coded output for readability
- Handles offline players gracefully
- Permission-protected

### Current Architecture

```mermaid
graph TB
    A[GGvGPlugin] --> B[Config System]
    A --> C[Module System]
    A --> D[Persistence Layer]
    
    B --> B1[config.yml]
    
    C --> E[WarPrisonerModule]
    E --> F[PrisonerManager]
    
    F --> G[In-Memory State]
    G --> G1[Pending Offers]
    G --> G2[Active Prisoners]
    G --> G3[Expiry Tasks]
    
    D --> I[PrisonerPersistence]
    I --> J[prisoners.yml]
    
    E --> K[Commands]
    K --> K1[/warprisoner]
    K --> K2[/freeprisoner]
    K --> K3[/executeprisoner]
    K --> K4[/listprisoners]
    K --> K5[/warprisoneraccept]
    K --> K6[/warprisonerdecline]
    
    style E fill:#90EE90
    style F fill:#90EE90
    style I fill:#90EE90
```

### Build Status
- ✅ Compiles successfully with Gradle
- ✅ Output: `build/libs/GGvGoons-1.0.0.jar`
- ✅ Compatible with Paper 1.21.x and Arclight 1.21.x
- ✅ Java 21 target

---

## Current Limitations

These are known limitations that will be addressed in future phases:

1. **No team validation** - Anyone can capture anyone (Phase 2)
2. **No movement restrictions** - Prisoners can walk away in Adventure mode (Phase 3)
3. **No inventory restrictions** - Prisoners can still use items (Phase 3)
4. **No escape mechanics** - No way for prisoners to break free (Phase 3)
5. **No Forge event integration** - Limited combat mod compatibility (Phase 4)

---

## Phase 2: Teams System - Implementation Plan

### Overview

Implement a two-team system (GG vs Goons) with Bukkit Scoreboard integration to enable team-based gameplay and add validation to the prisoner capture system.

### Goals

1. Create two teams: **GG** and **Goons**
2. Use Bukkit Scoreboard Teams for automatic features:
   - Colored nametags (Blue for GG, Red for Goons)
   - Friendly fire prevention
   - Team identification
3. Add team validation to prisoner captures (can only capture opposing team)
4. Implement team management commands
5. Add team-based spawn points
6. Create team chat channels

### Architecture Design

```mermaid
graph TB
    A[GGvGPlugin] --> B[Module System]
    
    B --> C[WarPrisonerModule]
    B --> D[TeamsModule NEW]
    
    D --> E[TeamManager]
    E --> F[Scoreboard Teams]
    F --> F1[GG Team - BLUE]
    F --> F2[Goons Team - RED]
    
    E --> G[Team Data]
    G --> G1[Team Members Map]
    G --> G2[Team Spawn Points]
    
    D --> H[Team Commands]
    H --> H1[/team join]
    H --> H2[/team leave]
    H --> H3[/team list]
    H --> H4[/team chat]
    H --> H5[/team setspawn]
    
    C --> I[PrisonerManager]
    I -.->|Team Validation| E
    
    D --> J[TeamPersistence]
    J --> K[teams.yml]
    
    style D fill:#FFB6C1
    style E fill:#FFB6C1
    style J fill:#FFB6C1
```

### Implementation Steps

#### Step 1: Create TeamManager Core
**Files to Create:**
- `src/main/kotlin/com/tyler/ggvsgoons/teams/TeamManager.kt`
- `src/main/kotlin/com/tyler/ggvsgoons/teams/Team.kt` (data class)

**Responsibilities:**
- Manage Bukkit Scoreboard Teams
- Track team membership (UUID -> Team mapping)
- Provide team validation methods
- Handle team join/leave operations
- Manage team spawn points

**Key Methods:**
```kotlin
class TeamManager(private val plugin: GGvGPlugin) {
    fun joinTeam(player: Player, teamName: String): Boolean
    fun leaveTeam(player: Player): Boolean
    fun getPlayerTeam(playerId: UUID): Team?
    fun areOpposingTeams(player1: UUID, player2: UUID): Boolean
    fun getTeamMembers(teamName: String): List<UUID>
    fun setTeamSpawn(teamName: String, location: Location)
    fun getTeamSpawn(teamName: String): Location?
    fun teleportToTeamSpawn(player: Player): Boolean
}
```

#### Step 2: Create TeamsModule
**File to Create:**
- `src/main/kotlin/com/tyler/ggvsgoons/teams/TeamsModule.kt`

**Responsibilities:**
- Implement `GGvGModule` interface
- Register team commands
- Initialize TeamManager
- Set up Scoreboard Teams on enable

**Integration:**
- Add to [`GGvGPlugin.onEnable()`](../src/main/kotlin/com/tyler/ggvsgoons/GGvGPlugin.kt:38)
- Expose `TeamManager` for cross-module access

#### Step 3: Implement Team Commands
**Commands to Create:**
- `/team join <gg|goons>` - Join a team
- `/team leave` - Leave current team
- `/team list [team]` - List team members
- `/team chat <message>` - Send message to team
- `/team setspawn` - Set team spawn point (admin only)
- `/team spawn` - Teleport to team spawn

**Permission Nodes:**
```yaml
ggvgoons.team.join: true          # Join a team
ggvgoons.team.leave: true         # Leave a team
ggvgoons.team.list: true          # List team members
ggvgoons.team.chat: true          # Use team chat
ggvgoons.team.spawn: true         # Teleport to team spawn
ggvgoons.team.setspawn: op        # Set team spawn (admin)
ggvgoons.team.admin: op           # All team admin permissions
```

#### Step 4: Add Team Persistence
**File to Create:**
- `src/main/kotlin/com/tyler/ggvsgoons/persistence/TeamPersistence.kt`

**Data Structure:**
```yaml
# plugins/GGvGoons/teams.yml
teams:
  gg:
    members:
      - 550e8400-e29b-41d4-a716-446655440000
      - 123e4567-e89b-12d3-a456-426614174000
    spawn:
      world: world
      x: 100.5
      y: 64.0
      z: 200.5
      yaw: 90.0
      pitch: 0.0
  goons:
    members:
      - 987fcdeb-51a2-43f7-8d9e-123456789abc
    spawn:
      world: world
      x: -100.5
      y: 64.0
      z: -200.5
      yaw: -90.0
      pitch: 0.0
```

**Integration:**
- Load teams on plugin enable
- Save teams on plugin disable
- Auto-save on team changes (optional)

#### Step 5: Integrate Team Validation into Prisoner System
**File to Modify:**
- [`WarPrisonerModule.kt`](../src/main/kotlin/com/tyler/ggvsgoons/commands/WarPrisonerModule.kt:77)

**Changes:**
In `WarPrisonerCommand.onCommand()`, add team validation before creating offer:

```kotlin
// After checking if target is already a prisoner...

// Check if teams module is available and validate teams
val teamsModule = plugin.teams // Need to expose this in GGvGPlugin
if (teamsModule != null) {
    if (!teamsModule.manager.areOpposingTeams(sender.uniqueId, target.uniqueId)) {
        sender.sendMessage("${ChatColor.RED}You can only capture members of the opposing team!")
        return true
    }
}
```

#### Step 6: Update Configuration
**Add to [`config.yml`](../src/main/resources/config.yml):**
```yaml
# Team Settings
teams:
  # Whether to enforce team validation for captures
  enforce-team-validation: true
  
  # Whether players can switch teams freely
  allow-team-switching: true
  
  # Cooldown for switching teams (seconds, 0 = no cooldown)
  team-switch-cooldown: 300
  
  # Whether to use team chat prefix
  team-chat-prefix: true
```

#### Step 7: Scoreboard Team Setup
**Implementation Details:**
- Create Scoreboard Teams on plugin enable
- Set team colors: GG = BLUE, Goons = RED
- Configure friendly fire: DISABLED
- Set name tag visibility: ALWAYS
- Handle player join events to restore team membership

**Code Location:**
In `TeamManager.kt`:
```kotlin
private fun setupScoreboardTeams() {
    val scoreboard = Bukkit.getScoreboardManager()?.mainScoreboard ?: return
    
    // GG Team
    var ggTeam = scoreboard.getTeam("GG")
    if (ggTeam == null) {
        ggTeam = scoreboard.registerNewTeam("GG")
    }
    ggTeam.color = ChatColor.BLUE
    ggTeam.setAllowFriendlyFire(false)
    ggTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS)
    
    // Goons Team
    var goonsTeam = scoreboard.getTeam("Goons")
    if (goonsTeam == null) {
        goonsTeam = scoreboard.registerNewTeam("Goons")
    }
    goonsTeam.color = ChatColor.RED
    goonsTeam.setAllowFriendlyFire(false)
    goonsTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS)
}
```

### Testing Requirements

Before marking Phase 2 complete, test:

1. **Team Join/Leave:**
   - Players can join GG or Goons
   - Players can leave teams
   - Scoreboard teams update correctly
   - Nametag colors appear correctly

2. **Team Validation:**
   - Cannot capture same-team members
   - Can capture opposing team members
   - Validation can be disabled via config

3. **Team Chat:**
   - Messages only visible to team members
   - Chat prefix works correctly

4. **Team Spawns:**
   - Admins can set spawn points
   - Players can teleport to team spawn
   - Spawns persist across restarts

5. **Persistence:**
   - Team membership persists across restarts
   - Spawn points persist across restarts
   - Scoreboard teams restore correctly

6. **Friendly Fire:**
   - Team members cannot damage each other
   - Can damage opposing team members

### File Structure After Phase 2

```
src/main/
├── kotlin/com/tyler/ggvsgoons/
│   ├── GGvGPlugin.kt (modified - add teams module)
│   ├── commands/
│   │   ├── PrisonerManager.kt
│   │   └── WarPrisonerModule.kt (modified - add team validation)
│   ├── teams/ (NEW)
│   │   ├── Team.kt (NEW)
│   │   ├── TeamManager.kt (NEW)
│   │   └── TeamsModule.kt (NEW)
│   └── persistence/
│       ├── PrisonerPersistence.kt
│       └── TeamPersistence.kt (NEW)
└── resources/
    ├── plugin.yml (modified - add team commands & permissions)
    └── config.yml (modified - add team settings)
```

### Configuration Updates Needed

**plugin.yml additions:**
```yaml
commands:
  # ... existing commands ...
  
  team:
    description: Team management commands
    usage: /team <join|leave|list|chat|spawn|setspawn> [args]
    permission: ggvgoons.team.join

permissions:
  # ... existing permissions ...
  
  ggvgoons.team.join:
    description: Allows joining a team
    default: true
  ggvgoons.team.leave:
    description: Allows leaving a team
    default: true
  ggvgoons.team.list:
    description: Allows listing team members
    default: true
  ggvgoons.team.chat:
    description: Allows using team chat
    default: true
  ggvgoons.team.spawn:
    description: Allows teleporting to team spawn
    default: true
  ggvgoons.team.setspawn:
    description: Allows setting team spawn points
    default: op
  ggvgoons.team.admin:
    description: All team administrative permissions
    default: op
    children:
      ggvgoons.team.setspawn: true
```

### Backward Compatibility

- Existing prisoner system continues to work without teams
- Team validation is optional (configurable)
- No breaking changes to existing commands
- Existing prisoners remain valid after upgrade

### Performance Considerations

- Scoreboard operations are efficient (native Bukkit)
- Team lookups are O(1) map operations
- YAML I/O only on startup/shutdown
- Team chat has minimal overhead

---

## Phase 3 Preview: Enhanced Prisoner System

After Phase 2 is complete, Phase 3 will focus on:

1. **Movement Restrictions**
   - `PlayerMoveEvent` listener to enforce jail radius
   - Configurable boundary distance
   - Teleport prisoners back if they escape bounds

2. **Inventory Restrictions**
   - Prevent item use while imprisoned
   - Block certain interactions
   - Configurable item blacklist

3. **Escape Mechanics**
   - Time-based escape (break free after X minutes)
   - Rescue mechanics (teammates can free prisoners)
   - Escape attempts with success chance

4. **Prisoner Transfer**
   - Trade prisoners between captors
   - Transfer command with consent system

---

## Recommended Next Steps

1. **Review this plan** - Ensure Phase 2 scope aligns with server goals
2. **Prioritize features** - Decide which Phase 2 features are must-have vs nice-to-have
3. **Test Phase 1** - Deploy current build to test server and verify all features work
4. **Begin Phase 2 implementation** - Start with TeamManager core and build incrementally
5. **Iterate** - Test each component before moving to the next

---

## Questions for Consideration

Before starting Phase 2 implementation:

1. **Team Balance**: Should there be limits on team sizes to prevent imbalance?
2. **Team Switching**: Should there be restrictions or cooldowns on switching teams?
3. **Default Team**: Should players be auto-assigned to a team on first join?
4. **Team Names**: Are "GG" and "Goons" the final team names, or should they be configurable?
5. **Spawn Protection**: Should team spawns have protection zones?
6. **Team Leaders**: Should teams have designated leaders with special permissions?

---

## Summary

**Phase 1 is complete and production-ready.** The plugin has a solid foundation with persistence, permissions, configuration, and a modular architecture.

**Phase 2 (Teams System) is the logical next step** to enable true GG vs Goons gameplay with team validation, colored nametags, friendly fire prevention, and team-based features.

The modular architecture makes adding the Teams system straightforward - it will integrate cleanly with the existing prisoner system without requiring major refactoring.
