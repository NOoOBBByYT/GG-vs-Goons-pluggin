# Phase 1: Core Stability Implementation Plan

## Overview
This plan addresses the 5 core stability items from the README:
1. ✅ Add YAML persistence for prisoner state across restarts
2. ✅ Implement offer expiry with configurable timeout
3. ✅ Add permission nodes to all commands
4. ✅ Create `config.yml` for customization
5. ✅ Add `/listprisoners` command to see all active prisoners

## Current Architecture Analysis

### Existing Components
- **GGvGPlugin**: Main plugin class with module system
- **PrisonerManager**: Manages prisoner state (in-memory only)
  - `pendingOffers: Map<UUID, PendingOffer>` - Capture offers awaiting response
  - `prisoners: Map<UUID, Prisoner>` - Active prisoners with captor info
- **WarPrisonerModule**: Command registration and execution
- **Commands**: `/warprisoner`, `/freeprisoner`, `/executeprisoner`, `/warprisoneraccept`, `/warprisonerdecline`

### Current Limitations
- All state is in-memory (lost on restart)
- No offer expiry mechanism
- No permission system (OP-only)
- No configuration file
- No way to list all prisoners

## Implementation Strategy

### 1. YAML Persistence System

#### Data Structure Design
```yaml
# plugins/GGvGoons/prisoners.yml
prisoners:
  # prisonerId: captorId, previousGameMode
  550e8400-e29b-41d4-a716-446655440000:
    captor: 123e4567-e89b-12d3-a456-426614174000
    previousGameMode: SURVIVAL
  
# Note: pendingOffers are NOT persisted (they expire on restart)
```

#### Implementation Details
- Create `src/main/kotlin/com/tyler/ggvsgoons/persistence/` package
- New class: `PrisonerPersistence.kt`
  - `savePrisoners(prisoners: Map<UUID, Prisoner>, file: File)`
  - `loadPrisoners(file: File): Map<UUID, Prisoner>`
- Use Bukkit's `YamlConfiguration` API
- Store in `plugins/GGvGoons/prisoners.yml`

#### Integration Points
- **GGvGPlugin.onEnable()**: Load prisoners after module initialization
- **GGvGPlugin.onDisable()**: Save prisoners before shutdown
- **PrisonerManager**: Add `getPrisoners()` method for persistence access

### 2. Offer Expiry System

#### Design
- Add configurable timeout (default: 60 seconds)
- Use `BukkitRunnable` to schedule expiry tasks
- Clean up expired offers automatically

#### Implementation Details
- Modify `PrisonerManager.createOffer()` to schedule expiry task
- Store task IDs to cancel if offer is accepted/declined early
- Add `offerExpiryTasks: Map<UUID, BukkitTask>` to track scheduled tasks
- Notify both players when offer expires

#### Expiry Behavior
- After timeout: Remove offer from `pendingOffers`
- Send message to target: "The capture offer from [captor] has expired"
- Send message to captor: "Your capture offer to [target] has expired"

### 3. Configuration System

#### config.yml Structure
```yaml
# GGvGoons Configuration

# Prisoner System Settings
prisoner:
  # How long capture offers remain valid (seconds)
  offer-expiry-timeout: 60
  
  # Whether to persist prisoner state across restarts
  enable-persistence: true
  
  # Future: Movement restriction radius (Phase 3)
  # jail-radius: 50

# Permission Settings
permissions:
  # Whether to enforce permissions (false = OP-only mode)
  enabled: true

# Future: Team settings (Phase 2)
# teams:
#   gg:
#     color: BLUE
#   goons:
#     color: RED
```

#### Implementation
- Create default config in `src/main/resources/config.yml`
- Load in `GGvGPlugin.onEnable()` using `saveDefaultConfig()`
- Add config accessor methods in `GGvGPlugin`
- Pass config values to `PrisonerManager` constructor

### 4. Permission System

#### Permission Nodes
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
    description: Full administrative access
    default: op
    children:
      ggvgoons.warprisoner.capture: true
      ggvgoons.warprisoner.free: true
      ggvgoons.warprisoner.execute: true
      ggvgoons.warprisoner.list: true
```

#### Implementation
- Add permission nodes to `plugin.yml`
- Add permission checks in each command executor:
  ```kotlin
  if (!sender.hasPermission("ggvgoons.warprisoner.capture")) {
      sender.sendMessage("${ChatColor.RED}You don't have permission to use this command.")
      return true
  }
  ```
- Make permission enforcement configurable via `config.yml`

### 5. List Prisoners Command

#### Command Design
- **Command**: `/listprisoners`
- **Permission**: `ggvgoons.warprisoner.list`
- **Output Format**:
  ```
  === Active War Prisoners (3) ===
  • PlayerName (held by CaptorName)
  • AnotherPlayer (held by SomeCaptor)
  • ThirdPlayer (held by AnotherCaptor)
  ```

#### Implementation
- New class: `ListPrisonersCommand` in `WarPrisonerModule.kt`
- Add to `plugin.yml` commands section
- Register in `WarPrisonerModule.register()`
- Query `PrisonerManager.prisoners` and format output

## Implementation Order

### Step 1: Configuration System
1. Create `src/main/resources/config.yml` with default values
2. Add config loading in `GGvGPlugin.onEnable()`
3. Add config accessor methods

### Step 2: Permission System
1. Add permission nodes to `plugin.yml`
2. Implement permission checks in all command executors
3. Add config option to enable/disable permission enforcement

### Step 3: Offer Expiry
1. Modify `PrisonerManager` to track expiry tasks
2. Update `createOffer()` to schedule expiry
3. Update `consumeOffer()` to cancel scheduled tasks
4. Add expiry notification messages

### Step 4: YAML Persistence
1. Create `persistence/PrisonerPersistence.kt`
2. Implement save/load methods using Bukkit YAML API
3. Add persistence hooks in `GGvGPlugin`
4. Add `getPrisoners()` method to `PrisonerManager`

### Step 5: List Prisoners Command
1. Create `ListPrisonersCommand` executor
2. Add command to `plugin.yml`
3. Register in `WarPrisonerModule`
4. Implement formatted output

### Step 6: Testing & Documentation
1. Test all features on test server
2. Verify persistence across restarts
3. Test offer expiry timing
4. Verify permission system works
5. Update README to mark Phase 1 items as complete

## Technical Considerations

### Thread Safety
- Bukkit is single-threaded for most operations
- `BukkitRunnable` tasks run on main thread
- No additional synchronization needed for current implementation

### Backward Compatibility
- First version with persistence - no migration needed
- Config will auto-generate with defaults
- Existing commands remain unchanged

### Error Handling
- Graceful degradation if `prisoners.yml` is corrupted
- Log warnings but don't crash on load failure
- Validate config values with sensible defaults

### Performance
- YAML I/O only on startup/shutdown (minimal impact)
- Offer expiry tasks are lightweight
- Map lookups remain O(1)

## File Structure After Implementation

```
src/main/
├── kotlin/com/tyler/ggvsgoons/
│   ├── GGvGPlugin.kt (modified - add config & persistence)
│   ├── commands/
│   │   ├── PrisonerManager.kt (modified - add expiry & persistence support)
│   │   └── WarPrisonerModule.kt (modified - add permissions & list command)
│   └── persistence/
│       └── PrisonerPersistence.kt (new)
└── resources/
    ├── plugin.yml (modified - add permissions & list command)
    └── config.yml (new)
```

## Success Criteria

- ✅ Prisoner state persists across server restarts
- ✅ Capture offers expire after configured timeout
- ✅ All commands respect permission nodes
- ✅ Config file generates with sensible defaults
- ✅ `/listprisoners` shows all active prisoners
- ✅ No breaking changes to existing functionality
- ✅ README Phase 1 checklist items marked complete

## Future Enhancements (Phase 2+)

These are NOT part of Phase 1 but should be considered in design:
- Team validation for captures (Phase 2)
- Movement restrictions for prisoners (Phase 3)
- Inventory restrictions (Phase 3)
- Prisoner escape mechanics (Phase 3)
- Combat event integration (Phase 4)

## Mermaid Diagram: Enhanced Architecture

```mermaid
graph TB
    A[GGvGPlugin] --> B[Config System]
    A --> C[Module System]
    A --> D[Persistence Layer]
    
    B --> B1[config.yml]
    B1 --> B2[Offer Timeout]
    B1 --> B3[Permission Mode]
    B1 --> B4[Persistence Toggle]
    
    C --> E[WarPrisonerModule]
    E --> F[PrisonerManager]
    
    F --> G[In-Memory State]
    G --> G1[Pending Offers]
    G --> G2[Active Prisoners]
    G --> G3[Expiry Tasks]
    
    F --> H[Offer Expiry System]
    H --> H1[BukkitRunnable]
    H1 --> H2[Auto-cleanup]
    
    D --> I[PrisonerPersistence]
    I --> J[prisoners.yml]
    J --> J1[Save on Disable]
    J --> J2[Load on Enable]
    
    E --> K[Commands]
    K --> K1[/warprisoner]
    K --> K2[/freeprisoner]
    K --> K3[/executeprisoner]
    K --> K4[/listprisoners NEW]
    
    K1 --> L[Permission Checks]
    K2 --> L
    K3 --> L
    K4 --> L
    
    style K4 fill:#90EE90
    style I fill:#90EE90
    style H fill:#90EE90
    style B fill:#90EE90
    style L fill:#90EE90
```

## Estimated Complexity

- **Configuration System**: Low complexity
- **Permission System**: Low complexity
- **Offer Expiry**: Medium complexity (async task management)
- **YAML Persistence**: Medium complexity (serialization logic)
- **List Command**: Low complexity

Total: Medium complexity project, well-scoped for Phase 1.
