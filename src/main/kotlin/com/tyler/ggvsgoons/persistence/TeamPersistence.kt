package com.tyler.ggvsgoons.persistence

import com.tyler.ggvsgoons.GGvGPlugin
import com.tyler.ggvsgoons.teams.TeamManager
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.UUID

/** Reads/writes plugins/GGvGoons/teams.yml — team membership and spawn points. */
class TeamPersistence(
    private val plugin: GGvGPlugin,
    private val manager: TeamManager
) {

    private val file = File(plugin.dataFolder, "teams.yml")

    fun load() {
        if (!file.exists()) return
        val config = YamlConfiguration.loadConfiguration(file)
        val teamsSection = config.getConfigurationSection("teams") ?: return

        var restoredMembers = 0

        for (teamId in teamsSection.getKeys(false)) {
            val section = teamsSection.getConfigurationSection(teamId) ?: continue

            section.getStringList("members").forEach { idString ->
                try {
                    val uuid = UUID.fromString(idString)
                    manager.setPlayerTeamSilently(uuid, teamId)
                    restoredMembers++
                } catch (e: IllegalArgumentException) {
                    plugin.logger.warning("Invalid UUID in teams.yml under $teamId: $idString")
                }
            }

            val spawnSection = section.getConfigurationSection("spawn")
            if (spawnSection != null) {
                val world = Bukkit.getWorld(spawnSection.getString("world") ?: "world")
                if (world != null) {
                    val loc = Location(
                        world,
                        spawnSection.getDouble("x"),
                        spawnSection.getDouble("y"),
                        spawnSection.getDouble("z"),
                        spawnSection.getDouble("yaw").toFloat(),
                        spawnSection.getDouble("pitch").toFloat()
                    )
                    manager.setTeamSpawn(teamId, loc)
                } else {
                    plugin.logger.warning("World '${spawnSection.getString("world")}' not loaded — skipping spawn for $teamId")
                }
            }
        }

        plugin.logger.info("Loaded team data: $restoredMembers member(s) restored")
    }

    fun save() {
        val config = YamlConfiguration()

        for ((teamId, team) in manager.teams) {
            val base = "teams.$teamId"
            config.set("$base.members", manager.getTeamMembers(teamId).map { it.toString() })

            team.spawn?.let { loc ->
                config.set("$base.spawn.world", loc.world?.name)
                config.set("$base.spawn.x", loc.x)
                config.set("$base.spawn.y", loc.y)
                config.set("$base.spawn.z", loc.z)
                config.set("$base.spawn.yaw", loc.yaw.toDouble())
                config.set("$base.spawn.pitch", loc.pitch.toDouble())
            }
        }

        try {
            if (!plugin.dataFolder.exists()) plugin.dataFolder.mkdirs()
            config.save(file)
        } catch (e: Exception) {
            plugin.logger.warning("Failed to save teams.yml: ${e.message}")
        }
    }
}