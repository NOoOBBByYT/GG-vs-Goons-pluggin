package com.tyler.ggvsgoons.admin

import com.tyler.ggvsgoons.GGvGPlugin
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages player moderation actions
 */
class ModerationManager(
    private val plugin: GGvGPlugin,
    private val auditLogger: AuditLogger
) {
    
    private val frozenPlayers = ConcurrentHashMap<UUID, FrozenPlayer>()
    private val commandRestrictions = ConcurrentHashMap<String, CommandRestriction>()
    
    /**
     * Freeze a player (prevent movement and interaction)
     */
    fun freezePlayer(playerId: UUID, adminId: UUID, reason: String): Result<Unit> {
        if (frozenPlayers.containsKey(playerId)) {
            return Result.failure(IllegalStateException("Player is already frozen"))
        }
        
        val frozen = FrozenPlayer(
            playerId = playerId,
            frozenBy = adminId,
            reason = reason
        )
        
        frozenPlayers[playerId] = frozen
        
        // Notify player
        Bukkit.getPlayer(playerId)?.sendMessage("§c§lYou have been frozen by an administrator")
        Bukkit.getPlayer(playerId)?.sendMessage("§7Reason: $reason")
        
        return Result.success(Unit)
    }
    
    /**
     * Unfreeze a player
     */
    fun unfreezePlayer(playerId: UUID): Result<Unit> {
        val frozen = frozenPlayers.remove(playerId)
            ?: return Result.failure(IllegalArgumentException("Player is not frozen"))
        
        // Notify player
        Bukkit.getPlayer(playerId)?.sendMessage("§aYou have been unfrozen")
        
        return Result.success(Unit)
    }
    
    /**
     * Check if a player is frozen
     */
    fun isPlayerFrozen(playerId: UUID): Boolean {
        return frozenPlayers.containsKey(playerId)
    }
    
    /**
     * Get frozen player info
     */
    fun getFrozenPlayer(playerId: UUID): FrozenPlayer? {
        return frozenPlayers[playerId]
    }
    
    /**
     * Get all frozen players
     */
    fun getAllFrozenPlayers(): List<FrozenPlayer> {
        return frozenPlayers.values.toList()
    }
    
    /**
     * Force free a prisoner
     */
    fun forceFreePrisoner(playerId: UUID, adminId: UUID, adminName: String): Result<Unit> {
        // Check if player is a prisoner
        val isPrisoner = plugin.warPrisoner.manager.isPrisoner(playerId)
        if (!isPrisoner) {
            return Result.failure(IllegalArgumentException("Player is not a prisoner"))
        }
        
        // Free the prisoner
        val success = plugin.warPrisoner.manager.releasePrisoner(playerId)
        if (success) {
            
            // Log the action
            val action = ModerationAction(
                adminId = adminId,
                targetId = playerId,
                actionType = ModActionType.FORCE_FREE_PRISONER,
                reason = "Admin force-freed prisoner"
            )
            val playerName = Bukkit.getOfflinePlayer(playerId).name ?: playerId.toString()
            auditLogger.logModAction(action, adminName, playerName, true)
            
            return Result.success(Unit)
        }
        
        return Result.failure(IllegalStateException("Could not find captor"))
    }
    
    /**
     * Reset all cooldowns for a player
     */
    fun resetCooldowns(playerId: UUID, adminId: UUID, adminName: String): Result<Unit> {
        // This would reset cooldowns in various systems
        // For now, we'll just log the action
        
        val action = ModerationAction(
            adminId = adminId,
            targetId = playerId,
            actionType = ModActionType.RESET_COOLDOWN,
            reason = "Admin reset all cooldowns"
        )
        val playerName = Bukkit.getOfflinePlayer(playerId).name ?: playerId.toString()
        auditLogger.logModAction(action, adminName, playerName, true)
        
        return Result.success(Unit)
    }
    
    /**
     * Kick a player from their team
     */
    fun kickFromTeam(playerId: UUID, adminId: UUID, adminName: String): Result<Unit> {
        val team = plugin.teams.manager.getPlayerTeam(playerId)
            ?: return Result.failure(IllegalArgumentException("Player is not on a team"))
        
        // Remove player from team by setting to empty string (removes from map)
        plugin.teams.manager.setPlayerTeamSilently(playerId, "")
        
        // Log the action
        val action = ModerationAction(
            adminId = adminId,
            targetId = playerId,
            actionType = ModActionType.KICK_FROM_TEAM,
            reason = "Admin kicked from team: ${team.id}"
        )
        val playerName = Bukkit.getOfflinePlayer(playerId).name ?: playerId.toString()
        auditLogger.logModAction(action, adminName, playerName, true)
        
        // Notify player
        Bukkit.getPlayer(playerId)?.sendMessage("§cYou have been removed from your team by an administrator")
        
        return Result.success(Unit)
    }
    
    /**
     * Kick a player from their faction
     */
    fun kickFromFaction(playerId: UUID, adminId: UUID, adminName: String): Result<Unit> {
        // Check if factions module exists
        try {
            val factionsModuleClass = Class.forName("com.tyler.ggvsgoons.factions.FactionsModule")
            val factionsModule = plugin.javaClass.getDeclaredField("factions").get(plugin)
            val manager = factionsModuleClass.getDeclaredField("manager").get(factionsModule)
            
            // Use reflection to get faction and kick player
            val getFactionMethod = manager.javaClass.getMethod("getFactionByPlayer", UUID::class.java)
            val faction = getFactionMethod.invoke(manager, playerId)
            
            if (faction == null) {
                return Result.failure(IllegalArgumentException("Player is not in a faction"))
            }
            
            // Remove player from faction
            val membersField = faction.javaClass.getDeclaredField("members")
            membersField.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val members = membersField.get(faction) as MutableSet<UUID>
            members.remove(playerId)
            
            // Log the action
            val action = ModerationAction(
                adminId = adminId,
                targetId = playerId,
                actionType = ModActionType.KICK_FROM_FACTION,
                reason = "Admin kicked from faction"
            )
            val playerName = Bukkit.getOfflinePlayer(playerId).name ?: playerId.toString()
            auditLogger.logModAction(action, adminName, playerName, true)
            
            // Notify player
            Bukkit.getPlayer(playerId)?.sendMessage("§cYou have been removed from your faction by an administrator")
            
            return Result.success(Unit)
        } catch (e: ClassNotFoundException) {
            return Result.failure(IllegalStateException("Factions module is not loaded"))
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
    
    /**
     * Get player information
     */
    fun getPlayerInfo(playerId: UUID): PlayerInfo {
        val player = Bukkit.getOfflinePlayer(playerId)
        val team = plugin.teams.manager.getPlayerTeam(playerId)
        val isPrisoner = plugin.warPrisoner.manager.isPrisoner(playerId)
        val isFrozen = isPlayerFrozen(playerId)
        
        // Try to get faction info
        var factionId: String? = null
        try {
            val factionsModuleClass = Class.forName("com.tyler.ggvsgoons.factions.FactionsModule")
            val factionsModule = plugin.javaClass.getDeclaredField("factions").get(plugin)
            val manager = factionsModuleClass.getDeclaredField("manager").get(factionsModule)
            val getFactionMethod = manager.javaClass.getMethod("getFactionByPlayer", UUID::class.java)
            val faction = getFactionMethod.invoke(manager, playerId)
            if (faction != null) {
                val idField = faction.javaClass.getDeclaredField("id")
                idField.isAccessible = true
                factionId = idField.get(faction) as String
            }
        } catch (e: Exception) {
            // Factions not loaded or player not in faction
        }
        
        val onlineSince = if (player.isOnline) {
            player.player?.let { System.currentTimeMillis() - (it.firstPlayed) }
        } else null
        
        return PlayerInfo(
            playerId = playerId,
            playerName = player.name ?: "Unknown",
            teamId = team?.id,
            factionId = factionId,
            isPrisoner = isPrisoner,
            isFrozen = isFrozen,
            onlineSince = onlineSince,
            totalPlaytime = player.firstPlayed.let { System.currentTimeMillis() - it },
            kills = 0, // Would need to track this
            deaths = 0, // Would need to track this
            prisonersCaptured = 0 // Would need to track this
        )
    }
    
    /**
     * Disable a command globally
     */
    fun disableCommand(command: String, reason: String? = null): Result<Unit> {
        val restriction = commandRestrictions.getOrPut(command) {
            CommandRestriction(command = command, disabled = false)
        }
        
        commandRestrictions[command] = restriction.copy(disabled = true, reason = reason)
        return Result.success(Unit)
    }
    
    /**
     * Enable a command globally
     */
    fun enableCommand(command: String): Result<Unit> {
        val restriction = commandRestrictions[command]
            ?: return Result.failure(IllegalArgumentException("Command is not restricted"))
        
        commandRestrictions[command] = restriction.copy(disabled = false)
        return Result.success(Unit)
    }
    
    /**
     * Disable a command for a specific player
     */
    fun disableCommandForPlayer(command: String, playerId: UUID, reason: String? = null): Result<Unit> {
        val restriction = commandRestrictions.getOrPut(command) {
            CommandRestriction(command = command, disabled = false)
        }
        
        restriction.disabledFor.add(playerId)
        commandRestrictions[command] = restriction.copy(reason = reason)
        return Result.success(Unit)
    }
    
    /**
     * Enable a command for a specific player
     */
    fun enableCommandForPlayer(command: String, playerId: UUID): Result<Unit> {
        val restriction = commandRestrictions[command]
            ?: return Result.failure(IllegalArgumentException("Command is not restricted"))
        
        restriction.disabledFor.remove(playerId)
        return Result.success(Unit)
    }
    
    /**
     * Check if a command is disabled for a player
     */
    fun isCommandDisabled(command: String, playerId: UUID): Boolean {
        val restriction = commandRestrictions[command] ?: return false
        return restriction.disabled || restriction.disabledFor.contains(playerId)
    }
    
    /**
     * Get command restriction
     */
    fun getCommandRestriction(command: String): CommandRestriction? {
        return commandRestrictions[command]
    }
    
    /**
     * Get all command restrictions
     */
    fun getAllCommandRestrictions(): List<CommandRestriction> {
        return commandRestrictions.values.toList()
    }
}
