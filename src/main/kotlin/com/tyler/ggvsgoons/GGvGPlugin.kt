package com.tyler.ggvsgoons

import com.tyler.ggvsgoons.commands.WarPrisonerModule
import com.tyler.ggvsgoons.persistence.PrisonerPersistence
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

/**
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
        // Save default config if it doesn't exist
        saveDefaultConfig()
        
        // Load configuration values
        val offerExpiryTimeout = config.getInt("prisoner.offer-expiry-timeout", 60)
        val persistenceEnabled = config.getBoolean("prisoner.enable-persistence", true)
        val permissionsEnabled = config.getBoolean("permissions.enabled", true)
        
        logger.info("Configuration loaded:")
        logger.info("  - Offer expiry timeout: ${if (offerExpiryTimeout > 0) "$offerExpiryTimeout seconds" else "disabled"}")
        logger.info("  - Persistence: ${if (persistenceEnabled) "enabled" else "disabled"}")
        logger.info("  - Permissions: ${if (permissionsEnabled) "enabled" else "OP-only"}")
        
        // Initialize modules
        warPrisoner = WarPrisonerModule(this, offerExpiryTimeout, permissionsEnabled)
        modules += warPrisoner

        // Future modules go here, e.g.:
        // modules += TerritoryControlModule(this)
        // zaidyn is a gay
        // modules += LootDropModule(this)

        modules.forEach { it.register(this) }
        
        // Load persisted prisoner data if enabled
        if (persistenceEnabled) {
            val dataFolder = dataFolder
            if (!dataFolder.exists()) {
                dataFolder.mkdirs()
            }
            val prisonersFile = File(dataFolder, "prisoners.yml")
            val loadedPrisoners = PrisonerPersistence.loadPrisoners(prisonersFile, logger)
            warPrisoner.manager.loadPrisoners(loadedPrisoners)
        }
        
        logger.info("GGvGoons enabled - ${modules.size} module(s) loaded")
    }

    override fun onDisable() {
        // Cancel all pending expiry tasks
        warPrisoner.manager.cancelAllExpiryTasks()
        
        // Save prisoner data if persistence is enabled
        val persistenceEnabled = config.getBoolean("prisoner.enable-persistence", true)
        if (persistenceEnabled) {
            val prisonersFile = File(dataFolder, "prisoners.yml")
            val prisoners = warPrisoner.manager.getPrisoners()
            PrisonerPersistence.savePrisoners(prisoners, prisonersFile, logger)
        }
        
        logger.info("GGvGoons disabled")
    }
}
