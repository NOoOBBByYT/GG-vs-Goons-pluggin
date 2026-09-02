package com.tyler.ggvsgoons.persistence

import com.tyler.ggvsgoons.factions.Faction
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.UUID
import java.util.logging.Logger

/**
 * Handles saving and loading faction data to/from YAML files
 */
object FactionPersistence {
    
    /**
     * Save factions to a YAML file
     */
    fun saveFactions(factions: List<Faction>, file: File, logger: Logger) {
        try {
            val config = YamlConfiguration()
            
            factions.forEachIndexed { index, faction ->
                val path = "factions.$index"
                config.set("$path.id", faction.id)
                config.set("$path.name", faction.name)
                config.set("$path.teamId", faction.teamId)
                config.set("$path.leaderId", faction.leaderId.toString())
                config.set("$path.members", faction.members.map { it.toString() })
                config.set("$path.officers", faction.officers.map { it.toString() })
                config.set("$path.description", faction.description)
                config.set("$path.color", faction.color.name)
                config.set("$path.createdAt", faction.createdAt)
                config.set("$path.maxMembers", faction.maxMembers)
                
                // Save home location if set
                faction.homeLocation?.let { loc ->
                    config.set("$path.home.world", loc.world?.name)
                    config.set("$path.home.x", loc.x)
                    config.set("$path.home.y", loc.y)
                    config.set("$path.home.z", loc.z)
                    config.set("$path.home.yaw", loc.yaw)
                    config.set("$path.home.pitch", loc.pitch)
                }
            }
            
            config.save(file)
            logger.info("Saved ${factions.size} faction(s) to ${file.name}")
        } catch (e: Exception) {
            logger.severe("Failed to save factions: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Load factions from a YAML file
     */
    fun loadFactions(file: File, logger: Logger): List<Faction> {
        if (!file.exists()) {
            logger.info("No faction data file found, starting fresh")
            return emptyList()
        }
        
        try {
            val config = YamlConfiguration.loadConfiguration(file)
            val factions = mutableListOf<Faction>()
            
            val factionsSection = config.getConfigurationSection("factions") ?: return emptyList()
            
            for (key in factionsSection.getKeys(false)) {
                val path = "factions.$key"
                
                try {
                    val id = config.getString("$path.id") ?: continue
                    val name = config.getString("$path.name") ?: continue
                    val teamId = config.getString("$path.teamId") ?: continue
                    val leaderId = UUID.fromString(config.getString("$path.leaderId") ?: continue)
                    
                    val members = config.getStringList("$path.members")
                        .mapNotNull { 
                            try { UUID.fromString(it) } 
                            catch (e: Exception) { null }
                        }
                        .toMutableSet()
                    
                    val officers = config.getStringList("$path.officers")
                        .mapNotNull { 
                            try { UUID.fromString(it) } 
                            catch (e: Exception) { null }
                        }
                        .toMutableSet()
                    
                    val description = config.getString("$path.description") ?: ""
                    val colorName = config.getString("$path.color") ?: "WHITE"
                    val color = try {
                        ChatColor.valueOf(colorName)
                    } catch (e: Exception) {
                        ChatColor.WHITE
                    }
                    
                    val createdAt = config.getLong("$path.createdAt", System.currentTimeMillis())
                    val maxMembers = config.getInt("$path.maxMembers", 10)
                    
                    // Load home location if exists
                    var homeLocation: Location? = null
                    if (config.contains("$path.home.world")) {
                        val worldName = config.getString("$path.home.world")
                        val world = Bukkit.getWorld(worldName ?: "")
                        
                        if (world != null) {
                            val x = config.getDouble("$path.home.x")
                            val y = config.getDouble("$path.home.y")
                            val z = config.getDouble("$path.home.z")
                            val yaw = config.getDouble("$path.home.yaw").toFloat()
                            val pitch = config.getDouble("$path.home.pitch").toFloat()
                            
                            homeLocation = Location(world, x, y, z, yaw, pitch)
                        } else {
                            logger.warning("World '$worldName' not found for faction $name home location")
                        }
                    }
                    
                    val faction = Faction(
                        id = id,
                        name = name,
                        teamId = teamId,
                        leaderId = leaderId,
                        members = members,
                        officers = officers,
                        description = description,
                        color = color,
                        homeLocation = homeLocation,
                        createdAt = createdAt,
                        maxMembers = maxMembers
                    )
                    
                    factions.add(faction)
                } catch (e: Exception) {
                    logger.warning("Failed to load faction at index $key: ${e.message}")
                }
            }
            
            logger.info("Loaded ${factions.size} faction(s) from ${file.name}")
            return factions
        } catch (e: Exception) {
            logger.severe("Failed to load factions: ${e.message}")
            e.printStackTrace()
            return emptyList()
        }
    }
}
