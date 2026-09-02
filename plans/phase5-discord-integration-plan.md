# Phase 5: Discord Integration Plan

## Overview
Create a Discord bot from scratch that integrates with the Minecraft plugin for war scheduling, prisoner notifications, team chat bridging, and event management. This provides a seamless communication layer between Discord and the game server.

## Architecture

### Discord Bot Structure
**Technology Stack:**
- **Language**: Kotlin (matches plugin)
- **Library**: JDA (Java Discord API) 5.x
- **Communication**: Shared database or REST API between bot and plugin

**Project Structure:**
```
discord-bot/
├── src/main/kotlin/com/tyler/ggvsgoons/discord/
│   ├── GGvGoonsBot.kt              # Main bot class
│   ├── commands/                    # Discord slash commands
│   │   ├── WarCommands.kt
│   │   ├── PrisonerCommands.kt
│   │   ├── TeamCommands.kt
│   │   └── StatsCommands.kt
│   ├── listeners/                   # Event listeners
│   │   ├── MessageListener.kt
│   │   ├── ButtonListener.kt
│   │   └── ModalListener.kt
│   ├── bridge/                      # MC <-> Discord bridge
│   │   ├── ChatBridge.kt
│   │   ├── EventBridge.kt
│   │   └── WebSocketServer.kt
│   ├── scheduling/                  # War scheduling system
│   │   ├── WarScheduler.kt
│   │   └── EventManager.kt
│   └── database/                    # Shared data access
│       ├── DatabaseManager.kt
│       └── models/
└── build.gradle.kts
```

### Plugin Integration Module
Location: `src/main/kotlin/com/tyler/ggvsgoons/discord/`

**Components:**
- [`DiscordModule.kt`](src/main/kotlin/com/tyler/ggvsgoons/discord/DiscordModule.kt) - Module registration
- [`DiscordBridge.kt`](src/main/kotlin/com/tyler/ggvsgoons/discord/DiscordBridge.kt) - Communication with Discord bot
- [`WebSocketClient.kt`](src/main/kotlin/com/tyler/ggvsgoons/discord/WebSocketClient.kt) - Real-time communication
- [`DiscordConfig.kt`](src/main/kotlin/com/tyler/ggvsgoons/discord/DiscordConfig.kt) - Configuration management

## Communication Architecture

### Option 1: WebSocket (Recommended)
**Advantages:**
- Real-time bidirectional communication
- Low latency for chat bridging
- Efficient for frequent updates

**Implementation:**
```
Discord Bot (WebSocket Server) <---> Plugin (WebSocket Client)
         ↓                                    ↓
    Discord API                         Bukkit Server
```

### Option 2: Shared Database
**Advantages:**
- Simple to implement
- No network configuration needed
- Good for async operations

**Implementation:**
```
Discord Bot → SQLite/MySQL ← Plugin
```

### Hybrid Approach (Best)
- WebSocket for real-time chat and events
- Database for persistent data (war schedules, stats)

## Features

### 1. War Scheduling System

**Discord Commands:**
- `/war schedule <date> <time> <team1> <team2>` - Schedule a war event
- `/war list` - View upcoming wars
- `/war cancel <id>` - Cancel a scheduled war
- `/war edit <id>` - Edit war details
- `/war notify <id>` - Send reminder notifications

**Features:**
- Interactive date/time picker using Discord modals
- Automatic timezone conversion
- Countdown timers in Discord
- Role mentions for participants
- Calendar integration (Google Calendar API)
- Recurring war schedules (weekly, bi-weekly)

**In-Game Integration:**
- Broadcast war announcements in-game
- `/war schedule` command in Minecraft
- Automatic team notifications 30min/10min/5min before war
- Optional: Auto-teleport teams to war zone at start time

**Discord UI:**
```
📅 Scheduled War Event
━━━━━━━━━━━━━━━━━━━━
⚔️ GG vs Goons
📍 War Zone Arena
🕐 Saturday, Sept 2, 2026 at 8:00 PM CST
⏰ Starts in: 2 hours, 15 minutes

Participants:
🔵 GG Team: 12 confirmed
🔴 Goons Team: 10 confirmed

[✅ Confirm Attendance] [❌ Cancel] [🔔 Remind Me]
```

### 2. Prisoner Notifications

**Automatic Notifications:**
- Prisoner captured (with captor and prisoner names)
- Prisoner released
- Prisoner executed
- Ransom trade initiated
- Ransom trade completed

**Discord Channels:**
- `#prisoner-log` - All prisoner events
- `#team-gg-prisoners` - GG team prisoner updates
- `#team-goons-prisoners` - Goons team prisoner updates

**Notification Format:**
```
🔒 Prisoner Captured!
━━━━━━━━━━━━━━━━━━━━
👤 Prisoner: PlayerName (GG)
⚔️ Captor: EnemyPlayer (Goons)
📍 Location: X: 123, Y: 64, Z: -456
🕐 Time: 2:30 PM CST

[View Stats] [Initiate Ransom]
```

**Interactive Features:**
- Click button to view prisoner stats
- Initiate ransom from Discord
- View prisoner history
- Team leaderboards for captures

### 3. Team Chat Bridge

**Bidirectional Chat:**
- Minecraft team chat → Discord team channel
- Discord team channel → Minecraft team chat
- Faction chat → Discord faction threads (if factions enabled)

**Discord Channels:**
- `#team-gg-chat` - GG team chat bridge
- `#team-goons-chat` - Goons team chat bridge
- `#faction-[name]` - Faction-specific channels (optional)

**Message Format:**

*Discord → Minecraft:*
```
[DISCORD] Username: Hello from Discord!
```

*Minecraft → Discord:*
```
[MC] PlayerName: Hello from Minecraft!
```

**Features:**
- Player status indicators (online/offline)
- Rich embeds for special events
- Mention support (@player in Discord pings in-game)
- Emoji support (convert Discord emojis to text)
- Attachment support (images show as links in-game)
- Rate limiting to prevent spam

### 4. Event Management

**Discord Commands:**
- `/event create <type> <details>` - Create custom event
- `/event list` - View all events
- `/event join <id>` - RSVP to event
- `/event leave <id>` - Cancel RSVP
- `/event start <id>` - Manually start event

**Event Types:**
- Scheduled Wars
- Tournaments
- Raid Events
- Team Meetings
- Training Sessions
- Custom Events

**Event Features:**
- RSVP system with attendance tracking
- Automatic reminders (24h, 1h, 15min before)
- Role-based access (team-specific events)
- Recurring events
- Event rewards (configured in plugin)

### 5. Statistics & Leaderboards

**Discord Commands:**
- `/stats player <name>` - View player statistics
- `/stats team <team>` - View team statistics
- `/stats faction <faction>` - View faction statistics
- `/leaderboard kills` - Top killers
- `/leaderboard captures` - Top captors
- `/leaderboard ransoms` - Most ransoms paid/received

**Statistics Tracked:**
- Kills/Deaths/KDR
- Prisoners captured/released
- Ransoms completed
- Wars participated/won
- Time played
- Team contributions

**Leaderboard Display:**
```
🏆 Top Captors - All Time
━━━━━━━━━━━━━━━━━━━━━━━━
1. 👑 PlayerOne (GG) - 47 captures
2. 🥈 PlayerTwo (Goons) - 42 captures
3. 🥉 PlayerThree (GG) - 38 captures
4. PlayerFour (Goons) - 31 captures
5. PlayerFive (GG) - 28 captures

Updated: 5 minutes ago
[View Full Leaderboard]
```

### 6. Server Status & Monitoring

**Discord Commands:**
- `/server status` - View server status
- `/server players` - List online players
- `/server tps` - View server performance

**Status Display:**
```
🟢 Server Online
━━━━━━━━━━━━━━━━━━━━
👥 Players: 24/50
📊 TPS: 19.8
💾 Memory: 4.2GB / 8GB
⏱️ Uptime: 3 days, 12 hours

Teams:
🔵 GG: 13 online
🔴 Goons: 11 online

[Join Server] [View Map]
```

**Automatic Notifications:**
- Server start/stop
- Server crashes (with error details)
- Low TPS warnings
- Player milestones (first join, 100 hours played, etc.)

## Configuration

### Plugin Config (`config.yml`)
```yaml
discord:
  # Enable/disable Discord integration
  enabled: true
  
  # Connection settings
  connection:
    # WebSocket connection to Discord bot
    websocket:
      enabled: true
      host: "localhost"
      port: 8080
      reconnect-delay: 5
    
    # Shared database (alternative/supplement to WebSocket)
    database:
      enabled: false
      type: "sqlite"  # sqlite, mysql
      host: "localhost"
      port: 3306
      database: "ggvgoons"
      username: "root"
      password: "password"
  
  # Chat bridge settings
  chat-bridge:
    enabled: true
    
    # Team chat bridging
    team-chat:
      enabled: true
      format-to-discord: "[MC] {player}: {message}"
      format-to-minecraft: "[DISCORD] {user}: {message}"
    
    # Faction chat bridging
    faction-chat:
      enabled: true
      use-threads: true  # Use Discord threads for factions
  
  # Notifications
  notifications:
    # Prisoner events
    prisoners:
      enabled: true
      capture: true
      release: true
      execute: true
      ransom: true
    
    # War events
    wars:
      enabled: true
      scheduled: true
      started: true
      ended: true
      reminders: [1440, 60, 15]  # minutes before (24h, 1h, 15min)
    
    # Server events
    server:
      enabled: true
      startup: true
      shutdown: true
      crash: true
      low-tps: true
      tps-threshold: 15.0
  
  # Statistics sync
  statistics:
    enabled: true
    sync-interval: 300  # seconds
    track-playtime: true
    track-kills: true
    track-captures: true
```

### Discord Bot Config (`bot-config.yml`)
```yaml
bot:
  # Bot token (keep secret!)
  token: "YOUR_BOT_TOKEN_HERE"
  
  # Guild (server) ID
  guild-id: "123456789012345678"
  
  # Channel IDs
  channels:
    # Team chat channels
    team-gg-chat: "123456789012345678"
    team-goons-chat: "123456789012345678"
    
    # Notification channels
    prisoner-log: "123456789012345678"
    war-announcements: "123456789012345678"
    server-status: "123456789012345678"
    
    # Admin channels
    admin-log: "123456789012345678"
  
  # Role IDs
  roles:
    team-gg: "123456789012345678"
    team-goons: "123456789012345678"
    admin: "123456789012345678"
    moderator: "123456789012345678"
  
  # WebSocket server
  websocket:
    enabled: true
    port: 8080
    auth-token: "SECURE_TOKEN_HERE"
  
  # Features
  features:
    war-scheduling: true
    chat-bridge: true
    notifications: true
    statistics: true
    leaderboards: true
```

## Implementation Steps

### Phase 5A: Discord Bot Foundation
1. Set up Discord bot project with JDA
2. Implement basic bot connection and authentication
3. Create slash command framework
4. Set up WebSocket server in bot
5. Implement database connection (SQLite for start)
6. Create basic command handlers
7. Test bot deployment

### Phase 5B: Plugin Integration
1. Create `DiscordModule` in plugin
2. Implement WebSocket client in plugin
3. Create `DiscordBridge` for message passing
4. Add configuration options to `config.yml`
5. Register module in `GGvGPlugin.onEnable()`
6. Test connection between plugin and bot

### Phase 5C: Chat Bridge
1. Implement team chat listener in plugin
2. Send team chat messages to Discord via WebSocket
3. Implement Discord message listener in bot
4. Send Discord messages to Minecraft via WebSocket
5. Add message formatting and filtering
6. Implement rate limiting
7. Test bidirectional chat

### Phase 5D: War Scheduling
1. Create war scheduling commands in Discord
2. Implement event storage in database
3. Create war notification system
4. Add in-game war commands
5. Implement countdown timers
6. Add RSVP system
7. Test scheduling and notifications

### Phase 5E: Prisoner Notifications
1. Hook into prisoner events in plugin
2. Send prisoner events to Discord via WebSocket
3. Create rich embeds for prisoner notifications
4. Implement interactive buttons
5. Add prisoner statistics display
6. Test all notification types

### Phase 5F: Statistics & Leaderboards
1. Implement statistics tracking in plugin
2. Sync statistics to database
3. Create leaderboard commands in Discord
4. Implement leaderboard display with embeds
5. Add automatic leaderboard updates
6. Test statistics accuracy

### Phase 5G: Server Monitoring
1. Implement server status tracking
2. Create status display command
3. Add TPS monitoring
4. Implement automatic status updates
5. Add crash detection and reporting
6. Test monitoring features

## Dependencies

### Plugin Dependencies (build.gradle.kts)
```kotlin
dependencies {
    // Existing dependencies...
    
    // WebSocket client
    implementation("org.java-websocket:Java-WebSocket:1.5.3")
    
    // JSON processing
    implementation("com.google.code.gson:gson:2.10.1")
    
    // HTTP client (for REST API fallback)
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
}
```

### Discord Bot Dependencies (build.gradle.kts)
```kotlin
dependencies {
    // JDA (Discord API)
    implementation("net.dv8tion:JDA:5.0.0-beta.18")
    
    // WebSocket server
    implementation("org.java-websocket:Java-WebSocket:1.5.3")
    
    // Database
    implementation("org.xerial:sqlite-jdbc:3.43.0.0")
    implementation("com.zaxxer:HikariCP:5.0.1")
    
    // JSON
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Logging
    implementation("ch.qos.logback:logback-classic:1.4.11")
}
```

## Security Considerations

- Store bot token securely (environment variable or encrypted config)
- Implement authentication for WebSocket connection
- Rate limit Discord commands
- Validate all input from Discord
- Sanitize messages before sending to Minecraft
- Implement permission checks for sensitive commands
- Log all admin actions
- Encrypt WebSocket communication (WSS)

## Testing Checklist

- [ ] Discord bot connects successfully
- [ ] WebSocket connection established
- [ ] Team chat bridge (Discord → MC)
- [ ] Team chat bridge (MC → Discord)
- [ ] War scheduling command
- [ ] War notifications (reminders)
- [ ] Prisoner capture notifications
- [ ] Prisoner release notifications
- [ ] Ransom notifications
- [ ] Statistics commands
- [ ] Leaderboard display
- [ ] Server status command
- [ ] Player list command
- [ ] RSVP system for events
- [ ] Automatic reconnection on disconnect
- [ ] Error handling and logging
- [ ] Permission enforcement

## Deployment

### Discord Bot Setup
1. Create Discord application at https://discord.com/developers
2. Create bot user and get token
3. Enable required intents (Server Members, Message Content)
4. Generate OAuth2 URL with required permissions
5. Invite bot to Discord server
6. Configure channel and role IDs
7. Deploy bot (VPS, Docker, or local server)

### Plugin Configuration
1. Enable Discord module in config.yml
2. Configure WebSocket connection details
3. Set up channel mappings
4. Test connection
5. Enable desired features

## Future Enhancements

- Voice channel integration (move players to voice during wars)
- Discord role sync with in-game ranks
- Custom Discord bot status (player count, server status)
- Slash command autocomplete for player names
- Discord-based voting system
- Integration with Discord events feature
- Multi-server support (multiple MC servers, one Discord)
- Web dashboard for statistics
