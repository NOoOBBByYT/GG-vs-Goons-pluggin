package com.tyler.ggvsgoons.teams

import com.tyler.ggvsgoons.GGvGPlugin
import net.md_5.bungee.api.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

/**
 * Handles all /team subcommands:
 * - join <gg|goons>
 * - leave
 * - info [player]
 * - list
 * - spawn
 * - setspawn (admin)
 * - chat <message>
 */
class TeamCommand(
    private val module: TeamsModule,
    private val plugin: GGvGPlugin
) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sendHelp(sender)
            return true
        }

        when (args[0].lowercase()) {
            "join" -> handleJoin(sender, args)
            "leave" -> handleLeave(sender)
            "info" -> handleInfo(sender, args)
            "list" -> handleList(sender)
            "spawn" -> handleSpawn(sender)
            "setspawn" -> handleSetSpawn(sender)
            "chat" -> handleChat(sender, args)
            else -> sendHelp(sender)
        }

        return true
    }

    private fun handleJoin(sender: CommandSender, args: Array<out String>) {
        if (sender !is Player) {
            sender.sendMessage("${ChatColor.RED}Only players can join teams.")
            return
        }

        if (!sender.hasPermission("ggvgoons.team.use")) {
            sender.sendMessage("${ChatColor.RED}You don't have permission to use this command.")
            return
        }

        if (args.size < 2) {
            sender.sendMessage("${ChatColor.RED}Usage: /team join <gg|goons>")
            return
        }

        val teamId = args[1].lowercase()
        if (teamId !in module.manager.teams) {
            sender.sendMessage("${ChatColor.RED}Invalid team. Choose 'gg' or 'goons'.")
            return
        }

        val currentTeam = module.manager.getPlayerTeam(sender.uniqueId)
        if (currentTeam?.id == teamId) {
            sender.sendMessage("${ChatColor.RED}You're already on team ${currentTeam.label}.")
            return
        }

        if (module.manager.joinTeam(sender, teamId)) {
            val team = module.manager.teams[teamId]!!
            sender.sendMessage("${ChatColor.GREEN}You joined team ${team.displayName}${ChatColor.GREEN}!")
            
            // Announce to team members
            module.manager.getTeamMembers(teamId).forEach { uuid ->
                if (uuid != sender.uniqueId) {
                    plugin.server.getPlayer(uuid)?.sendMessage(
                        "${ChatColor.GRAY}${sender.name} joined your team!"
                    )
                }
            }
        }
    }

    private fun handleLeave(sender: CommandSender) {
        if (sender !is Player) {
            sender.sendMessage("${ChatColor.RED}Only players can leave teams.")
            return
        }

        if (!sender.hasPermission("ggvgoons.team.use")) {
            sender.sendMessage("${ChatColor.RED}You don't have permission to use this command.")
            return
        }

        val team = module.manager.getPlayerTeam(sender.uniqueId)
        if (team == null) {
            sender.sendMessage("${ChatColor.RED}You're not on a team.")
            return
        }

        if (module.manager.leaveTeam(sender)) {
            sender.sendMessage("${ChatColor.GRAY}You left team ${team.displayName}${ChatColor.GRAY}.")
            
            // Announce to former team members
            module.manager.getTeamMembers(team.id).forEach { uuid ->
                plugin.server.getPlayer(uuid)?.sendMessage(
                    "${ChatColor.GRAY}${sender.name} left the team."
                )
            }
        }
    }

    private fun handleInfo(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("ggvgoons.team.use")) {
            sender.sendMessage("${ChatColor.RED}You don't have permission to use this command.")
            return
        }

        val target = if (args.size > 1) {
            plugin.server.getPlayer(args[1]) ?: run {
                sender.sendMessage("${ChatColor.RED}Player not found.")
                return
            }
        } else {
            if (sender !is Player) {
                sender.sendMessage("${ChatColor.RED}You must specify a player name.")
                return
            }
            sender
        }

        val team = module.manager.getPlayerTeam(target.uniqueId)
        if (team == null) {
            sender.sendMessage("${ChatColor.GRAY}${target.name} is not on a team.")
            return
        }

        val memberCount = module.manager.getTeamMembers(team.id).size
        val spawnSet = if (team.spawn != null) "${ChatColor.GREEN}Yes" else "${ChatColor.RED}No"

        sender.sendMessage("${ChatColor.GOLD}=== Team Info: ${team.displayName}${ChatColor.GOLD} ===")
        sender.sendMessage("${ChatColor.YELLOW}Player: ${ChatColor.WHITE}${target.name}")
        sender.sendMessage("${ChatColor.YELLOW}Members: ${ChatColor.WHITE}$memberCount")
        sender.sendMessage("${ChatColor.YELLOW}Spawn Set: $spawnSet")
    }

    private fun handleList(sender: CommandSender) {
        if (!sender.hasPermission("ggvgoons.team.use")) {
            sender.sendMessage("${ChatColor.RED}You don't have permission to use this command.")
            return
        }

        sender.sendMessage("${ChatColor.GOLD}=== Team Rosters ===")

        for ((teamId, team) in module.manager.teams) {
            val members = module.manager.getTeamMembers(teamId)
            sender.sendMessage("${team.displayName}${ChatColor.GRAY} (${members.size} members):")
            
            if (members.isEmpty()) {
                sender.sendMessage("  ${ChatColor.GRAY}No members")
            } else {
                members.forEach { uuid ->
                    val player = plugin.server.getPlayer(uuid)
                    val name = player?.name ?: uuid.toString().substring(0, 8)
                    val status = if (player?.isOnline == true) "${ChatColor.GREEN}●" else "${ChatColor.GRAY}●"
                    sender.sendMessage("  $status ${ChatColor.WHITE}$name")
                }
            }
        }
    }

    private fun handleSpawn(sender: CommandSender) {
        if (sender !is Player) {
            sender.sendMessage("${ChatColor.RED}Only players can teleport to team spawn.")
            return
        }

        if (!sender.hasPermission("ggvgoons.team.use")) {
            sender.sendMessage("${ChatColor.RED}You don't have permission to use this command.")
            return
        }

        val team = module.manager.getPlayerTeam(sender.uniqueId)
        if (team == null) {
            sender.sendMessage("${ChatColor.RED}You're not on a team.")
            return
        }

        if (module.manager.teleportToTeamSpawn(sender)) {
            sender.sendMessage("${ChatColor.GREEN}Teleported to team spawn.")
        } else {
            sender.sendMessage("${ChatColor.RED}Your team doesn't have a spawn point set yet.")
        }
    }

    private fun handleSetSpawn(sender: CommandSender) {
        if (sender !is Player) {
            sender.sendMessage("${ChatColor.RED}Only players can set team spawns.")
            return
        }

        if (!sender.hasPermission("ggvgoons.team.admin")) {
            sender.sendMessage("${ChatColor.RED}You don't have permission to use this command.")
            return
        }

        val team = module.manager.getPlayerTeam(sender.uniqueId)
        if (team == null) {
            sender.sendMessage("${ChatColor.RED}You're not on a team.")
            return
        }

        module.manager.setTeamSpawn(team.id, sender.location)
        sender.sendMessage("${ChatColor.GREEN}Team spawn set at your current location.")
        
        // Announce to team
        module.manager.getTeamMembers(team.id).forEach { uuid ->
            if (uuid != sender.uniqueId) {
                plugin.server.getPlayer(uuid)?.sendMessage(
                    "${ChatColor.GRAY}${sender.name} set a new team spawn point."
                )
            }
        }
    }

    private fun handleChat(sender: CommandSender, args: Array<out String>) {
        if (sender !is Player) {
            sender.sendMessage("${ChatColor.RED}Only players can use team chat.")
            return
        }

        if (!sender.hasPermission("ggvgoons.team.use")) {
            sender.sendMessage("${ChatColor.RED}You don't have permission to use this command.")
            return
        }

        if (!plugin.config.getBoolean("teams.chat.enabled", true)) {
            sender.sendMessage("${ChatColor.RED}Team chat is disabled.")
            return
        }

        val team = module.manager.getPlayerTeam(sender.uniqueId)
        if (team == null) {
            sender.sendMessage("${ChatColor.RED}You're not on a team.")
            return
        }

        if (args.size < 2) {
            sender.sendMessage("${ChatColor.RED}Usage: /team chat <message>")
            return
        }

        val message = args.drop(1).joinToString(" ")
        val prefix = plugin.config.getString("teams.chat.prefix", "[TEAM] ") ?: "[TEAM] "
        val formatted = "$prefix${team.displayName}${ChatColor.RESET} ${sender.name}: $message"

        // Send to all team members
        module.manager.getTeamMembers(team.id).forEach { uuid ->
            plugin.server.getPlayer(uuid)?.sendMessage(formatted)
        }
    }

    private fun sendHelp(sender: CommandSender) {
        sender.sendMessage("${ChatColor.GOLD}=== Team Commands ===")
        sender.sendMessage("${ChatColor.YELLOW}/team join <gg|goons> ${ChatColor.GRAY}- Join a team")
        sender.sendMessage("${ChatColor.YELLOW}/team leave ${ChatColor.GRAY}- Leave your team")
        sender.sendMessage("${ChatColor.YELLOW}/team info [player] ${ChatColor.GRAY}- Show team info")
        sender.sendMessage("${ChatColor.YELLOW}/team list ${ChatColor.GRAY}- List all team members")
        sender.sendMessage("${ChatColor.YELLOW}/team spawn ${ChatColor.GRAY}- Teleport to team spawn")
        sender.sendMessage("${ChatColor.YELLOW}/team setspawn ${ChatColor.GRAY}- Set team spawn (admin)")
        sender.sendMessage("${ChatColor.YELLOW}/team chat <message> ${ChatColor.GRAY}- Send team message")
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        if (args.size == 1) {
            return listOf("join", "leave", "info", "list", "spawn", "setspawn", "chat")
                .filter { it.startsWith(args[0].lowercase()) }
        }

        if (args.size == 2 && args[0].equals("join", ignoreCase = true)) {
            return listOf("gg", "goons").filter { it.startsWith(args[1].lowercase()) }
        }

        if (args.size == 2 && args[0].equals("info", ignoreCase = true)) {
            return plugin.server.onlinePlayers.map { it.name }
                .filter { it.lowercase().startsWith(args[1].lowercase()) }
        }

        return emptyList()
    }
}
