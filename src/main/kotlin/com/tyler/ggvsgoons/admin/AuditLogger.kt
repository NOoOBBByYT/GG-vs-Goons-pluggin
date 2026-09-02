package com.tyler.ggvsgoons.admin

import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.logging.Logger

/**
 * Handles audit logging for all admin actions
 */
class AuditLogger(
    private val logFile: File,
    private val logger: Logger
) {
    private val entries = ConcurrentLinkedQueue<AuditEntry>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    private var retentionDays = 90
    
    // Configuration
    private var logPlayerActions = true
    private var logConfigChanges = true
    private var logModActions = true
    
    init {
        // Ensure log file exists
        if (!logFile.exists()) {
            logFile.parentFile?.mkdirs()
            logFile.createNewFile()
        }
        
        // Load existing entries from file
        loadEntriesFromFile()
    }
    
    /**
     * Update configuration
     */
    fun updateConfig(retentionDays: Int, logPlayerActions: Boolean, logConfigChanges: Boolean, logModActions: Boolean) {
        this.retentionDays = retentionDays
        this.logPlayerActions = logPlayerActions
        this.logConfigChanges = logConfigChanges
        this.logModActions = logModActions
    }
    
    /**
     * Log an audit entry
     */
    fun log(entry: AuditEntry) {
        // Check if this type of action should be logged
        val shouldLog = when (entry.actionType) {
            ModActionType.CONFIG_CHANGE -> logConfigChanges
            ModActionType.FREEZE_PLAYER, ModActionType.UNFREEZE_PLAYER,
            ModActionType.FORCE_FREE_PRISONER, ModActionType.KICK_FROM_TEAM,
            ModActionType.KICK_FROM_FACTION, ModActionType.RESET_COOLDOWN -> logModActions
            else -> true
        }
        
        if (!shouldLog) return
        
        entries.add(entry)
        writeToFile(entry)
        
        // Log to console as well
        logger.info("[AUDIT] ${entry.format()}")
    }
    
    /**
     * Log a configuration change
     */
    fun logConfigChange(change: ConfigChange) {
        if (!logConfigChanges) return
        
        val entry = AuditEntry(
            adminId = change.adminId,
            adminName = change.adminName,
            actionType = ModActionType.CONFIG_CHANGE,
            target = change.path,
            details = "Changed from '${change.oldValue}' to '${change.newValue}'${change.reason?.let { " | Reason: $it" } ?: ""}",
            success = true
        )
        log(entry)
    }
    
    /**
     * Log a moderation action
     */
    fun logModAction(action: ModerationAction, adminName: String, targetName: String? = null, success: Boolean = true) {
        if (!logModActions) return
        
        val entry = AuditEntry(
            adminId = action.adminId,
            adminName = adminName,
            actionType = action.actionType,
            target = targetName,
            details = action.reason + (action.duration?.let { " | Duration: ${it}s" } ?: ""),
            success = success
        )
        log(entry)
    }
    
    /**
     * Log a player action (if enabled)
     */
    fun logPlayerAction(playerId: UUID, playerName: String, action: String, details: String) {
        if (!logPlayerActions) return
        
        val entry = AuditEntry(
            adminId = playerId,
            adminName = playerName,
            actionType = ModActionType.CONFIG_CHANGE, // Generic type for player actions
            target = null,
            details = "$action | $details",
            success = true
        )
        log(entry)
    }
    
    /**
     * Get recent audit entries
     */
    fun getRecentEntries(limit: Int = 50): List<AuditEntry> {
        return entries.toList().takeLast(limit).reversed()
    }
    
    /**
     * Get entries by page
     */
    fun getEntriesByPage(page: Int, pageSize: Int = 20): List<AuditEntry> {
        val allEntries = entries.toList().reversed()
        val startIndex = page * pageSize
        val endIndex = minOf(startIndex + pageSize, allEntries.size)
        
        return if (startIndex < allEntries.size) {
            allEntries.subList(startIndex, endIndex)
        } else {
            emptyList()
        }
    }
    
    /**
     * Search audit log
     */
    fun search(query: String): List<AuditEntry> {
        val lowerQuery = query.lowercase()
        return entries.filter { entry ->
            entry.adminName.lowercase().contains(lowerQuery) ||
            entry.target?.lowercase()?.contains(lowerQuery) == true ||
            entry.details.lowercase().contains(lowerQuery) ||
            entry.actionType.displayName.lowercase().contains(lowerQuery)
        }.toList().reversed()
    }
    
    /**
     * Filter entries by action type
     */
    fun filterByType(actionType: ModActionType): List<AuditEntry> {
        return entries.filter { it.actionType == actionType }.toList().reversed()
    }
    
    /**
     * Filter entries by admin
     */
    fun filterByAdmin(adminId: UUID): List<AuditEntry> {
        return entries.filter { it.adminId == adminId }.toList().reversed()
    }
    
    /**
     * Filter entries by time range
     */
    fun filterByTimeRange(startTime: Long, endTime: Long): List<AuditEntry> {
        return entries.filter { it.timestamp in startTime..endTime }.toList().reversed()
    }
    
    /**
     * Get total entry count
     */
    fun getTotalEntries(): Int = entries.size
    
    /**
     * Write an entry to the log file
     */
    private fun writeToFile(entry: AuditEntry) {
        try {
            PrintWriter(FileWriter(logFile, true)).use { writer ->
                writer.println(entry.format())
            }
        } catch (e: Exception) {
            logger.severe("Failed to write audit log entry: ${e.message}")
        }
    }
    
    /**
     * Load existing entries from file
     */
    private fun loadEntriesFromFile() {
        if (!logFile.exists()) return
        
        try {
            logFile.readLines().forEach { line ->
                // Parse line if needed for searching
                // For now, we just keep recent entries in memory
            }
        } catch (e: Exception) {
            logger.warning("Failed to load audit log entries: ${e.message}")
        }
    }
    
    /**
     * Clean up old entries based on retention policy
     */
    fun cleanupOldEntries() {
        val cutoffTime = System.currentTimeMillis() - (retentionDays * 24 * 60 * 60 * 1000L)
        
        // Remove old entries from memory
        val iterator = entries.iterator()
        var removedCount = 0
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.timestamp < cutoffTime) {
                iterator.remove()
                removedCount++
            }
        }
        
        if (removedCount > 0) {
            logger.info("Cleaned up $removedCount old audit log entries")
        }
        
        // Optionally rotate log file if it gets too large
        if (logFile.length() > 10 * 1024 * 1024) { // 10MB
            rotateLogFile()
        }
    }
    
    /**
     * Rotate the log file
     */
    private fun rotateLogFile() {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            val rotatedFile = File(logFile.parentFile, "${logFile.nameWithoutExtension}_$timestamp.log")
            logFile.renameTo(rotatedFile)
            logFile.createNewFile()
            logger.info("Rotated audit log file to ${rotatedFile.name}")
        } catch (e: Exception) {
            logger.severe("Failed to rotate audit log file: ${e.message}")
        }
    }
    
    /**
     * Export to plain text
     */
    private fun exportToText(outputFile: File): Boolean {
        PrintWriter(FileWriter(outputFile)).use { writer ->
            writer.println("=== GGvGoons Audit Log Export ===")
            writer.println("Generated: ${dateFormat.format(Date())}")
            writer.println("Total Entries: ${entries.size}")
            writer.println("=" .repeat(50))
            writer.println()
            
            entries.forEach { entry ->
                writer.println(entry.format())
            }
        }
        return true
    }
    
    /**
     * Export to CSV
     */
    private fun exportToCSV(outputFile: File): Boolean {
        PrintWriter(FileWriter(outputFile)).use { writer ->
            writer.println("Timestamp,Admin ID,Admin Name,Action Type,Target,Details,Success")
            
            entries.forEach { entry ->
                val timestamp = dateFormat.format(Date(entry.timestamp))
                val target = entry.target ?: ""
                val details = entry.details.replace(",", ";").replace("\n", " ")
                writer.println("$timestamp,${entry.adminId},${entry.adminName},${entry.actionType.displayName},$target,$details,${entry.success}")
            }
        }
        return true
    }
    
    /**
     * Export to JSON
     */
    private fun exportToJSON(outputFile: File): Boolean {
        PrintWriter(FileWriter(outputFile)).use { writer ->
            writer.println("{")
            writer.println("  \"export_date\": \"${dateFormat.format(Date())}\",")
            writer.println("  \"total_entries\": ${entries.size},")
            writer.println("  \"entries\": [")
            
            entries.forEachIndexed { index, entry ->
                writer.println("    {")
                writer.println("      \"id\": \"${entry.id}\",")
                writer.println("      \"timestamp\": ${entry.timestamp},")
                writer.println("      \"admin_id\": \"${entry.adminId}\",")
                writer.println("      \"admin_name\": \"${entry.adminName}\",")
                writer.println("      \"action_type\": \"${entry.actionType.name}\",")
                writer.println("      \"target\": ${entry.target?.let { "\"$it\"" } ?: "null"},")
                writer.println("      \"details\": \"${entry.details.replace("\"", "\\\"")}\",")
                writer.println("      \"success\": ${entry.success}")
                writer.print("    }")
                if (index < entries.size - 1) writer.println(",")
                else writer.println()
            }
            
            writer.println("  ]")
            writer.println("}")
        }
        return true
    }
    
    /**
     * Export audit log to a file
     */
    fun export(outputFile: File, format: String = "txt"): Boolean {
        return try {
            when (format.lowercase()) {
                "txt" -> exportToText(outputFile)
                "csv" -> exportToCSV(outputFile)
                "json" -> exportToJSON(outputFile)
                else -> {
                    logger.warning("Unknown export format: $format")
                    false
                }
            }
        } catch (e: Exception) {
            logger.severe("Failed to export audit log: ${e.message}")
            false
        }
    }
}
