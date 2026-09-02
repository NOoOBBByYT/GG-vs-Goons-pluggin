# Phase 4: Factions System Plan

## Overview
Implement a faction system that works as sub-groups within the existing team structure (GG vs Goons). Each team can have 2-3 factions, and the system can be completely disabled via configuration.

## Architecture

### New Module: FactionsModule
Location: `src/main/kotlin/com/tyler/ggvsgoons/factions/`

**Components:**
- [`FactionsModule.kt`](src/main/kotlin/com/tyler/ggvsgoons/factions/FactionsModule.kt) - Module registration and initialization
- [`FactionManager.kt`](src/main/kotlin/com/tyler/ggvsgoons/factions/FactionManager.kt) - Core faction management logic
- [`FactionCommand.kt`](src/main/kotlin/com/tyler/ggvsgoons/factions/FactionCommand.kt) - Command executor for `/faction` commands
- [`Faction.kt`](src/main/kotlin/com/tyler/ggvsgoons/factions/Faction.kt) - Faction data class
- [`FactionPersistence.kt`](src/main/kotlin/com/tyler/ggvsgoons/persistence/FactionPersistence.kt) - Save/load faction data

### Data Structures

```kotlin
data class Faction(
    val id: String,                    // Unique identifier
    val name: String,                  // Display name
    val teamId: String,                // Parent team ("gg" or "goons")
    val leaderId: UUID,                // Faction leader
    val members: MutableSet<UUID>,     // All members including leader
    val officers: MutableSet<UUID>,    // Officers (can invite/kick)
    val description: String = "",
    val color: ChatColor = ChatColor.WHITE,
    val homeLocation: Location? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val maxMembers: Int = 10           // Configurable per faction
)

data class FactionInvite(
    val factionId: String,
    val inviterId: UUID,
    val inviteeId: UUID,
    val expiresAt: Long
)

data class FactionStats(
    val factionId: String,
    val totalKills: Int = 0,
    val totalDeaths: Int = 0,
    val prisonersCaptured: Int = 0,
    val territoryClaimed: Int = 0
)
```

## Features

### 1. Faction Management

**Commands:**
- `/faction create <name>` - Create a new faction (requires team membership)
- `/faction disband` - Disband your faction (leader only)
- `/faction info [faction]` - View faction information
- `/faction list` - List all factions (organized by team)
- `/faction leave` - Leave your current faction
- `/faction transfer <player>` - Transfer leadership (leader only)

**Validation:**
- Must be on a team to create a faction
- Limit of 2-3 factions per team (configurable)
- Faction names must be unique within a team
- Cannot be in multiple factions simultaneously

### 2. Member Management

**Commands:**
- `/faction invite <player>` - Invite a teammate to your faction (leader/officer)
- `/faction kick <player>` - Remove a member (leader/officer)
- `/faction promote <player>` - Promote to officer (leader only)
- `/faction demote <player>` - Demote from officer (leader only)
- `/faction accept <faction>` - Accept a faction invite
- `/faction decline <faction>` - Decline a faction invite

**Permissions Hierarchy:**
- **Leader**: Full control (disband, transfer, promote, demote, invite, kick)
- **Officer**: Can invite and kick members
- **Member**: Basic faction benefits

### 3. Faction Features

**Home/Base System:**
- `/faction sethome` - Set faction home location (leader/officer)
- `/faction home` - Teleport to faction home
- Configurable cooldown and warmup time

**Faction Chat:**
- `/faction chat <message>` - Send message to faction only
- `/fc <message>` - Shorthand for faction chat
- `@faction` prefix in regular chat

**Faction Allies (Optional):**
- `/faction ally <faction>` - Request alliance with another faction
- `/faction unally <faction>` - Break alliance
- Allies cannot damage each other
- Shared faction chat channel

### 4. Configuration System

**Config Options:**
```yaml
factions:
  # Enable/disable entire faction system
  enabled: true
  
  # Maximum factions per team
  max-factions-per-team: 3
  
  # Faction creation
  creation:
    # Minimum team members required to create faction
    min-team-members: 3
    
    # Cost to create faction (optional)
    creation-cost:
      enabled: false
      item: DIAMOND
      amount: 10
  
  # Member limits
  members:
    # Default max members per faction
    default-max-members: 10
    
    # Allow factions to increase member limit
    allow-expansion: true
    max-expansion-limit: 20
  
  # Faction homes
  homes:
    enabled: true
    teleport-warmup: 5  # seconds
    teleport-cooldown: 300  # seconds
    cancel-on-damage: true
  
  # Faction chat
  chat:
    enabled: true
    prefix: "[FACTION] "
    format: "{faction} {player}: {message}"
    color-by-faction: true
  
  # Alliances
  alliances:
    enabled: false
    max-allies: 2
    require-mutual-acceptance: true
  
  # Integration with other systems
  integration:
    # Faction-based prisoner bonuses
    prisoner-bonus-enabled: true
    prisoner-bonus-multiplier: 1.5
    
    # Faction-based ransom discounts
    ransom-discount-enabled: true
    ransom-discount-percent: 10
  
  # Persistence
  enable-persistence: true
```

### 5. Disable Mechanism

**When `factions.enabled: false`:**
- All faction commands return "Factions system is disabled"
- Existing faction data is preserved but not loaded
- No faction-related events are processed
- Faction chat is disabled
- Players retain team membership (teams work independently)
- Can be re-enabled without data loss

**Admin Commands:**
- `/faction admin enable` - Enable faction system
- `/faction admin disable` - Disable faction system
- `/faction admin reload` - Reload faction configuration

### 6. Integration Points

#### With TeamsModule
- Factions are sub-groups of teams
- Must be on a team to join/create a faction
- Leaving a team automatically removes from faction
- Team chat and faction chat are separate

#### With WarPrisonerModule
- Track prisoner captures per faction
- Optional: Faction bonuses for capturing enemy faction members
- Faction stats include prisoner counts

#### With RansomModule (Phase 3)
- Optional: Faction members can pool resources for ransoms
- Faction-based ransom discounts (configurable)
- Faction treasury for collective trades

#### With Discord Integration (Phase 5)
- Faction-specific Discord channels
- Faction event notifications
- Faction leaderboards

## Commands & Permissions

### Commands
```yaml
faction:
  description: Faction management commands
  usage: /<command> <create|disband|info|list|invite|kick|etc> [args]
  aliases: [f, fac]
  permission: ggvgoons.faction.use

factionchat:
  description: Send message to faction
  usage: /<command> <message>
  aliases: [fc]
  permission: ggvgoons.faction.chat
```

### Permissions
```yaml
permissions:
  ggvgoons.faction.use:
    description: Basic faction commands
    default: true
  
  ggvgoons.faction.create:
    description: Create a faction
    default: true
  
  ggvgoons.faction.chat:
    description: Use faction chat
    default: true
  
  ggvgoons.faction.home:
    description: Use faction home teleport
    default: true
  
  ggvgoons.faction.admin:
    description: Admin faction commands
    default: op
    children:
      ggvgoons.faction.admin.enable: true
      ggvgoons.faction.admin.disable: true
      ggvgoons.faction.admin.disband: true
      ggvgoons.faction.admin.reload: true
      ggvgoons.faction.bypass-limits: true
```

## Event Listeners

### Required Events
- `PlayerJoinEvent` - Restore faction scoreboard tags
- `PlayerQuitEvent` - Save faction data
- `AsyncPlayerChatEvent` - Handle faction chat prefix
- `PlayerTeleportEvent` - Handle faction home teleports
- `EntityDamageByEntityEvent` - Cancel damage during teleport warmup
- Custom events from TeamsModule for team changes

## Implementation Steps

1. Create `FactionsModule` class implementing `GGvGModule`
2. Implement `Faction` data class with all properties
3. Create `FactionManager` with faction CRUD operations
4. Implement member management (invite, kick, promote, demote)
5. Add faction home system with teleport logic
6. Implement faction chat system
7. Create `FactionCommand` with all subcommands
8. Add event listeners for chat and teleportation
9. Implement `FactionPersistence` for save/load
10. Add configuration options to `config.yml`
11. Update `plugin.yml` with new commands and permissions
12. Register module in `GGvGPlugin.onEnable()` with enable check
13. Add integration hooks with `TeamsModule`
14. Implement disable/enable mechanism
15. Add faction statistics tracking
16. Write unit tests
17. Update README with faction system documentation

## Testing Checklist

- [ ] Faction creation with team validation
- [ ] Faction limit enforcement (2-3 per team)
- [ ] Member invitation and acceptance
- [ ] Member kicking and leaving
- [ ] Leadership transfer
- [ ] Officer promotion/demotion
- [ ] Faction home set and teleport
- [ ] Teleport warmup and cooldown
- [ ] Faction chat functionality
- [ ] Faction disbanding
- [ ] Enable/disable system functionality
- [ ] Persistence across server restarts
- [ ] Integration with team system
- [ ] Permission enforcement
- [ ] Admin commands
- [ ] Faction list and info display

## UI/UX Considerations

### Visual Feedback
- Faction tags in chat (optional)
- Colored faction names
- Faction prefix in scoreboard (if implemented)
- Clear hierarchy indicators (leader/officer/member)

### User Experience
- Intuitive command structure
- Clear error messages
- Confirmation prompts for destructive actions
- Helpful command suggestions
- Tab completion for faction names

## Security Considerations

- Validate team membership before faction operations
- Prevent faction name exploits (length, special chars)
- Rate limit faction creation
- Prevent leadership transfer exploits
- Validate permissions for all operations
- Prevent teleport exploits (combat logging)

## Performance Considerations

- Cache faction data in memory
- Efficient faction lookups by player UUID
- Batch save operations
- Limit faction list queries
- Optimize chat event handling
- Clean up expired invites regularly

## Migration Strategy

### For Existing Servers
1. Factions start disabled by default
2. Admin enables when ready
3. No impact on existing team system
4. Players can gradually form factions
5. No forced migration required

### Data Compatibility
- Faction data stored separately from teams
- Can disable without losing data
- Re-enabling restores all faction state
- Backward compatible with team-only mode

## Future Enhancements (Post-Phase 4)

- Faction wars (scheduled battles between factions)
- Faction territory claiming
- Faction banks/treasuries
- Faction ranks beyond leader/officer/member
- Faction achievements and rewards
- Cross-team alliances (if desired)
- Faction power system (based on activity)
- Faction upgrades (increased member limits, perks)
