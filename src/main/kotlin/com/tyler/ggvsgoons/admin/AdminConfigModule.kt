package com.tyler.ggvsgoons.admin

import com.tyler.ggvsgoons.GGvGModule
import com.tyler.ggvsgoons.GGvGPlugin
import org.bukkit.Bukkit
import java.io.File

/**
 * Module for the admin configuration and moderation system
 */
class AdminConfigModule(private val plugin: GGvGPlugin) : GGvGModule {
    
    lateinit var configManager: AdminConfigManager
        private set
    
    lateinit var moderationManager: ModerationManager
        private set
    
    lateinit var permissionManager: PermissionManager
        private set
    
    lateinit var backupManager: BackupManager
        private set
    
    lateinit var auditLogger: AuditLogger
        private set
    
    private lateinit var validator: ConfigValidator
    
    private var cleanupTaskId: Int = -1
    private var autoBackupTaskId: Int = -1
    
    override fun register(plugin: GGvGPlugin) {
        // Check if admin system is enabled
        if (!plugin.config.getBoolean("admin.enabled", true)) {
            plugin.logger.info("Admin configuration system is disabled")
            return
        }
        
        // Initialize validator
        validator = ConfigValidator()
        
        // Initialize audit logger
        val auditFile = File(plugin.dataFolder, plugin.config.getString("admin.audit.log-file", "audit.log") ?: "audit.log")
        auditLogger = AuditLogger(auditFile, plugin.logger)
        
        // Load audit configuration
        val retentionDays = plugin.config.getInt("admin.audit.retention-days", 90)
        val logPlayerActions = plugin.config.getBoolean("admin.audit.log-player-actions", true)
        val logConfigChanges = plugin.config.getBoolean("admin.audit.log-config-changes", true)
        val logModActions = plugin.config.getBoolean("admin.audit.log-mod-actions", true)
        auditLogger.updateConfig(retentionDays, logPlayerActions, logConfigChanges, logModActions)
        
        // Initialize config manager
        configManager = AdminConfigManager(plugin, validator, auditLogger)
        
        // Initialize moderation manager
        moderationManager = ModerationManager(plugin, auditLogger)
        
        // Initialize permission manager
        val useLuckPerms = plugin.config.getBoolean("admin.permissions.use-luckperms", true)
        permissionManager = PermissionManager(plugin, useLuckPerms)
        
        // Initialize backup manager
        backupManager = BackupManager(plugin.dataFolder, plugin.logger)
        val maxBackups = plugin.config.getInt("admin.backups.max-backups", 10)
        val autoBackup = plugin.config.getBoolean("admin.backups.auto-backup", true)
        val autoBackupInterval = plugin.config.getInt("admin.backups.auto-backup-interval", 3600)
        backupManager.updateConfig(maxBackups, autoBackup, autoBackupInterval)
        
        // Register admin command
        val adminCommand = AdminCommand(
            plugin,
            configManager,
            moderationManager,
            permissionManager,
            backupManager,
            auditLogger
        )
        plugin.getCommand("ggadmin")?.setExecutor(adminCommand)
        plugin.getCommand("ggadmin")?.tabCompleter = adminCommand
        
        // Schedule cleanup task for expired permissions (every 5 minutes)
        cleanupTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, {
            permissionManager.cleanupExpiredPermissions()
            auditLogger.cleanupOldEntries()
        }, 6000L, 6000L) // 5 minutes in ticks
        
        // Schedule auto-backup task if enabled
        if (autoBackup) {
            val intervalTicks = (autoBackupInterval * 20).toLong() // Convert seconds to ticks
            autoBackupTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, {
                plugin.logger.info("Running automatic backup...")
                val result = backupManager.createBackup(null, null, "Automatic backup")
                result.onSuccess { info ->
                    plugin.logger.info("Automatic backup created: ${info.name}")
                }.onFailure { error ->
                    plugin.logger.severe("Automatic backup failed: ${error.message}")
                }
            }, intervalTicks, intervalTicks)
        }
        
        plugin.logger.info("Admin configuration module enabled")
    }
    
    /**
     * Called when the plugin is disabled
     */
    fun onDisable() {
        // Cancel scheduled tasks
        if (cleanupTaskId != -1) {
            Bukkit.getScheduler().cancelTask(cleanupTaskId)
        }
        if (autoBackupTaskId != -1) {
            Bukkit.getScheduler().cancelTask(autoBackupTaskId)
        }
        
        // Final cleanup
        auditLogger.cleanupOldEntries()
        
        // Save configuration
        configManager.saveConfig()
    }
}
