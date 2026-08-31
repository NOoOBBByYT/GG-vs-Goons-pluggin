package com.tyler.ggvsgoons.commands

import com.tyler.ggvsgoons.GGvGModule
import com.tyler.ggvsgoons.GGvGPlugin
import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.EntitySelectorArgument
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import java.util.UUID

/**
 * Handles /warprisoner, /freeprisoner, /executeprisoner.
 * Self-contained: owns its own PrisonerManager, exposes it in case other
 * future modules need to check prisoner status (e.g. blocking a territory
 * capture command while someone is imprisoned).
 */
class WarPrisonerModule(plugin: GGvGPlugin) : GGvGModule {

    val manager = PrisonerManager(plugin)

    override fun register(plugin: GGvGPlugin) {
        // /warprisoner <target>  — issue a capture offer
        CommandAPICommand("warprisoner")
            .withArguments(EntitySelectorArgument.OnePlayer("target"))
            .executesPlayer(PlayerCommandExecutor { captor, args ->
                val target = args.get("target") as Player

                if (target.uniqueId == captor.uniqueId) {
                    captor.sendMessage(Component.text("You can't take yourself prisoner.", NamedTextColor.RED))
                    return@PlayerCommandExecutor
                }
                if (manager.isPrisoner(target.uniqueId)) {
                    captor.sendMessage(Component.text("${target.name} is already someone's prisoner.", NamedTextColor.RED))
                    return@PlayerCommandExecutor
                }
                if (!manager.createOffer(captor, target)) {
                    captor.sendMessage(Component.text("${target.name} already has a pending offer.", NamedTextColor.RED))
                    return@PlayerCommandExecutor
                }

                captor.sendMessage(Component.text("Capture offer sent to ${target.name}.", NamedTextColor.GRAY))

                val captorIdStr = captor.uniqueId.toString()
                val accept = Component.text("[Accept]", NamedTextColor.GREEN)
                    .clickEvent(ClickEvent.runCommand("/warprisoneraccept $captorIdStr"))
                val decline = Component.text("[Decline]", NamedTextColor.RED)
                    .clickEvent(ClickEvent.runCommand("/warprisonerdecline $captorIdStr"))

                target.sendMessage(
                    Component.text("${captor.name} wants to take you as a war prisoner. ", NamedTextColor.YELLOW)
                        .append(accept).append(Component.text("  ")).append(decline)
                )
            })
            .register()

        // Hidden command triggered by clicking [Accept]
        CommandAPICommand("warprisoneraccept")
            .withArguments(StringArgument("captorId"))
            .executesPlayer(PlayerCommandExecutor { target, args ->
                val captorId = runCatching { UUID.fromString(args.get("captorId") as String) }.getOrNull()
                    ?: return@PlayerCommandExecutor
                val confirmedCaptorId = manager.consumeOffer(target.uniqueId, captorId) ?: run {
                    target.sendMessage(Component.text("That offer is no longer valid.", NamedTextColor.RED))
                    return@PlayerCommandExecutor
                }
                val captor = plugin.server.getPlayer(confirmedCaptorId)
                if (captor == null) {
                    target.sendMessage(Component.text("Your captor logged off before you could accept.", NamedTextColor.RED))
                    return@PlayerCommandExecutor
                }
                manager.takePrisoner(target, captor)

                target.sendMessage(Component.text("You are now a war prisoner. You've been set to Adventure mode.", NamedTextColor.GOLD))
                captor.sendMessage(Component.text("${target.name} accepted — they're your prisoner now.", NamedTextColor.GREEN))
            })
            .register()

        // Hidden command triggered by clicking [Decline]
        CommandAPICommand("warprisonerdecline")
            .withArguments(StringArgument("captorId"))
            .executesPlayer(PlayerCommandExecutor { target, args ->
                val captorId = runCatching { UUID.fromString(args.get("captorId") as String) }.getOrNull()
                    ?: return@PlayerCommandExecutor
                val confirmedCaptorId = manager.consumeOffer(target.uniqueId, captorId) ?: run {
                    target.sendMessage(Component.text("That offer is no longer valid.", NamedTextColor.RED))
                    return@PlayerCommandExecutor
                }
                val captor = plugin.server.getPlayer(confirmedCaptorId)
                target.sendMessage(Component.text("You declined. Better hope they don't just kill you now.", NamedTextColor.RED))
                captor?.sendMessage(Component.text("${target.name} declined. It's your call what happens next.", NamedTextColor.YELLOW))
            })
            .register()

        // /freeprisoner <target> — captor releases their prisoner
        CommandAPICommand("freeprisoner")
            .withArguments(EntitySelectorArgument.OnePlayer("target"))
            .executesPlayer(PlayerCommandExecutor { captor, args ->
                val target = args.get("target") as Player
                if (manager.captorOf(target.uniqueId) != captor.uniqueId) {
                    captor.sendMessage(Component.text("${target.name} isn't your prisoner.", NamedTextColor.RED))
                    return@PlayerCommandExecutor
                }
                manager.releasePrisoner(target.uniqueId)
                target.sendMessage(Component.text("You've been released.", NamedTextColor.GREEN))
                captor.sendMessage(Component.text("You released ${target.name}.", NamedTextColor.GRAY))
            })
            .register()

        // /executeprisoner <target> — captor ends the arrangement without restoring gamemode kindness;
        // actual killing is up to the captor in-game, this just clears the tracked state
        CommandAPICommand("executeprisoner")
            .withArguments(EntitySelectorArgument.OnePlayer("target"))
            .executesPlayer(PlayerCommandExecutor { captor, args ->
                val target = args.get("target") as Player
                if (manager.captorOf(target.uniqueId) != captor.uniqueId) {
                    captor.sendMessage(Component.text("${target.name} isn't your prisoner.", NamedTextColor.RED))
                    return@PlayerCommandExecutor
                }
                manager.clearPrisonerState(target.uniqueId)
                target.sendMessage(Component.text("Your captor has ended your imprisonment. Good luck.", NamedTextColor.DARK_RED))
                captor.sendMessage(Component.text("${target.name} is no longer marked as your prisoner.", NamedTextColor.GRAY))
            })
            .register()
    }
}
