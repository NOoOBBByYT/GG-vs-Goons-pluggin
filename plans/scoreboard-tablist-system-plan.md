# Scoreboard & Tab List System Plan
**Feature Addition to GG vs Goons Plugin**

## Overview
Implement a fully customizable scoreboard and team-separated tab list system that displays server name, team name, faction name, player stats, and other information. The system should be highly configurable through config.yml with color codes, formatting options, and toggle switches.

---

## Features

### 1. Customizable Scoreboard

#### Display Elements:
- **Server Name/Title** (customizable)
- **Player's Team** (GG or Goons with colors)
- **Player's Faction** (if in one)
- **Player Stats**:
  - Kills/Deaths/KDR
  - Prisoners Captured
  - Current Status (Prisoner, Frozen, etc.)
- **Team Stats**:
  - Online team members
  - Total team prisoners
- **Server Info**:
  - Online players count
  - TPS (optional)
  - Current time (optional)

#### Customization Options:
- Enable/disable entire scoreboard
- Toggle individual elements
- Custom colors for each element
- Custom text/labels
- Update interval
- Conditional display (show only when in combat, etc.)

### 2. Team-Separated Tab List

#### Features:
- **Separate sections** for GG and Goons teams
- **Custom headers/footers** for each team
- **Player prefixes** showing:
  - Team color
  - Faction tag (if applicable)
  - Rank/role indicators
  - Status indicators (prisoner, frozen, etc.)
- **Sorting options**:
  - By team first, then alphabetically
  - By faction within team
  - By rank/role
  - Custom sort order

#### Customization Options:
- Header/footer text with placeholders
- Team colors
- Faction tag format
- Status indicator symbols
- Separator lines
- Player name format

---

## Architecture

### New Module: ScoreboardModule

```
src/main/kotlin/com/tyler/ggvsgoons/scoreboard/
├── ScoreboardModule.kt          # Module registration
├── ScoreboardManager.kt         # Scoreboard management
├── TabListManager.kt            # Tab list management
├── ScoreboardRenderer.kt        # Render scoreboard for players
├── TabListRenderer.kt           # Render tab list
├── ScoreboardConfig.kt          # Configuration data class
└── ScoreboardEventListener.kt   # Event handling
```

### Data Structures

```kotlin
data class ScoreboardConfig(
    val enabled: Boolean,
    val title: String,
    val updateInterval: Int,
    val elements: ScoreboardElements,
    val colors: ScoreboardColors
)

data class ScoreboardElements(
    val showServerName: Boolean,
    val showTeam: Boolean,
    val showFaction: Boolean,
    val showKills: Boolean,
    val showDeaths: Boolean,
    val showKDR: Boolean,
    val showPrisoners: Boolean,
    val showStatus: Boolean,
    val showOnlinePlayers: Boolean,
    val showTeamOnline: Boolean,
    val showTPS: Boolean,
    val showTime: Boolean
)

data class ScoreboardColors(
    val title: String,
    val labels: String,
    val values: String,
    val teamGG: String,
    val teamGoons: String,
    val positive: String,
    val negative: String,
    val neutral: String
)

data class TabListConfig(
    val enabled: Boolean,
    val separateByTeam: Boolean,
    val header: String,
    val footer: String,
    val teamHeaders: Map<String, String>,
    val showFactionTags: Boolean,
    val showStatusIndicators: Boolean,
    val sortOrder: String
)
```

---

## Configuration (config.yml)

```yaml
scoreboard:
  # Enable/disable scoreboard system
  enabled: true
  
  # Scoreboard title (supports color codes with &)
  title: "&6&lGG vs Goons"
  
  # Update interval in ticks (20 ticks = 1 second)
  update-interval: 20
  
  # Elements to display
  elements:
    server-name: true
    team: true
    faction: true
    kills: true
    deaths: true
    kdr: true
    prisoners-captured: true
    player-status: true
    online-players: true
    team-online: true
    tps: false
    current-time: false
  
  # Custom labels (supports color codes)
  labels:
    team: "&eTeam:"
    faction: "&eFaction:"
    kills: "&eKills:"
    deaths: "&eDeaths:"
    kdr: "&eK/D:"
    prisoners: "&ePrisoners:"
    status: "&eStatus:"
    online: "&eOnline:"
    team-online: "&eTeam Online:"
  
  # Colors for different elements
  colors:
    title: "&6&l"
    labels: "&e"
    values: "&f"
    team-gg: "&9"
    team-goons: "&c"
    positive: "&a"
    negative: "&c"
    neutral: "&7"
  
  # Conditional display
  conditions:
    # Only show in certain worlds
    enabled-worlds: []
    # Hide when in combat
    hide-in-combat: false
    # Hide when prisoner
    hide-when-prisoner: false

tablist:
  # Enable/disable tab list customization
  enabled: true
  
  # Separate teams into sections
  separate-by-team: true
  
  # Global header (supports placeholders and color codes)
  header: |
    &6&l━━━━━━━━━━━━━━━━━━━━━━━━
    &6&lGG vs Goons Server
    &7Online: &f{online}/{max}
    &6&l━━━━━━━━━━━━━━━━━━━━━━━━
  
  # Global footer
  footer: |
    &6&l━━━━━━━━━━━━━━━━━━━━━━━━
    &7TPS: &f{tps}
    &7Website: &fggvsgoons.com
    &6&l━━━━━━━━━━━━━━━━━━━━━━━━
  
  # Team-specific headers
  team-headers:
    gg: "&9&l▬▬▬▬ GG Team ▬▬▬▬"
    goons: "&c&l▬▬▬▬ Goons Team ▬▬▬▬"
    none: "&7&l▬▬▬▬ No Team ▬▬▬▬"
  
  # Player name format
  player-format:
    # Show faction tags
    show-faction-tags: true
    faction-tag-format: "&7[{faction}] "
    
    # Show status indicators
    show-status-indicators: true
    status-indicators:
      prisoner: "&c⛓ "
      frozen: "&b❄ "
      leader: "&6★ "
      officer: "&e⚑ "
    
    # Team colors
    team-colors:
      gg: "&9"
      goons: "&c"
      none: "&7"
  
  # Sorting
  sort-order: "team-faction-name"  # Options: team-faction-name, team-name, faction-name, name
  
  # Ping display
  ping:
    show-ping: true
    format: " &7({ping}ms)"
    colors:
      excellent: "&a"  # 0-50ms
      good: "&e"       # 51-100ms
      fair: "&6"       # 101-200ms
      poor: "&c"       # 201+ms

# Placeholders available:
# {player} - Player name
# {team} - Team name
# {faction} - Faction name
# {kills} - Kill count
# {deaths} - Death count
# {kdr} - K/D ratio
# {prisoners} - Prisoners captured
# {online} - Online players
# {max} - Max players
# {tps} - Server TPS
# {time} - Current time
# {team_online} - Team members online
# {status} - Player status
```

---

## Implementation Steps

### Step 1: Create Scoreboard Manager
1. Create `ScoreboardManager.kt`
2. Implement scoreboard creation and updates
3. Add player-specific scoreboard tracking
4. Implement update scheduling

### Step 2: Create Scoreboard Renderer
1. Create `ScoreboardRenderer.kt`
2. Implement line rendering with placeholders
3. Add color code support
4. Implement conditional display logic

### Step 3: Create Tab List Manager
1. Create `TabListManager.kt`
2. Implement team separation logic
3. Add header/footer management
4. Implement player sorting

### Step 4: Create Tab List Renderer
1. Create `TabListRenderer.kt`
2. Implement player prefix/suffix system
3. Add faction tag rendering
4. Implement status indicators

### Step 5: Event Listeners
1. Create `ScoreboardEventListener.kt`
2. Handle player join (create scoreboard)
3. Handle player quit (cleanup)
4. Handle team changes (update scoreboard)
5. Handle faction changes (update tab list)
6. Handle stat changes (update displays)

### Step 6: Configuration
1. Add all config options to `config.yml`
2. Create `ScoreboardConfig.kt` data class
3. Implement config loading and validation
4. Add reload command support

### Step 7: Module Registration
1. Create `ScoreboardModule.kt`
2. Register with main plugin
3. Initialize managers
4. Schedule update tasks

### Step 8: Integration
1. Hook into team system for updates
2. Hook into faction system for updates
3. Hook into prisoner system for status
4. Hook into admin system for frozen status

---

## Commands

### Admin Commands
```
/ggadmin scoreboard reload - Reload scoreboard configuration
/ggadmin scoreboard toggle - Enable/disable scoreboard
/ggadmin scoreboard update - Force update all scoreboards
/ggadmin tablist reload - Reload tab list configuration
/ggadmin tablist toggle - Enable/disable tab list
```

### Player Commands
```
/scoreboard toggle - Toggle personal scoreboard on/off
/scoreboard - Show scoreboard help
```

---

## Permissions

```yaml
permissions:
  ggvgoons.scoreboard.toggle:
    description: Toggle personal scoreboard
    default: true
  
  ggvgoons.scoreboard.admin:
    description: Admin scoreboard commands
    default: op
  
  ggvgoons.tablist.admin:
    description: Admin tab list commands
    default: op
```

---

## Technical Details

### Scoreboard Implementation
- Use Bukkit Scoreboard API
- Create individual scoreboards per player
- Update asynchronously to avoid lag
- Cache rendered lines to reduce processing

### Tab List Implementation
- Use Player.setPlayerListName() for prefixes
- Use Player.setPlayerListHeaderFooter() for headers/footers
- Update on team/faction changes
- Sort players using custom comparator

### Performance Considerations
- Batch updates when possible
- Use async tasks for rendering
- Cache formatted strings
- Limit update frequency
- Only update changed elements

### Placeholder System
- Create placeholder resolver
- Support custom placeholders
- Allow plugins to register placeholders
- Cache resolved values

---

## Testing Checklist

- [ ] Scoreboard displays correctly for all players
- [ ] Team colors show properly
- [ ] Faction names display when applicable
- [ ] Stats update in real-time
- [ ] Tab list separates teams correctly
- [ ] Headers/footers display properly
- [ ] Player prefixes show faction tags
- [ ] Status indicators work (prisoner, frozen)
- [ ] Sorting works correctly
- [ ] Configuration reload works
- [ ] Toggle commands work
- [ ] Performance is acceptable with 50+ players
- [ ] No flickering or visual glitches
- [ ] Color codes parse correctly
- [ ] Placeholders resolve properly

---

## Example Scoreboard Display

```
    ╔═══════════════╗
    ║ GG vs Goons   ║
    ╚═══════════════╝

Team: GG
Faction: Warriors

Kills: 47
Deaths: 23
K/D: 2.04
Prisoners: 12

Status: Active

Team Online: 15
Online: 32/50
```

## Example Tab List Display

```
━━━━━━━━━━━━━━━━━━━━━━━━
    GG vs Goons Server
    Online: 32/50
━━━━━━━━━━━━━━━━━━━━━━━━

▬▬▬▬ GG Team ▬▬▬▬
★ [Warriors] PlayerOne
⚑ [Warriors] PlayerTwo
[Warriors] PlayerThree
[Defenders] PlayerFour

▬▬▬▬ Goons Team ▬▬▬▬
★ [Raiders] EnemyOne
⚑ [Raiders] EnemyTwo
⛓ [Raiders] EnemyThree (Prisoner)

━━━━━━━━━━━━━━━━━━━━━━━━
TPS: 19.8
Website: ggvsgoons.com
━━━━━━━━━━━━━━━━━━━━━━━━
```

---

## Future Enhancements

- Animated scoreboard titles
- Per-player scoreboard customization
- Multiple scoreboard layouts
- Scoreboard API for other plugins
- Tab list animations
- Custom tab list layouts
- Player head textures in tab list
- Faction vs faction comparisons
- Leaderboard integration
- Discord status integration

---

**Status**: Ready for Implementation  
**Priority**: High  
**Complexity**: Medium  
**Dependencies**: Teams Module, Factions Module
