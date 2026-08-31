package com.tyler.ggvsgoons.commands

import com.tyler.ggvsgoons.GGvGPlugin
import org.bukkit.GameMode
import org.bukkit.entity.Player
import java.util.UUID

/**
 * captorId -> targetId, while an offer is awaiting accept/decline
 */
data class PendingOffer(val captorId: UUID, val targetId: UUID, val issuedAtMillis: Long = System.currentTimeMillis())

/**
 * prisonerId -> captorId, plus the gamemode to restore them to on release
 */
data class Prisoner(val captorId: UUID, val previousGameMode: GameMode)

class PrisonerManager(private val plugin: GGvGPlugin) {

    private val pendingOffers = mutableMapOf<UUID, PendingOffer>() // keyed by targetId
    private val prisoners = mutableMapOf<UUID, Prisoner>()          // keyed by prisonerId

    fun activePrisonerCount() = prisoners.size

    fun isPrisoner(playerId: UUID) = prisoners.containsKey(playerId)

    fun captorOf(playerId: UUID): UUID? = prisoners[playerId]?.captorId

    fun hasPendingOffer(targetId: UUID) = pendingOffers.containsKey(targetId)

    fun createOffer(captor: Player, target: Player): Boolean {
        if (isPrisoner(target.uniqueId) || hasPendingOffer(target.uniqueId)) return false
        pendingOffers[target.uniqueId] = PendingOffer(captor.uniqueId, target.uniqueId)
        return true
    }

    /** Returns the captorId this offer came from, or null if it wasn't valid / expired */
    fun consumeOffer(targetId: UUID, expectedCaptorId: UUID): UUID? {
        val offer = pendingOffers[targetId] ?: return null
        if (offer.captorId != expectedCaptorId) return null
        pendingOffers.remove(targetId)
        return offer.captorId
    }

    fun clearOffer(targetId: UUID) {
        pendingOffers.remove(targetId)
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
}
