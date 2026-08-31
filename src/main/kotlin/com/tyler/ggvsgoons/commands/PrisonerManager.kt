package com.tyler.ggvsgoons.commands

import com.tyler.ggvsgoons.GGvGPlugin
import net.md_5.bungee.api.ChatColor
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import java.util.UUID

/**
 * captorId -> targetId, while an offer is awaiting accept/decline
 */
data class PendingOffer(val captorId: UUID, val targetId: UUID, val issuedAtMillis: Long = System.currentTimeMillis())

/**
 * prisonerId -> captorId, plus the gamemode to restore them to on release
 */
data class Prisoner(val captorId: UUID, val previousGameMode: GameMode)

class PrisonerManager(private val plugin: GGvGPlugin, private val offerExpirySeconds: Int) {

    private val pendingOffers = mutableMapOf<UUID, PendingOffer>() // keyed by targetId
    private val prisoners = mutableMapOf<UUID, Prisoner>()          // keyed by prisonerId
    private val expiryTasks = mutableMapOf<UUID, BukkitTask>()     // keyed by targetId

    fun activePrisonerCount() = prisoners.size

    fun isPrisoner(playerId: UUID) = prisoners.containsKey(playerId)

    fun captorOf(playerId: UUID): UUID? = prisoners[playerId]?.captorId

    fun hasPendingOffer(targetId: UUID) = pendingOffers.containsKey(targetId)

    /**
     * Get all active prisoners (for persistence and listing)
     */
    fun getPrisoners(): Map<UUID, Prisoner> = prisoners.toMap()

    /**
     * Load prisoners from persistence (called on plugin enable)
     */
    fun loadPrisoners(loadedPrisoners: Map<UUID, Prisoner>) {
        prisoners.clear()
        prisoners.putAll(loadedPrisoners)
        
        // Restore Adventure mode for any prisoners who are online
        loadedPrisoners.keys.forEach { prisonerId ->
            plugin.server.getPlayer(prisonerId)?.let { player ->
                player.gameMode = GameMode.ADVENTURE
                player.sendMessage("${ChatColor.GOLD}You are still a war prisoner from before the server restart.")
            }
        }
    }

    fun createOffer(captor: Player, target: Player): Boolean {
        if (isPrisoner(target.uniqueId) || hasPendingOffer(target.uniqueId)) return false
        pendingOffers[target.uniqueId] = PendingOffer(captor.uniqueId, target.uniqueId)
        
        // Schedule expiry task if timeout is configured
        if (offerExpirySeconds > 0) {
            val task = plugin.server.scheduler.runTaskLater(plugin, Runnable {
                expireOffer(target.uniqueId)
            }, (offerExpirySeconds * 20L)) // Convert seconds to ticks
            
            expiryTasks[target.uniqueId] = task
        }
        
        return true
    }

    /** Returns the captorId this offer came from, or null if it wasn't valid / expired */
    fun consumeOffer(targetId: UUID, expectedCaptorId: UUID): UUID? {
        val offer = pendingOffers[targetId] ?: return null
        if (offer.captorId != expectedCaptorId) return null
        pendingOffers.remove(targetId)
        
        // Cancel the expiry task since offer was consumed
        expiryTasks.remove(targetId)?.cancel()
        
        return offer.captorId
    }

    fun clearOffer(targetId: UUID) {
        pendingOffers.remove(targetId)
        expiryTasks.remove(targetId)?.cancel()
    }

    /**
     * Called when an offer expires due to timeout
     */
    private fun expireOffer(targetId: UUID) {
        val offer = pendingOffers.remove(targetId) ?: return
        expiryTasks.remove(targetId)
        
        // Notify both players
        plugin.server.getPlayer(targetId)?.let { target ->
            plugin.server.getPlayer(offer.captorId)?.let { captor ->
                target.sendMessage("${ChatColor.YELLOW}The capture offer from ${captor.name} has expired.")
                captor.sendMessage("${ChatColor.YELLOW}Your capture offer to ${target.name} has expired.")
            }
        }
    }

    fun takePrisoner(target: Player, captor: Player) {
        prisoners[target.uniqueId] = Prisoner(captor.uniqueId, target.gameMode)
        target.gameMode = GameMode.ADVENTURE
    }

    /** Restores the prisoner's original gamemode and clears their state. Returns false if they weren't a prisoner. */
    fun releasePrisoner(playerId: UUID): Boolean {
        val prisoner = prisoners.remove(playerId) ?: return false
        val player = plugin.server.getPlayer(playerId)
        player?.gameMode = prisoner.previousGameMode
        return true
    }

    /** For the captor choosing to execute rather than free — just clears the tracked state, doesn't kill the player itself */
    fun clearPrisonerState(playerId: UUID) {
        prisoners.remove(playerId)
    }

    /**
     * Cancel all pending expiry tasks (called on plugin disable)
     */
    fun cancelAllExpiryTasks() {
        expiryTasks.values.forEach { it.cancel() }
        expiryTasks.clear()
    }
}
