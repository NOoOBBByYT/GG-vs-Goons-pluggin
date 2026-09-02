package com.tyler.ggvsgoons.factions

import com.tyler.ggvsgoons.GGvGPlugin
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.entity.Player

/**
 * Handles events related to factions
 */
class FactionEventListener(
    private val plugin: GGvGPlugin,
    private val manager: FactionManager
) : Listener {
    
    /**
     * Handle player join - restore faction data
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        val faction = manager.getFactionByPlayer(player.uniqueId)
        
        if (faction != null) {
            // Optionally send a welcome message or update scoreboard
            // This can be expanded later
        }
    }
    
    /**
     * Handle player quit - save faction data
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        // Faction data is saved on plugin disable
        // Individual player data doesn't need special handling on quit
    }
    
    /**
     * Handle damage events - cancel damage during faction home teleport warmup
     * TODO: Implement teleport warmup system
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onEntityDamage(event: EntityDamageByEntityEvent) {
        val entity = event.entity
        if (entity !is Player) return
        
        // TODO: Check if player is in teleport warmup and cancel if so
        // This will be implemented when we add the teleport warmup/cooldown system
    }
}
