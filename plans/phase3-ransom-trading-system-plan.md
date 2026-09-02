# Phase 3: Ransom/Trading System Plan

## Overview
Implement a prisoner ransom and item trading system where both captors and prisoners can initiate trades to negotiate release. The system uses a chest GUI interface with green/red confirmation panes.

## Architecture

### New Module: RansomModule
Location: `src/main/kotlin/com/tyler/ggvsgoons/ransom/`

**Components:**
- [`RansomModule.kt`](src/main/kotlin/com/tyler/ggvsgoons/ransom/RansomModule.kt) - Module registration and initialization
- [`RansomManager.kt`](src/main/kotlin/com/tyler/ggvsgoons/ransom/RansomManager.kt) - Core trading logic and state management
- [`RansomCommand.kt`](src/main/kotlin/com/tyler/ggvsgoons/ransom/RansomCommand.kt) - Command executor for `/ransom` commands
- [`TradeGUI.kt`](src/main/kotlin/com/tyler/ggvsgoons/ransom/TradeGUI.kt) - Chest inventory GUI handler
- [`RansomPersistence.kt`](src/main/kotlin/com/tyler/ggvsgoons/persistence/RansomPersistence.kt) - Save/load pending trades

### Data Structures

```kotlin
data class RansomTrade(
    val tradeId: UUID,
    val initiatorId: UUID,      // Who started the trade
    val responderId: UUID,       // Who needs to respond
    val prisonerId: UUID,        // The prisoner being traded for
    val initiatorItems: List<ItemStack>,
    val responderItems: List<ItemStack>,
    val initiatorConfirmed: Boolean = false,
    val responderConfirmed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class TradeRestrictions(
    val blacklistedMaterials: Set<Material>,
    val whitelistedMaterials: Set<Material>?,  // null = all allowed
    val maxItemsPerSide: Int,
    val requireMinimumValue: Boolean,
    val minimumValueInDiamonds: Int
)
```

## Features

### 1. Trade Initiation
**Commands:**
- `/ransom offer <player>` - Initiate a ransom trade (works for both captor and prisoner)
- `/ransom cancel` - Cancel your pending trade offer
- `/ransom list` - View all active ransom trades

**Validation:**
- Only works between captor and their prisoner
- Cannot have multiple active trades for the same prisoner
- Respects configurable item restrictions

### 2. Trade GUI Interface

**Layout (54-slot double chest):**
```
Row 1-2: Initiator's items (18 slots)
Row 3: Separator (gray glass panes)
Row 4-5: Responder's items (18 slots)
Row 6: Control panel
  - Slot 47: Red concrete (Cancel/Decline)
  - Slot 49: Yellow concrete (Waiting/Not Ready)
  - Slot 51: Green concrete (Confirm/Accept)
```

**Interaction Flow:**
1. Initiator uses `/ransom offer <player>` - opens GUI for both players
2. Both players place items in their respective slots
3. When ready, click green pane to confirm
4. Yellow pane shows when one side is confirmed, waiting for other
5. When both confirm, trade executes and prisoner is released
6. Red pane cancels the trade at any time

### 3. Item Restrictions (Configurable)

**Config Options:**
```yaml
ransom:
  enabled: true
  
  # Item restrictions
  restrictions:
    # Blacklist specific items from trades
    blacklisted-items:
      - BEDROCK
      - COMMAND_BLOCK
      - BARRIER
    
    # Whitelist mode (if enabled, only these items allowed)
    whitelist-enabled: false
    whitelisted-items:
      - DIAMOND
      - EMERALD
      - GOLD_INGOT
    
    # Maximum items per side
    max-items-per-side: 18
    
    # Require minimum value
    require-minimum-value: false
    minimum-value-diamonds: 5
  
  # Trade timeout (seconds, 0 = no timeout)
  trade-timeout: 300
  
  # Auto-release prisoner on successful trade
  auto-release: true
  
  # Persistence
  enable-persistence: true
```

### 4. Trade Execution

**On Successful Trade:**
1. Validate both sides confirmed
2. Check both players have inventory space
3. Transfer items between players
4. If `auto-release: true`, automatically free the prisoner
5. Log the trade for audit purposes
6. Send confirmation messages to both players
7. Broadcast to team (optional config)

**Failure Cases:**
- Insufficient inventory space → Cancel trade, return items
- Player disconnects → Save trade state, resume on reconnect
- Timeout expires → Cancel trade, return items
- Items become invalid → Cancel trade, return items

### 5. Persistence

**File:** `plugins/GGvGoons/ransom_trades.yml`

**Structure:**
```yaml
trades:
  uuid-1:
    initiator: player-uuid
    responder: player-uuid
    prisoner: prisoner-uuid
    initiator-items: [serialized items]
    responder-items: [serialized items]
    initiator-confirmed: false
    responder-confirmed: false
    created-at: timestamp
```

## Integration Points

### With WarPrisonerModule
- Validate prisoner relationship before allowing trades
- Auto-release prisoner on successful trade (if configured)
- Prevent prisoner from being freed/executed during active trade

### With TeamsModule
- Optional: Broadcast successful ransoms to team chat
- Track ransom statistics per team

### With AdminConfigModule (Phase 6)
- Allow admins to view/cancel any trade
- Configure restrictions in real-time
- Enable/disable ransom system without restart

## Commands & Permissions

### Commands
```yaml
ransom:
  description: Ransom and trading commands
  usage: /<command> <offer|cancel|list|accept|decline> [player]
  permission: ggvgoons.ransom.use
```

**Subcommands:**
- `/ransom offer <player>` - Start a ransom trade
- `/ransom cancel` - Cancel your pending trade
- `/ransom list` - List all active trades
- `/ransom view <player>` - View specific trade details (admin)
- `/ransom forcecancel <player>` - Force cancel a trade (admin)

### Permissions
```yaml
permissions:
  ggvgoons.ransom.use:
    description: Allows using ransom trading system
    default: true
  
  ggvgoons.ransom.bypass-restrictions:
    description: Bypass item restrictions in trades
    default: op
  
  ggvgoons.ransom.admin:
    description: Admin commands for ransom system
    default: op
    children:
      ggvgoons.ransom.view: true
      ggvgoons.ransom.forcecancel: true
```

## Event Listeners

### Required Events
- `InventoryClickEvent` - Handle GUI interactions
- `InventoryCloseEvent` - Handle GUI closure (save state)
- `PlayerQuitEvent` - Save trade state on disconnect
- `PlayerJoinEvent` - Restore trade GUI if pending

## Implementation Steps

1. Create `RansomModule` class implementing `GGvGModule`
2. Implement `RansomManager` with trade state management
3. Create `TradeGUI` class for chest inventory interface
4. Implement `RansomCommand` with all subcommands
5. Add event listeners for inventory interactions
6. Implement `RansomPersistence` for save/load
7. Add configuration options to `config.yml`
8. Update `plugin.yml` with new commands and permissions
9. Register module in `GGvGPlugin.onEnable()`
10. Add integration hooks with `WarPrisonerModule`
11. Write unit tests for trade logic
12. Test GUI interactions thoroughly
13. Update README with ransom system documentation

## Testing Checklist

- [ ] Trade initiation between captor and prisoner
- [ ] Trade initiation from prisoner to captor
- [ ] Item placement and removal in GUI
- [ ] Confirmation system (both sides must confirm)
- [ ] Trade cancellation
- [ ] Trade timeout expiry
- [ ] Item restriction enforcement (blacklist/whitelist)
- [ ] Inventory space validation
- [ ] Player disconnect during trade
- [ ] Prisoner auto-release on successful trade
- [ ] Persistence across server restarts
- [ ] Permission enforcement
- [ ] Admin commands (view, force cancel)
- [ ] Integration with prisoner system

## UI/UX Considerations

### Visual Feedback
- Use colored glass panes for clear separation
- Item tooltips show restrictions
- Chat messages for all state changes
- Sound effects for confirmations/cancellations

### User Experience
- Clear instructions in GUI title
- Hover tooltips on control buttons
- Confirmation messages before execution
- Undo capability before final confirmation

## Security Considerations

- Validate all item transfers
- Prevent item duplication exploits
- Rate limit trade initiations
- Log all trades for audit
- Prevent trades during combat (optional)
- Validate prisoner relationship hasn't changed

## Performance Considerations

- Limit active trades per player (1 at a time)
- Clean up expired trades regularly
- Efficient GUI updates (only when needed)
- Async persistence operations
- Cache item restrictions in memory
