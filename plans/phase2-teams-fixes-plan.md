# Phase 2: Teams System - Issues & Fixes Plan

## Issues Identified

### 1. **Critical: TeamPersistance.kt Filename Typo**
- **File**: `src/main/kotlin/com/tyler/ggvsgoons/persistence/TeamPersistance.kt`
- **Issue**: Filename is misspelled as "TeamPersistance" instead of "TeamPersistence"
- **Impact**: Inconsistent with naming convention (PrisonerPersistence uses correct spelling)
- **Fix**: Rename file and update all references in TeamsModule.kt

### 2. **Critical: Missing TeamCommand.kt Implementation**
- **File**: `src/main/kotlin/com/tyler/ggvsgoons/teams/TeamCommand.kt`
- **Issue**: File exists but is completely empty (0 lines of code)
- **Impact**: No team commands are functional (/team join, /team leave, etc.)
- **Required Commands**:
  - `/team join <gg|goons>` - Join a team
  - `/team leave` - Leave current team
  - `/team info [player]` - Show team info
  - `/team list` - List all team members
  - `/team spawn` - Teleport to team spawn
  - `/team setspawn` - Set team spawn point (admin)
  - `/team chat <message>` - Send message to team chat

### 3. **Missing: Team Commands Not Registered**
- **File**: `src/main/resources/plugin.yml`
- **Issue**: No team commands are registered in plugin.yml
- **Impact**: Commands won't be recognized by Bukkit
- **Fix**: Add team command registration with proper permissions

### 4. **Missing: Team Configuration Settings**
- **File**: `src/main/resources/config.yml`
- **Issue**: No team-related configuration options
- **Current Settings Needed**:
  - `teams.allow-team-switching` (already referenced in TeamManager.kt:62)
  - `teams.team-switch-cooldown` (already referenced in TeamManager.kt:68)
  - Team chat prefix/format settings
- **Fix**: Add comprehensive team configuration section

### 5. **Missing: Team Validation in Capture System**
- **File**: `src/main/kotlin/com/tyler/ggvsgoons/commands/WarPrisonerModule.kt`
- **Issue**: No validation to ensure players can only capture opposing team members
- **Current Behavior**: Anyone can capture anyone
- **Required Behavior**: 
  - GG members can only capture Goons members
  - Goons members can only capture GG members
  - Cannot capture teammates
  - Cannot capture players without a team
- **Fix**: Add team validation in WarPrisonerCommand.onCommand() before creating offer

### 6. **Missing: Team Chat Channels**
- **Issue**: No team chat implementation
- **Required Features**:
  - Separate chat channels for each team
  - Team-only messages (not visible to opposing team)
  - Optional chat prefix showing team affiliation
  - Command to send team messages
- **Implementation**: Add PlayerChatEvent listener in TeamsModule

### 7. **Minor: Typo in Team.kt Comment**
- **File**: `src/main/kotlin/com/tyler/ggvsgoons/teams/Team.kt:26`
- **Issue**: "ConvertColored" should be "Colored" or "Color-formatted"
- **Impact**: Documentation clarity

## Implementation Plan

### Step 1: Fix Filename Typo
- Rename `TeamPersistance.kt` to `TeamPersistence.kt`
- Update import in `TeamsModule.kt` line 5

### Step 2: Implement TeamCommand.kt
Create complete command handler with subcommands:

```kotlin
class TeamCommand(
    private val module: TeamsModule,
    private val plugin: GGvGPlugin
) : CommandExecutor, TabCompleter {
    
    // Subcommands:
    // - join <team>
    // - leave
    // - info [player]
    // - list
    // - spawn
    // - setspawn (admin)
    // - chat <message>
}
```

### Step 3: Update config.yml
Add teams configuration section:

```yaml
teams:
  # Allow players to switch teams after joining
  allow-team-switching: true
  
  # Cooldown between team switches (seconds)
  team-switch-cooldown: 300
  
  # Team chat settings
  chat:
    enabled: true
    prefix: "[TEAM] "
    format: "{player}: {message}"
```

### Step 4: Update plugin.yml
Add team command registration:

```yaml
commands:
  team:
    description: Team management commands
    usage: /<command> <join|leave|info|list|spawn|setspawn|chat>
    permission: ggvgoons.team.use
    
permissions:
  ggvgoons.team.use:
    description: Basic team commands
    default: true
  ggvgoons.team.admin:
    description: Team admin commands (setspawn)
    default: op
```

### Step 5: Add Team Validation to Capture System
Modify `WarPrisonerCommand.onCommand()` in WarPrisonerModule.kt:

```kotlin
// After line 69 (can't capture self check)
// Add team validation
val captorTeam = plugin.teams.manager.getPlayerTeam(sender.uniqueId)
val targetTeam = plugin.teams.manager.getPlayerTeam(target.uniqueId)

if (captorTeam == null) {
    sender.sendMessage("${ChatColor.RED}You must join a team first.")
    return true
}

if (targetTeam == null) {
    sender.sendMessage("${ChatColor.RED}${target.name} is not on a team.")
    return true
}

if (!plugin.teams.manager.areOpposingTeams(sender.uniqueId, target.uniqueId)) {
    sender.sendMessage("${ChatColor.RED}You can only capture members of the opposing team.")
    return true
}
```

### Step 6: Implement Team Chat
Add to TeamsModule.kt:

```kotlin
@EventHandler
fun onChat(event: AsyncPlayerChatEvent) {
    val message = event.message
    if (!message.startsWith("@team ")) return
    
    event.isCancelled = true
    val teamMessage = message.substring(6)
    
    val senderTeam = manager.getPlayerTeam(event.player.uniqueId) ?: run {
        event.player.sendMessage("${ChatColor.RED}You're not on a team.")
        return
    }
    
    val prefix = plugin.config.getString("teams.chat.prefix", "[TEAM] ")
    val formatted = "$prefix${senderTeam.displayName} ${event.player.name}: $teamMessage"
    
    // Send to all team members
    manager.getTeamMembers(senderTeam.id).forEach { uuid ->
        plugin.server.getPlayer(uuid)?.sendMessage(formatted)
    }
}
```

### Step 7: Add Team Permissions
Update plugin.yml permissions section with team-specific permissions.

## Architecture Diagram

```mermaid
graph TB
    A[GGvGPlugin] --> B[TeamsModule]
    A --> C[WarPrisonerModule]
    
    B --> D[TeamManager]
    B --> E[TeamPersistence]
    B --> F[TeamCommand]
    
    D --> G[Bukkit Scoreboard]
    E --> H[teams.yml]
    
    C -.team validation.-> D
    
    F --> I[/team join]
    F --> J[/team leave]
    F --> K[/team spawn]
    F --> L[/team chat]
    
    style B fill:#90EE90
    style C fill:#FFB6C1
    style E fill:#FF6B6B
    style F fill:#FF6B6B
```

## File Changes Summary

| File | Change Type | Description |
|------|-------------|-------------|
| `TeamPersistance.kt` | Rename | Fix typo → TeamPersistence.kt |
| `TeamsModule.kt` | Modify | Update import, add chat listener |
| `TeamCommand.kt` | Create | Implement all team commands |
| `plugin.yml` | Modify | Add team command registration |
| `config.yml` | Modify | Add team configuration section |
| `WarPrisonerModule.kt` | Modify | Add team validation to capture |
| `Team.kt` | Minor | Fix comment typo |

## Testing Checklist

After implementation, verify:

- [ ] Players can join teams with `/team join <gg|goons>`
- [ ] Players can leave teams with `/team leave`
- [ ] Team switching respects cooldown configuration
- [ ] Scoreboard shows correct team colors
- [ ] Team spawns can be set and used
- [ ] Team chat works with `@team` prefix or `/team chat`
- [ ] Capture validation prevents same-team captures
- [ ] Capture validation prevents capturing non-team players
- [ ] Team data persists across server restarts
- [ ] All commands have proper permission checks

## Priority Order

1. **High Priority** (Blocking Phase 2 completion):
   - Fix TeamPersistance.kt filename
   - Implement TeamCommand.kt
   - Register commands in plugin.yml
   - Add team validation to capture system

2. **Medium Priority** (Core functionality):
   - Add team configuration to config.yml
   - Implement team chat channels

3. **Low Priority** (Polish):
   - Fix comment typo in Team.kt
   - Add comprehensive testing

## Notes

- The TeamManager and Team classes are well-implemented and don't need changes
- The Bukkit Scoreboard integration is already functional
- TeamPersistence (once renamed) is complete and working
- The module follows the established GGvGModule pattern correctly
