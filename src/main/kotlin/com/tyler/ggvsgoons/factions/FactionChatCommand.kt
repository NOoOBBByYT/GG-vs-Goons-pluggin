package com.tyler.ggvsgoons.factions

import com.tyler.ggvsgoons.GGvGPlugin
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * Shorthand command for faction chat: /fc <message>
 */
class FactionChatCommand(
    private val plugin: GGvGPlugin,
    private val manager: FactionManager
) : CommandExecutor {
    
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("${ChatColor.RED}This command can only be used by players")
            return true
        }
        
        // Check if factions are enabled
        if (!plugin.config.getBoolean("factions.enabled", true)) {
            sender.sendMessage("${ChatColor.RED}The faction system is currently disabled")
            return true
        }
        
        // Check if faction chat is enabled
        if (!plugin.config.getBoolean("factions.chat.enabled", true)) {
            sender.sendMessage("${ChatColor.RED}Faction chat is disabled")
            return true
        }
        
        if (args.isEmpty()) {
            sender.sendMessage("${ChatColor.RED}Usage: /fc <message>")
            return true
        }
        
        val faction = manager.getFactionByPlayer(sender.uniqueId)
        if (faction == null) {
            sender.sendMessage("${ChatColor.RED}You are not in a faction")
            return true
        }
        
        val message = args.joinToString(" ")
        val prefix = plugin.config.getString("factions.chat.prefix", "[FACTION] ")
        val formattedMessage = "$prefix${faction.color}${sender.name}: ${ChatColor.WHITE}$message"
        
        // Send to all faction members
        faction.members.forEach { memberId ->
            Bukkit.getPlayer(memberId)?.sendMessage(formattedMessage)
        }
        
        return true
    }
}
