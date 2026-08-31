package com.tyler.ggvsgoons

import com.tyler.ggvsgoons.commands.WarPrisonerModule
import org.bukkit.plugin.java.JavaPlugin

/**
 * A "module" is one self-contained feature (a set of related commands + its own state).
 * To add a new mechanic later: create a class implementing GGvGModule under commands/,
 * instantiate it below, and add it to the `modules` list. Nothing else needs to change.
 */
interface GGvGModule {
    fun register(plugin: GGvGPlugin)
}

class GGvGPlugin : JavaPlugin() {

    lateinit var warPrisoner: WarPrisonerModule
        private set

    private val modules = mutableListOf<GGvGModule>()

    override fun onEnable() {
        warPrisoner = WarPrisonerModule(this)
        modules += warPrisoner

        // Future modules go here, e.g.:
        // modules += TerritoryControlModule(this)
        // modules += LootDropModule(this)

        modules.forEach { it.register(this) }
        logger.info("GGvGoons enabled - ${modules.size} module(s) loaded")
    }

    override fun onDisable() {
        logger.info("GGvGoons disabled")
    }
}
