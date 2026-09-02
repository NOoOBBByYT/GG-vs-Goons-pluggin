package com.tyler.ggvsgoons.teams

import net.md_5.bungee.api.ChatColor as BungeeChatColor
import org.bukkit.ChatColor as BukkitChatColor
import org.bukkit.Location

/**
 * One side of the match (GG or Goons).
 *
 * @param id         lowercase key used in commands/config/persistence, e.g. "gg"
 * @param bukkitName the name of the actual Bukkit Scoreboard Team, e.g. "GG"
 * @param label      plain display name, e.g. "GG"
 * @param color      nametag color. Bukkit's Scoreboard Team API requires org.bukkit.ChatColor
 *                   specifically (not the bungee one the rest of the plugin uses for messages)
 * @param spawn      team spawn point, null until an admin sets one
 */

 data class Team(
    val id: String,
    val bukkitName: String,
    val label: String,
    val color: BukkitChatColor,
    var spawn: Location? = null
) {
    /**
     * Color-formatted name for chat messages, uses the same bungee ChatColor the rest of the plugin uses.
     */
    val displayName: String
        get() = "${BungeeChatColor.valueOf(color.name)}$label"
}