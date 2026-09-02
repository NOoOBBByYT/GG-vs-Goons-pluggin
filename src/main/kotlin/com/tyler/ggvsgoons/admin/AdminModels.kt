package com.tyler.ggvsgoons.admin

import java.util.UUID

/**
 * Represents a configuration change made by an admin
 */
data class ConfigChange(
    val adminId: UUID,
    val adminName: String,
    val path: String,
    val oldValue: Any?,
    val newValue: Any?,
    val timestamp: Long = System.currentTimeMillis(),
    val reason: String? = null
)

/**
 * Represents a moderation action taken by an admin
 */
data class ModerationAction(
    val actionId: UUID = UUID.randomUUID(),
    val adminId: UUID,
    val targetId: UUID?,
    val actionType: ModActionType,
    val reason: String,
    val duration: Long? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Types of moderation actions
 */
enum class ModActionType(val displayName: String) {
    DISABLE_COMMAND("Disable Command"),
    ENABLE_COMMAND("Enable Command"),
    FREEZE_PLAYER("Freeze Player"),
    UNFREEZE_PLAYER("Unfreeze Player"),
    FORCE_FREE_PRISONER("Force Free Prisoner"),
    CANCEL_TRADE("Cancel Trade"),
    KICK_FROM_TEAM("Kick from Team"),
    KICK_FROM_FACTION("Kick from Faction"),
    RESET_COOLDOWN("Reset Cooldown"),
    GRANT_PERMISSION("Grant Permission"),
    REVOKE_PERMISSION("Revoke Permission"),
    MODULE_ENABLE("Enable Module"),
    MODULE_DISABLE("Disable Module"),
    CONFIG_CHANGE("Configuration Change"),
    BACKUP_CREATE("Create Backup"),
    BACKUP_RESTORE("Restore Backup")
}

/**
 * Represents a command restriction
 */
data class CommandRestriction(
    val command: String,
    val disabled: Boolean,
    val disabledFor: MutableSet<UUID> = mutableSetOf(),
    val reason: String? = null,
    val disabledAt: Long = System.currentTimeMillis()
)

/**
 * Represents a frozen player
 */
data class FrozenPlayer(
    val playerId: UUID,
    val frozenBy: UUID,
    val reason: String,
    val frozenAt: Long = System.currentTimeMillis()
)

/**
 * Represents information about a module
 */
data class ModuleInfo(
    val name: String,
    val enabled: Boolean,
    val description: String,
    val dependencies: List<String> = emptyList(),
    val version: String = "1.0.0"
)

/**
 * Represents a backup
 */
data class BackupInfo(
    val name: String,
    val timestamp: Long,
    val size: Long,
    val createdBy: UUID?,
    val description: String? = null
)

/**
 * Represents an audit log entry
 */
data class AuditEntry(
    val id: UUID = UUID.randomUUID(),
    val timestamp: Long = System.currentTimeMillis(),
    val adminId: UUID,
    val adminName: String,
    val actionType: ModActionType,
    val target: String? = null,
    val details: String,
    val success: Boolean = true
) {
    /**
     * Format the entry for display
     */
    fun format(): String {
        val timeStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date(timestamp))
        val status = if (success) "SUCCESS" else "FAILED"
        val targetStr = target?.let { " | Target: $it" } ?: ""
        return "[$timeStr] [$status] $adminName - ${actionType.displayName}$targetStr | $details"
    }
}

/**
 * Represents player information for admin viewing
 */
data class PlayerInfo(
    val playerId: UUID,
    val playerName: String,
    val teamId: String?,
    val factionId: String?,
    val isPrisoner: Boolean,
    val isFrozen: Boolean,
    val onlineSince: Long?,
    val totalPlaytime: Long = 0,
    val kills: Int = 0,
    val deaths: Int = 0,
    val prisonersCaptured: Int = 0
)

/**
 * Represents server statistics
 */
data class ServerStats(
    val onlinePlayers: Int,
    val maxPlayers: Int,
    val tps: Double,
    val memoryUsed: Long,
    val memoryMax: Long,
    val uptime: Long,
    val activePrisoners: Int,
    val activeTrades: Int,
    val totalFactions: Int
)

/**
 * Configuration validation result
 */
sealed class ValidationResult {
    data class Valid(val value: Any) : ValidationResult()
    data class Invalid(val reason: String) : ValidationResult()
}

/**
 * Configuration value type
 */
enum class ConfigValueType {
    STRING,
    INTEGER,
    LONG,
    DOUBLE,
    BOOLEAN,
    STRING_LIST,
    UNKNOWN
}
