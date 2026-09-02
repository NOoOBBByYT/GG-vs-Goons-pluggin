package com.tyler.ggvsgoons.commands

import com.tyler.ggvsgoons.GGvGModule
import com.tyler.ggvsgoons.GGvGPlugin
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.ComponentBuilder
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.UUID

/**
 * Handles /warprisoner, /freeprisoner, /executeprisoner, /listprisoners.
 * Self-contained: owns its own PrisonerManager, exposes it in case other
 * future modules need to check prisoner status (e.g. blocking a territory
 * capture command while someone is imprisoned).
 */
class WarPrisonerModule(
    private val plugin: GGvGPlugin,
    offerExpirySeconds: Int,
    private val permissionsEnabled: Boolean
) : GGvGModule {

    val manager = PrisonerManager(plugin, offerExpirySeconds)

    override fun register(plugin: GGvGPlugin) {
        plugin.getCommand("warprisoner")?.setExecutor(WarPrisonerCommand(this, plugin, permissionsEnabled))
        plugin.getCommand("warprisoneraccept")?.setExecutor(WarPrisonerAcceptCommand(this, plugin))
        plugin.getCommand("warprisonerdecline")?.setExecutor(WarPrisonerDeclineCommand(this, plugin))
        plugin.getCommand("freeprisoner")?.setExecutor(FreePrisonerCommand(this, plugin, permissionsEnabled))
        plugin.getCommand("executeprisoner")?.setExecutor(ExecutePrisonerCommand(this, plugin, permissionsEnabled))
        plugin.getCommand("listprisoners")?.setExecutor(ListPrisonersCommand(this, plugin, permissionsEnabled))
    }
}

// /warprisoner <target> — issue a capture offer
class WarPrisonerCommand(
    private val module: WarPrisonerModule,
    private val plugin: GGvGPlugin,
    private val permissionsEnabled: Boolean
) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("${ChatColor.RED}Only players can use this command.")
            return true
        }

        // Check permissions
        if (permissionsEnabled && !sender.hasPermission("ggvgoons.warprisoner.capture")) {
            sender.sendMessage("${ChatColor.RED}You don't have permission to use this command.")
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage("${ChatColor.RED}Usage: /warprisoner <player>")
            return true
        }

        val target = plugin.server.getPlayer(args[0])
        if (target == null) {
            sender.sendMessage("${ChatColor.RED}Player not found.")
            return true
        }

        if (target.uniqueId == sender.uniqueId) {
            sender.sendMessage("${ChatColor.RED}You can't take yourself prisoner.")
            return true
        }

        // Team validation: can only capture opposing team members
        val captorTeam = plugin.teams.manager.getPlayerTeam(sender.uniqueId)
        val targetTeam = plugin.teams.manager.getPlayerTeam(target.uniqueId)

        if (captorTeam == null) {
            sender.sendMessage("${ChatColor.RED}You must join a team first. Use /team join <gg|goons>")
            return true
        }

        if (targetTeam == null) {
            sender.sendMessage("${ChatColor.RED}${target.name} is not on a team and cannot be captured.")
            return true
        }

        if (!plugin.teams.manager.areOpposingTeams(sender.uniqueId, target.uniqueId)) {
            sender.sendMessage("${ChatColor.RED}You can only capture members of the opposing team!")
            return true
        }

        if (module.manager.isPrisoner(target.uniqueId)) {
            sender.sendMessage("${ChatColor.RED}${target.name} is already someone's prisoner.")
            return true
        }

        if (!module.manager.createOffer(sender, target)) {
            sender.sendMessage("${ChatColor.RED}${target.name} already has a pending offer.")
            return true
        }

        sender.sendMessage("${ChatColor.GRAY}Capture offer sent to ${target.name}.")

        val captorIdStr = sender.uniqueId.toString()
        val message = ComponentBuilder("${sender.name} wants to take you as a war prisoner. ")
            .color(ChatColor.YELLOW)
            .append("[Accept]")
            .color(ChatColor.GREEN)
            .event(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/warprisoneraccept $captorIdStr"))
            .append("  ")
            .color(ChatColor.YELLOW)
            .event(null as ClickEvent?)
            .append("[Decline]")
            .color(ChatColor.RED)
            .event(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/warprisonerdecline $captorIdStr"))
            .create()

        target.spigot().sendMessage(*message)

        return true
    }
}

// Hidden command triggered by clicking [Accept]
class WarPrisonerAcceptCommand(private val module: WarPrisonerModule, private val plugin: GGvGPlugin) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) return true
        if (args.isEmpty()) return true

        val captorId = runCatching { UUID.fromString(args[0]) }.getOrNull() ?: return true
        val confirmedCaptorId = module.manager.consumeOffer(sender.uniqueId, captorId) ?: run {
            sender.sendMessage("${ChatColor.RED}That offer is no longer valid.")
            return true
        }

        val captor = plugin.server.getPlayer(confirmedCaptorId)
        if (captor == null) {
            sender.sendMessage("${ChatColor.RED}Your captor logged off before you could accept.")
            return true
        }

        module.manager.takePrisoner(sender, captor)

        sender.sendMessage("${ChatColor.GOLD}You are now a war prisoner. You've been set to Adventure mode.")
        captor.sendMessage("${ChatColor.GREEN}${sender.name} accepted — they're your prisoner now.")

        return true
    }
}

// Hidden command triggered by clicking [Decline]
class WarPrisonerDeclineCommand(private val module: WarPrisonerModule, private val plugin: GGvGPlugin) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) return true
        if (args.isEmpty()) return true

        val captorId = runCatching { UUID.fromString(args[0]) }.getOrNull() ?: return true
        val confirmedCaptorId = module.manager.consumeOffer(sender.uniqueId, captorId) ?: run {
            sender.sendMessage("${ChatColor.RED}That offer is no longer valid.")
            return true
        }

        val captor = plugin.server.getPlayer(confirmedCaptorId)
        sender.sendMessage("${ChatColor.RED}You declined. Better hope they don't just kill you now.")
        captor?.sendMessage("${ChatColor.YELLOW}${sender.name} declined. It's your call what happens next.")

        return true
    }
}

// /freeprisoner <target> — captor releases their prisoner
class FreePrisonerCommand(
    private val module: WarPrisonerModule,
    private val plugin: GGvGPlugin,
    private val permissionsEnabled: Boolean
) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("${ChatColor.RED}Only players can use this command.")
            return true
        }

        // Check permissions
        if (permissionsEnabled && !sender.hasPermission("ggvgoons.warprisoner.free")) {
            sender.sendMessage("${ChatColor.RED}You don't have permission to use this command.")
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage("${ChatColor.RED}Usage: /freeprisoner <player>")
            return true
        }

        val target = plugin.server.getPlayer(args[0])
        if (target == null) {
            sender.sendMessage("${ChatColor.RED}Player not found.")
            return true
        }

        if (module.manager.captorOf(target.uniqueId) != sender.uniqueId) {
            sender.sendMessage("${ChatColor.RED}${target.name} isn't your prisoner.")
            return true
        }

        module.manager.releasePrisoner(target.uniqueId)
        target.sendMessage("${ChatColor.GREEN}You've been released.")
        sender.sendMessage("${ChatColor.GRAY}You released ${target.name}.")

        return true
    }
}

// /executeprisoner <target> — captor ends the arrangement without restoring gamemode kindness;
// actual killing is up to the captor in-game, this just clears the tracked state
class ExecutePrisonerCommand(
    private val module: WarPrisonerModule,
    private val plugin: GGvGPlugin,
    private val permissionsEnabled: Boolean
) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("${ChatColor.RED}Only players can use this command.")
            return true
        }

        // Check permissions
        if (permissionsEnabled && !sender.hasPermission("ggvgoons.warprisoner.execute")) {
            sender.sendMessage("${ChatColor.RED}You don't have permission to use this command.")
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage("${ChatColor.RED}Usage: /executeprisoner <player>")
            return true
        }

        val target = plugin.server.getPlayer(args[0])
        if (target == null) {
            sender.sendMessage("${ChatColor.RED}Player not found.")
            return true
        }

        if (module.manager.captorOf(target.uniqueId) != sender.uniqueId) {
            sender.sendMessage("${ChatColor.RED}${target.name} isn't your prisoner.")
            return true
        }

        module.manager.clearPrisonerState(target.uniqueId)
        target.sendMessage("${ChatColor.DARK_RED}Your captor has ended your imprisonment. Good luck.")
        sender.sendMessage("${ChatColor.GRAY}${target.name} is no longer marked as your prisoner.")

        return true
    }
}

// /listprisoners — show all active prisoners
class ListPrisonersCommand(
    private val module: WarPrisonerModule,
    private val plugin: GGvGPlugin,
    private val permissionsEnabled: Boolean
) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        // Check permissions
        if (permissionsEnabled && sender is Player && !sender.hasPermission("ggvgoons.warprisoner.list")) {
            sender.sendMessage("${ChatColor.RED}You don't have permission to use this command.")
            return true
        }

        val prisoners = module.manager.getPrisoners()
        
        if (prisoners.isEmpty()) {
            sender.sendMessage("${ChatColor.GRAY}There are currently no active prisoners.")
            return true
        }

        sender.sendMessage("${ChatColor.GOLD}=== Active War Prisoners (${prisoners.size}) ===")
        prisoners.forEach { (prisonerId, prisoner) ->
            val prisonerPlayer = plugin.server.getPlayer(prisonerId)
            val captorPlayer = plugin.server.getPlayer(prisoner.captorId)
            
            val prisonerName = prisonerPlayer?.name ?: prisonerId.toString().substring(0, 8)
            val captorName = captorPlayer?.name ?: prisoner.captorId.toString().substring(0, 8)
            
            sender.sendMessage("${ChatColor.YELLOW}• ${ChatColor.WHITE}$prisonerName ${ChatColor.GRAY}(held by ${ChatColor.WHITE}$captorName${ChatColor.GRAY})")
        }

        return true
    }
}
