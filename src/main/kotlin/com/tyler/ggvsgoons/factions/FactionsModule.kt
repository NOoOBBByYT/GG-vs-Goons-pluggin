package com.tyler.ggvsgoons.factions

import com.tyler.ggvsgoons.GGvGModule
import com.tyler.ggvsgoons.GGvGPlugin
import com.tyler.ggvsgoons.persistence.FactionPersistence
import org.bukkit.Bukkit
import java.io.File

/**
 * Module for the faction system
 */
class FactionsModule(private val plugin: GGvGPlugin) : GGvGModule {
    
    lateinit var manager: FactionManager
        private set
    
    private lateinit var eventListener: FactionEventListener
    private var cleanupTaskId: Int = -1
    
    override fun register(plugin: GGvGPlugin) {
        // Check if factions are enabled
        if (!plugin.config.getBoolean("factions.enabled", true)) {
            plugin.logger.info("Factions system is disabled")
            return
        }
        
        // Initialize manager
        manager = FactionManager(plugin)
        manager.loadConfig()
        
        // Register command
        val command = FactionCommand(plugin, manager)
        plugin.getCommand("faction")?.setExecutor(command)
        plugin.getCommand("faction")?.tabCompleter = command
        
        // Register shorthand for faction chat
        val chatCommand = FactionChatCommand(plugin, manager)
        plugin.getCommand("fc")?.setExecutor(chatCommand)
        
        // Register event listener
        eventListener = FactionEventListener(plugin, manager)
        Bukkit.getPluginManager().registerEvents(eventListener, plugin)
        
        // Load persisted faction data
        if (plugin.config.getBoolean("factions.enable-persistence", true)) {
            loadFactions()
        }
        
        // Schedule cleanup task for expired invites (every 5 minutes)
        cleanupTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, {
            manager.cleanupExpiredInvites()
        }, 6000L, 6000L) // 5 minutes in ticks
        
        plugin.logger.info("Factions module enabled")
    }
    
    /**
     * Load factions from persistence
     */
    private fun loadFactions() {
        val dataFolder = plugin.dataFolder
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }
        
        val factionsFile = File(dataFolder, "factions.yml")
        val loadedFactions = FactionPersistence.loadFactions(factionsFile, plugin.logger)
        manager.loadFactions(loadedFactions)
        
        plugin.logger.info("Loaded ${loadedFactions.size} faction(s)")
    }
    
    /**
     * Save factions to persistence
     */
    fun saveFactions() {
        if (!plugin.config.getBoolean("factions.enable-persistence", true)) {
            return
        }
        
        val factionsFile = File(plugin.dataFolder, "factions.yml")
        val factions = manager.getFactions()
        FactionPersistence.saveFactions(factions, factionsFile, plugin.logger)
    }
    
    /**
     * Called when the plugin is disabled
     */
    fun onDisable() {
        // Cancel cleanup task
        if (cleanupTaskId != -1) {
            Bukkit.getScheduler().cancelTask(cleanupTaskId)
        }
        
        // Save factions
        saveFactions()
    }
}
