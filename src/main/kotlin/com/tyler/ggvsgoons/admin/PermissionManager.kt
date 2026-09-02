package com.tyler.ggvsgoons.admin

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.permissions.PermissionAttachment
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages runtime permissions for players
 * Integrates with LuckPerms if available, otherwise uses internal system
 */
class PermissionManager(
    private val plugin: org.bukkit.plugin.Plugin,
    private val useLuckPerms: Boolean = true
) {
    
    private val permissionAttachments = ConcurrentHashMap<UUID, PermissionAttachment>()
    private val temporaryPermissions = ConcurrentHashMap<UUID, MutableMap<String, Long>>() // permission -> expiry time
    
    private var luckPermsAvailable = false
    
    init {
        // Check if LuckPerms is available
        if (useLuckPerms) {
            luckPermsAvailable = Bukkit.getPluginManager().getPlugin("LuckPerms") != null
            if (luckPermsAvailable) {
                plugin.logger.info("LuckPerms detected, using LuckPerms for permission management")
            } else {
                plugin.logger.info("LuckPerms not found, using internal permission system")
            }
        }
    }
    
    /**
     * Grant a permission to a player
     */
    fun grantPermission(playerId: UUID, permission: String, temporary: Boolean = false, duration: Long? = null): Result<Unit> {
        val player = Bukkit.getPlayer(playerId)
        
        if (luckPermsAvailable) {
            // Use LuckPerms API
            return grantPermissionLuckPerms(playerId, permission, temporary, duration)
        } else {
            // Use internal system
            if (player == null) {
                return Result.failure(IllegalArgumentException("Player must be online for internal permission system"))
            }
            
            val attachment = permissionAttachments.getOrPut(playerId) {
                player.addAttachment(plugin)
            }
            
            attachment.setPermission(permission, true)
            
            // Handle temporary permissions
            if (temporary && duration != null) {
                val expiryTime = System.currentTimeMillis() + (duration * 1000)
                temporaryPermissions.getOrPut(playerId) { mutableMapOf() }[permission] = expiryTime
                
                // Schedule removal
                Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                    revokePermission(playerId, permission)
                }, duration * 20) // Convert seconds to ticks
            }
            
            return Result.success(Unit)
        }
    }
    
    /**
     * Revoke a permission from a player
     */
    fun revokePermission(playerId: UUID, permission: String): Result<Unit> {
        if (luckPermsAvailable) {
            // Use LuckPerms API
            return revokePermissionLuckPerms(playerId, permission)
        } else {
            // Use internal system
            val attachment = permissionAttachments[playerId]
                ?: return Result.failure(IllegalArgumentException("Player has no permission attachments"))
            
            attachment.unsetPermission(permission)
            
            // Remove from temporary permissions
            temporaryPermissions[playerId]?.remove(permission)
            
            return Result.success(Unit)
        }
    }
    
    /**
     * Check if a player has a permission
     */
    fun checkPermission(playerId: UUID, permission: String): Boolean {
        val player = Bukkit.getPlayer(playerId) ?: Bukkit.getOfflinePlayer(playerId)
        
        if (luckPermsAvailable) {
            // Use LuckPerms API
            return checkPermissionLuckPerms(playerId, permission)
        } else {
            // Use Bukkit's permission system
            return player.player?.hasPermission(permission) ?: false
        }
    }
    
    /**
     * List all permissions for a player
     */
    fun listPermissions(playerId: UUID): Set<String> {
        if (luckPermsAvailable) {
            // Use LuckPerms API
            return listPermissionsLuckPerms(playerId)
        } else {
            // Use internal system
            val attachment = permissionAttachments[playerId] ?: return emptySet()
            return attachment.permissions.keys
        }
    }
    
    /**
     * Assign a permission group to a player
     */
    fun assignGroup(playerId: UUID, group: String): Result<Unit> {
        if (luckPermsAvailable) {
            // Use LuckPerms API
            return assignGroupLuckPerms(playerId, group)
        } else {
            // Internal system doesn't support groups
            return Result.failure(UnsupportedOperationException("Groups are only supported with LuckPerms"))
        }
    }
    
    /**
     * Clean up expired temporary permissions
     */
    fun cleanupExpiredPermissions() {
        val currentTime = System.currentTimeMillis()
        
        temporaryPermissions.forEach { (playerId, permissions) ->
            val expired = permissions.filter { (_, expiryTime) -> expiryTime <= currentTime }
            expired.keys.forEach { permission ->
                revokePermission(playerId, permission)
            }
        }
    }
    
    /**
     * Remove all permission attachments for a player (on logout)
     */
    fun removePlayerAttachments(playerId: UUID) {
        permissionAttachments.remove(playerId)?.remove()
        temporaryPermissions.remove(playerId)
    }
    
    // LuckPerms integration methods (stubs for now)
    
    private fun grantPermissionLuckPerms(playerId: UUID, permission: String, temporary: Boolean, duration: Long?): Result<Unit> {
        // TODO: Implement LuckPerms API integration
        // For now, return success
        plugin.logger.info("Would grant permission via LuckPerms: $permission to $playerId")
        return Result.success(Unit)
    }
    
    private fun revokePermissionLuckPerms(playerId: UUID, permission: String): Result<Unit> {
        // TODO: Implement LuckPerms API integration
        plugin.logger.info("Would revoke permission via LuckPerms: $permission from $playerId")
        return Result.success(Unit)
    }
    
    private fun checkPermissionLuckPerms(playerId: UUID, permission: String): Boolean {
        // TODO: Implement LuckPerms API integration
        // For now, fall back to Bukkit
        val player = Bukkit.getPlayer(playerId)
        return player?.hasPermission(permission) ?: false
    }
    
    private fun listPermissionsLuckPerms(playerId: UUID): Set<String> {
        // TODO: Implement LuckPerms API integration
        return emptySet()
    }
    
    private fun assignGroupLuckPerms(playerId: UUID, group: String): Result<Unit> {
        // TODO: Implement LuckPerms API integration
        plugin.logger.info("Would assign group via LuckPerms: $group to $playerId")
        return Result.success(Unit)
    }
}
