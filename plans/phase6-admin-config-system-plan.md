# Phase 6: Admin Configuration System Plan

## Overview
Implement a comprehensive command-based admin configuration system that allows server administrators to modify all plugin settings in real-time without requiring server restarts or direct file editing. This includes enabling/disabling modules, adjusting cooldowns, managing permissions, and moderating player actions.

## Architecture

### New Module: AdminConfigModule
Location: `src/main/kotlin/com/tyler/ggvsgoons/admin/`

**Components:**
- [`AdminConfigModule.kt`](src/main/kotlin/com/tyler/ggvsgoons/admin/AdminConfigModule.kt) - Module registration
- [`AdminConfigManager.kt`](src/main/kotlin/com/tyler/ggvsgoons/admin/AdminConfigManager.kt) - Configuration management
- [`AdminCommand.kt`](src/main/kotlin/com/tyler/ggvsgoons/admin/AdminCommand.kt) - Main admin command executor
- [`ConfigValidator.kt`](src/main/kotlin/com/tyler/ggvsgoons/admin/ConfigValidator.kt) - Validate configuration changes
- [`ModerationManager.kt`](src/main/kotlin/com/tyler/ggvsgoons/admin/ModerationManager.kt) - Player moderation actions
- [`PermissionManager.kt`](src/main/kotlin/com/tyler/ggvsgoons/admin/PermissionManager.kt) - Runtime permission management
- [`AuditLogger.kt`](src/main/kotlin/com/tyler/ggvsgoons/admin/AuditLogger.kt) - Log all admin actions

### Data Structures

```kotlin
data class ConfigChange(
    val adminId: UUID,
    val adminName: String,
    val path: String,
    val oldValue: Any?,
    val newValue: Any?,
    val timestamp: Long = System.currentTimeMillis(),
    val reason: String? = null
)

data class ModerationAction(
    val actionId: UUID,
    val adminId: UUID,
    val targetId: UUID,
    val actionType: ModActionType,
    val reason: String,
    val duration: Long? = null,  // For temporary actions
    val timestamp: Long = System.currentTimeMillis()
)

enum class ModActionType {
    DISABLE_COMMAND,
    ENABLE_COMMAND,
    FREEZE_PLAYER,
    UNFREEZE_PLAYER,
    FORCE_FREE_PRISONER,
    CANCEL_TRADE,
    KICK_FROM_TEAM,
    KICK_FROM_FACTION,
    RESET_COOLDOWN,
    GRANT_PERMISSION,
    REVOKE_PERMISSION
}

data class CommandRestriction(
    val command: String,
    val disabled: Boolean,
    val disabledFor: Set<UUID> = emptySet(),  // Specific players
    val reason: String? = null
)
```

## Features

### 1. Module Management

**Commands:**
- `/ggadmin module list` - List all modules and their status
- `/ggadmin module enable <module>` - Enable a module
- `/ggadmin module disable <module>` - Disable a module
- `/ggadmin module reload <module>` - Reload a specific module
- `/ggadmin module info <module>` - View module details

**Supported Modules:**
- `warprisoner` - War prisoner system
- `teams` - Team system
- `ransom` - Ransom/trading system
- `factions` - Faction system
- `discord` - Discord integration

**Features:**
- Hot-reload modules without server restart
- Graceful shutdown of disabled modules
- Dependency checking (e.g., ransom requires warprisoner)
- State preservation when disabling

### 2. Configuration Management

**Commands:**
- `/ggadmin config get <path>` - View a configuration value
- `/ggadmin config set <path> <value>` - Set a configuration value
- `/ggadmin config list [section]` - List all config options
- `/ggadmin config reset <path>` - Reset to default value
- `/ggadmin config reload` - Reload config from file
- `/ggadmin config save` - Save current config to file

**Configuration Paths:**
```
prisoner.offer-expiry-timeout
prisoner.enable-persistence
teams.allow-team-switching
teams.team-switch-cooldown
teams.chat.enabled
ransom.enabled
ransom.restrictions.max-items-per-side
ransom.trade-timeout
factions.enabled
factions.max-factions-per-team
discord.enabled
discord.chat-bridge.enabled
permissions.enabled
```

**Examples:**
```
/ggadmin config set teams.team-switch-cooldown 600
/ggadmin config set ransom.enabled false
/ggadmin config get prisoner.offer-expiry-timeout
/ggadmin config list teams
```

**Features:**
- Tab completion for config paths
- Type validation (int, boolean, string, etc.)
- Range validation (e.g., cooldowns must be >= 0)
- Immediate effect (no restart required)
- Audit logging of all changes
- Rollback capability

### 3. Command Management

**Commands:**
- `/ggadmin command list` - List all plugin commands
- `/ggadmin command disable <command> [player]` - Disable a command globally or for specific player
- `/ggadmin command enable <command> [player]` - Enable a command
- `/ggadmin command status <command>` - Check command status
- `/ggadmin command cooldown <command> <player> reset` - Reset command cooldown

**Examples:**
```
/ggadmin command disable warprisoner
/ggadmin command disable ransom PlayerName
/ggadmin command enable team
/ggadmin command cooldown team PlayerName reset
```

**Features:**
- Disable commands globally or per-player
- Temporary disables with auto-expiry
- Bypass for admins (configurable)
- Custom disable messages
- Command usage statistics

### 4. Permission Management

**Commands:**
- `/ggadmin permission list [player]` - List permissions
- `/ggadmin permission grant <player> <permission>` - Grant permission
- `/ggadmin permission revoke <player> <permission>` - Revoke permission
- `/ggadmin permission check <player> <permission>` - Check if player has permission
- `/ggadmin permission group <player> <group>` - Assign permission group

**Permission Groups:**
- `default` - Basic player permissions
- `vip` - VIP player permissions
- `moderator` - Moderator permissions
- `admin` - Full admin permissions

**Examples:**
```
/ggadmin permission grant PlayerName ggvgoons.ransom.bypass-restrictions
/ggadmin permission revoke PlayerName ggvgoons.warprisoner.capture
/ggadmin permission check PlayerName ggvgoons.team.admin
/ggadmin permission group PlayerName moderator
```

**Features:**
- Runtime permission changes (no restart)
- Integration with LuckPerms (if installed)
- Fallback to internal permission system
- Temporary permissions with expiry
- Permission inheritance

### 5. Player Moderation

**Commands:**
- `/ggadmin player freeze <player>` - Freeze player (cannot move/interact)
- `/ggadmin player unfreeze <player>` - Unfreeze player
- `/ggadmin player freeprisoner <player>` - Force free a prisoner
- `/ggadmin player resetcooldowns <player>` - Reset all cooldowns
- `/ggadmin player kickteam <player>` - Remove from team
- `/ggadmin player kickfaction <player>` - Remove from faction
- `/ggadmin player info <player>` - View player details
- `/ggadmin player teleport <player> <location>` - Teleport player

**Examples:**
```
/ggadmin player freeze GrieferName
/ggadmin player freeprisoner InnocentPlayer
/ggadmin player resetcooldowns PlayerName
/ggadmin player info PlayerName
```

**Features:**
- Freeze players for investigation
- Force-free prisoners (abuse prevention)
- Reset cooldowns (bug fixes)
- Remove from teams/factions
- View comprehensive player info
- Reason tracking for all actions

### 6. Monitoring & Statistics

**Commands:**
- `/ggadmin stats server` - Server statistics
- `/ggadmin stats module <module>` - Module-specific stats
- `/ggadmin stats player <player>` - Player statistics
- `/ggadmin stats team <team>` - Team statistics
- `/ggadmin monitor start` - Start real-time monitoring
- `/ggadmin monitor stop` - Stop monitoring

**Server Statistics:**
- Active prisoners count
- Active trades count
- Players per team
- Factions count
- Commands executed (last hour/day)
- Module status
- Performance metrics

**Real-time Monitoring:**
- Live feed of plugin events
- Command usage tracking
- Error detection
- Performance warnings
- Configurable filters

### 7. Audit Logging

**Commands:**
- `/ggadmin audit view [page]` - View audit log
- `/ggadmin audit search <query>` - Search audit log
- `/ggadmin audit filter <type>` - Filter by action type
- `/ggadmin audit export` - Export audit log to file

**Logged Actions:**
- All configuration changes
- Module enable/disable
- Command restrictions
- Permission changes
- Moderation actions
- Player actions (captures, trades, etc.)

**Log Format:**
```
[2026-09-02 05:15:30] [ADMIN] AdminName changed config 'teams.team-switch-cooldown' from 300 to 600
[2026-09-02 05:16:45] [ADMIN] AdminName disabled command 'warprisoner' globally
[2026-09-02 05:17:12] [MOD] ModName froze player 'GrieferName' - Reason: Investigation
[2026-09-02 05:18:00] [PLAYER] PlayerOne captured PlayerTwo as prisoner
```

**Features:**
- Persistent log storage
- Searchable and filterable
- Export to CSV/JSON
- Automatic rotation
- Configurable retention period

### 8. Backup & Restore

**Commands:**
- `/ggadmin backup create [name]` - Create configuration backup
- `/ggadmin backup list` - List all backups
- `/ggadmin backup restore <name>` - Restore from backup
- `/ggadmin backup delete <name>` - Delete a backup
- `/ggadmin backup auto <enable|disable>` - Toggle automatic backups

**Features:**
- Automatic backups before major changes
- Manual backup creation
- Point-in-time restore
- Backup verification
- Configurable backup retention

## Configuration

### Config Options (`config.yml`)
```yaml
admin:
  # Enable admin configuration system
  enabled: true
  
  # Audit logging
  audit:
    enabled: true
    log-file: "audit.log"
    retention-days: 90
    log-player-actions: true
    log-config-changes: true
    log-mod-actions: true
  
  # Backups
  backups:
    enabled: true
    auto-backup: true
    auto-backup-interval: 3600  # seconds (1 hour)
    max-backups: 10
    backup-directory: "backups"
  
  # Command restrictions
  commands:
    allow-admin-bypass: true
    custom-disable-message: "&cThis command is currently disabled."
  
  # Permissions
  permissions:
    use-luckperms: true  # Use LuckPerms if available
    fallback-to-internal: true
  
  # Monitoring
  monitoring:
    enabled: true
    performance-warnings: true
    error-notifications: true
    notify-admins: true
```

## Commands & Permissions

### Main Command
```yaml
ggadmin:
  description: Admin configuration and moderation commands
  usage: /<command> <subcommand> [args]
  aliases: [admin, ggconfig]
  permission: ggvgoons.admin.use
```

### Permissions
```yaml
permissions:
  ggvgoons.admin.use:
    description: Access to admin commands
    default: op
  
  ggvgoons.admin.config:
    description: Modify configuration
    default: op
  
  ggvgoons.admin.module:
    description: Enable/disable modules
    default: op
  
  ggvgoons.admin.command:
    description: Manage command restrictions
    default: op
  
  ggvgoons.admin.permission:
    description: Manage permissions
    default: op
  
  ggvgoons.admin.moderate:
    description: Player moderation actions
    default: op
  
  ggvgoons.admin.audit:
    description: View audit logs
    default: op
  
  ggvgoons.admin.backup:
    description: Create and restore backups
    default: op
  
  ggvgoons.admin.monitor:
    description: Use monitoring tools
    default: op
  
  ggvgoons.admin.*:
    description: All admin permissions
    default: op
    children:
      ggvgoons.admin.config: true
      ggvgoons.admin.module: true
      ggvgoons.admin.command: true
      ggvgoons.admin.permission: true
      ggvgoons.admin.moderate: true
      ggvgoons.admin.audit: true
      ggvgoons.admin.backup: true
      ggvgoons.admin.monitor: true
```

## Implementation Steps

1. Create `AdminConfigModule` class implementing `GGvGModule`
2. Implement `AdminConfigManager` for runtime config changes
3. Create `ConfigValidator` for input validation
4. Implement `ModerationManager` for player actions
5. Create `PermissionManager` with LuckPerms integration
6. Implement `AuditLogger` with file persistence
7. Create `AdminCommand` with all subcommands
8. Add tab completion for all commands
9. Implement backup/restore system
10. Add monitoring and statistics
11. Create event listeners for audit logging
12. Update `config.yml` with admin settings
13. Update `plugin.yml` with admin commands
14. Register module in `GGvGPlugin.onEnable()`
15. Add integration hooks with all other modules
16. Write comprehensive tests
17. Update README with admin documentation

## Integration Points

### With All Modules
- Enable/disable any module at runtime
- Access module-specific configuration
- View module statistics
- Force module reloads

### With WarPrisonerModule
- Force free prisoners
- View prisoner statistics
- Disable capture commands
- Reset prisoner cooldowns

### With RansomModule
- Cancel active trades
- View trade details
- Modify trade restrictions
- Disable ransom system

### With FactionsModule
- Disband factions
- Remove players from factions
- Modify faction limits
- Enable/disable faction system

### With DiscordModule
- Restart Discord connection
- View connection status
- Modify Discord settings
- Test notifications

## Testing Checklist

- [ ] Module enable/disable
- [ ] Configuration get/set/reset
- [ ] Configuration validation
- [ ] Command disable/enable (global)
- [ ] Command disable/enable (per-player)
- [ ] Permission grant/revoke
- [ ] Player freeze/unfreeze
- [ ] Force free prisoner
- [ ] Reset cooldowns
- [ ] Kick from team/faction
- [ ] Audit log viewing
- [ ] Audit log searching
- [ ] Backup creation
- [ ] Backup restoration
- [ ] Monitoring system
- [ ] Statistics display
- [ ] Tab completion
- [ ] Permission enforcement
- [ ] LuckPerms integration
- [ ] Error handling

## UI/UX Considerations

### Command Feedback
- Clear success/error messages
- Confirmation prompts for destructive actions
- Progress indicators for long operations
- Helpful suggestions on errors

### Tab Completion
- Complete config paths
- Complete module names
- Complete player names
- Complete command names
- Complete permission nodes

### Help System
- `/ggadmin help` - Main help menu
- `/ggadmin help <subcommand>` - Detailed help
- In-game documentation
- Example commands

## Security Considerations

- Require strong permissions for all admin commands
- Log all admin actions with timestamps
- Validate all input to prevent exploits
- Rate limit admin commands
- Require confirmation for destructive actions
- Encrypt sensitive config values
- Prevent permission escalation
- Audit log tampering prevention

## Performance Considerations

- Cache configuration values
- Async file operations for backups
- Efficient audit log storage
- Limit monitoring overhead
- Optimize permission checks
- Batch configuration updates
- Clean up old audit logs automatically

## Error Handling

- Graceful handling of invalid config values
- Rollback on failed configuration changes
- Automatic backup before risky operations
- Clear error messages with solutions
- Notification to admins on critical errors
- Fallback to defaults on corruption

## Documentation

### In-Game Help
```
/ggadmin help - Show this help menu

Module Management:
  /ggadmin module list - List all modules
  /ggadmin module enable <module> - Enable a module
  /ggadmin module disable <module> - Disable a module

Configuration:
  /ggadmin config get <path> - Get config value
  /ggadmin config set <path> <value> - Set config value
  /ggadmin config list [section] - List config options

Commands:
  /ggadmin command disable <cmd> [player] - Disable command
  /ggadmin command enable <cmd> [player] - Enable command

Moderation:
  /ggadmin player freeze <player> - Freeze player
  /ggadmin player freeprisoner <player> - Force free prisoner

For detailed help: /ggadmin help <subcommand>
```

## Future Enhancements

- Web-based admin panel
- Mobile app for remote administration
- Advanced analytics and reporting
- Scheduled configuration changes
- Configuration templates
- Multi-admin collaboration tools
- Integration with external monitoring tools
- Machine learning for anomaly detection
