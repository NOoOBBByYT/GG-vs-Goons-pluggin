package com.tyler.ggvsgoons.admin

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import java.util.logging.Logger

/**
 * Manages configuration backups and restoration
 */
class BackupManager(
    private val dataFolder: File,
    private val logger: Logger
) {
    
    private val backupDirectory: File
    private var maxBackups = 10
    private var autoBackupEnabled = false
    private var autoBackupInterval = 3600 // seconds
    
    init {
        backupDirectory = File(dataFolder, "backups")
        if (!backupDirectory.exists()) {
            backupDirectory.mkdirs()
        }
    }
    
    /**
     * Update configuration
     */
    fun updateConfig(maxBackups: Int, autoBackupEnabled: Boolean, autoBackupInterval: Int) {
        this.maxBackups = maxBackups
        this.autoBackupEnabled = autoBackupEnabled
        this.autoBackupInterval = autoBackupInterval
    }
    
    /**
     * Create a backup
     */
    fun createBackup(name: String? = null, createdBy: UUID? = null, description: String? = null): Result<BackupInfo> {
        return try {
            val timestamp = System.currentTimeMillis()
            val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date(timestamp))
            val backupName = name ?: "backup_$dateStr"
            val backupFile = File(backupDirectory, "$backupName.zip")
            
            // Create zip file
            ZipOutputStream(FileOutputStream(backupFile)).use { zos ->
                // Add config.yml
                addFileToZip(zos, File(dataFolder, "config.yml"), "config.yml")
                
                // Add persistence files
                addFileToZip(zos, File(dataFolder, "prisoners.yml"), "prisoners.yml")
                addFileToZip(zos, File(dataFolder, "teams.yml"), "teams.yml")
                addFileToZip(zos, File(dataFolder, "factions.yml"), "factions.yml")
                
                // Add audit log if it exists
                addFileToZip(zos, File(dataFolder, "audit.log"), "audit.log")
            }
            
            val backupInfo = BackupInfo(
                name = backupName,
                timestamp = timestamp,
                size = backupFile.length(),
                createdBy = createdBy,
                description = description
            )
            
            logger.info("Created backup: $backupName (${backupFile.length()} bytes)")
            
            // Clean up old backups
            cleanupOldBackups()
            
            Result.success(backupInfo)
        } catch (e: Exception) {
            logger.severe("Failed to create backup: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Add a file to a zip archive
     */
    private fun addFileToZip(zos: ZipOutputStream, file: File, entryName: String) {
        if (!file.exists()) {
            return
        }
        
        try {
            zos.putNextEntry(ZipEntry(entryName))
            FileInputStream(file).use { fis ->
                fis.copyTo(zos)
            }
            zos.closeEntry()
        } catch (e: Exception) {
            logger.warning("Failed to add file to backup: ${file.name}")
        }
    }
    
    /**
     * List all backups
     */
    fun listBackups(): List<BackupInfo> {
        val backups = mutableListOf<BackupInfo>()
        
        backupDirectory.listFiles()?.forEach { file ->
            if (file.extension == "zip") {
                val name = file.nameWithoutExtension
                backups.add(BackupInfo(
                    name = name,
                    timestamp = file.lastModified(),
                    size = file.length(),
                    createdBy = null,
                    description = null
                ))
            }
        }
        
        return backups.sortedByDescending { it.timestamp }
    }
    
    /**
     * Restore from a backup
     */
    fun restoreBackup(name: String): Result<Unit> {
        return try {
            val backupFile = File(backupDirectory, "$name.zip")
            if (!backupFile.exists()) {
                return Result.failure(IllegalArgumentException("Backup not found: $name"))
            }
            
            // Create a safety backup before restoring
            createBackup("pre_restore_${System.currentTimeMillis()}", null, "Automatic backup before restore")
            
            // Extract zip file
            ZipInputStream(FileInputStream(backupFile)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val file = File(dataFolder, entry.name)
                    
                    if (!entry.isDirectory) {
                        file.parentFile?.mkdirs()
                        FileOutputStream(file).use { fos ->
                            zis.copyTo(fos)
                        }
                    }
                    
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            
            logger.info("Restored backup: $name")
            Result.success(Unit)
        } catch (e: Exception) {
            logger.severe("Failed to restore backup: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Delete a backup
     */
    fun deleteBackup(name: String): Result<Unit> {
        return try {
            val backupFile = File(backupDirectory, "$name.zip")
            if (!backupFile.exists()) {
                return Result.failure(IllegalArgumentException("Backup not found: $name"))
            }
            
            if (backupFile.delete()) {
                logger.info("Deleted backup: $name")
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("Failed to delete backup file"))
            }
        } catch (e: Exception) {
            logger.severe("Failed to delete backup: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Clean up old backups based on retention policy
     */
    private fun cleanupOldBackups() {
        val backups = listBackups()
        
        if (backups.size > maxBackups) {
            val toDelete = backups.drop(maxBackups)
            toDelete.forEach { backup ->
                deleteBackup(backup.name)
            }
            logger.info("Cleaned up ${toDelete.size} old backup(s)")
        }
    }
    
    /**
     * Check if auto-backup is enabled
     */
    fun isAutoBackupEnabled(): Boolean = autoBackupEnabled
    
    /**
     * Get auto-backup interval in seconds
     */
    fun getAutoBackupInterval(): Int = autoBackupInterval
}
