package com.tyler.ggvsgoons.teams

import com.tyler.ggvsgoons.GGvGModule
import com.tyler.ggvsgoons.GGvGPlugin
import com.tyler.ggvsgoons.persistence.TeamPersistence
import net.md_5.bungee.api.ChatColor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerJoinEvent

/**
 * Phase 2 entry point. Follows the same GGvGModule pattern as WarPrisonerModule —
 * self-contained, exposes `manager` publicly for cross-module access (used by the
 * prisoner system for team validation, see Step 5 in the plan).
 */
class TeamsModule(private val plugin: GGvGPlugin) : GGvGModule, Listener {

    val manager = TeamManager(plugin)
    val persistence = TeamPersistence(plugin, manager)

    override fun register(plugin: GGvGPlugin) {
        plugin.getCommand("team")?.setExecutor(TeamCommand(this, plugin))
        plugin.server.pluginManager.registerEvents(this, plugin)

        manager.setupScoreboardTeams()
        persistence.load()
    }

    /** Call this from GGvGPlugin.onDisable() so team data is saved on shutdown,
     *  the same way PrisonerPersistence is. */
    fun onDisable() {
        persistence.save()
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        // Bukkit scoreboard team entries don't survive a restart on their own —
        // teams.yml remembers who's on which team, this puts them back on rejoin.
        manager.restoreScoreboardEntry(event.player)
    }

    @EventHandler
    fun onChat(event: AsyncPlayerChatEvent) {
        // Support @team prefix for quick team chat
        val message = event.message
        if (!message.startsWith("@team ")) return
        
        if (!plugin.config.getBoolean("teams.chat.enabled", true)) {
            event.player.sendMessage("${ChatColor.RED}Team chat is disabled.")
            event.isCancelled = true
            return
        }

        event.isCancelled = true
        val teamMessage = message.substring(6).trim()
        
        if (teamMessage.isEmpty()) {
            event.player.sendMessage("${ChatColor.RED}Usage: @team <message>")
            return
        }

        val senderTeam = manager.getPlayerTeam(event.player.uniqueId)
        if (senderTeam == null) {
            event.player.sendMessage("${ChatColor.RED}You're not on a team.")
            return
        }

        val prefix = plugin.config.getString("teams.chat.prefix", "[TEAM] ") ?: "[TEAM] "
        val formatted = "$prefix${senderTeam.displayName}${ChatColor.RESET} ${event.player.name}: $teamMessage"

        // Send to all team members
        manager.getTeamMembers(senderTeam.id).forEach { uuid ->
            plugin.server.getPlayer(uuid)?.sendMessage(formatted)
        }
    }
}