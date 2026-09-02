package com.tyler.ggvsgoons.factions

import org.bukkit.ChatColor
import org.bukkit.Location
import java.util.UUID

/**
 * Represents a faction within a team.
 * Factions are sub-groups that allow players on the same team to organize into smaller units.
 */
data class Faction(
    val id: String,
    val name: String,
    val teamId: String,                    // "gg" or "goons"
    val leaderId: UUID,
    val members: MutableSet<UUID> = mutableSetOf(),
    val officers: MutableSet<UUID> = mutableSetOf(),
    val description: String = "",
    val color: ChatColor = ChatColor.WHITE,
    var homeLocation: Location? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val maxMembers: Int = 10
) {
    init {
        // Ensure leader is always in members set
        members.add(leaderId)
    }
    
    /**
     * Check if a player is the leader of this faction
     */
    fun isLeader(playerId: UUID): Boolean = leaderId == playerId
    
    /**
     * Check if a player is an officer of this faction
     */
    fun isOfficer(playerId: UUID): Boolean = officers.contains(playerId)
    
    /**
     * Check if a player is a member of this faction
     */
    fun isMember(playerId: UUID): Boolean = members.contains(playerId)
    
    /**
     * Check if the faction is at max capacity
     */
    fun isFull(): Boolean = members.size >= maxMembers
    
    /**
     * Get the role of a player in this faction
     */
    fun getRole(playerId: UUID): FactionRole? {
        return when {
            isLeader(playerId) -> FactionRole.LEADER
            isOfficer(playerId) -> FactionRole.OFFICER
            isMember(playerId) -> FactionRole.MEMBER
            else -> null
        }
    }
}

/**
 * Represents a pending invitation to join a faction
 */
data class FactionInvite(
    val factionId: String,
    val inviterId: UUID,
    val inviteeId: UUID,
    val expiresAt: Long
) {
    /**
     * Check if this invite has expired
     */
    fun isExpired(): Boolean = System.currentTimeMillis() > expiresAt
}

/**
 * Represents statistics for a faction
 */
data class FactionStats(
    val factionId: String,
    var totalKills: Int = 0,
    var totalDeaths: Int = 0,
    var prisonersCaptured: Int = 0,
    var territoryClaimed: Int = 0
) {
    /**
     * Calculate kill/death ratio
     */
    fun getKDR(): Double {
        return if (totalDeaths == 0) totalKills.toDouble()
        else totalKills.toDouble() / totalDeaths.toDouble()
    }
}

/**
 * Represents the role hierarchy within a faction
 */
enum class FactionRole(val displayName: String, val priority: Int) {
    LEADER("Leader", 3),
    OFFICER("Officer", 2),
    MEMBER("Member", 1);
    
    /**
     * Check if this role has permission to invite members
     */
    fun canInvite(): Boolean = this == LEADER || this == OFFICER
    
    /**
     * Check if this role has permission to kick members
     */
    fun canKick(): Boolean = this == LEADER || this == OFFICER
    
    /**
     * Check if this role has permission to promote/demote
     */
    fun canPromote(): Boolean = this == LEADER
    
    /**
     * Check if this role has permission to disband the faction
     */
    fun canDisband(): Boolean = this == LEADER
    
    /**
     * Check if this role has permission to set faction home
     */
    fun canSetHome(): Boolean = this == LEADER || this == OFFICER
}
