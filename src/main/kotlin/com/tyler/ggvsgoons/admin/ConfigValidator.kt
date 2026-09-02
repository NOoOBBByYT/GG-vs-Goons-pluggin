package com.tyler.ggvsgoons.admin

/**
 * Validates configuration values before they are applied
 */
class ConfigValidator {
    
    /**
     * Validate an integer value
     */
    private fun validateInt(value: Any, min: Int? = null, max: Int? = null): ValidationResult {
        val intValue = when (value) {
            is Int -> value
            is String -> value.toIntOrNull() ?: return ValidationResult.Invalid("Value must be an integer")
            is Number -> value.toInt()
            else -> return ValidationResult.Invalid("Value must be an integer")
        }
        
        if (min != null && intValue < min) {
            return ValidationResult.Invalid("Value must be at least $min")
        }
        
        if (max != null && intValue > max) {
            return ValidationResult.Invalid("Value must be at most $max")
        }
        
        return ValidationResult.Valid(intValue)
    }
    
    /**
     * Validate a boolean value
     */
    private fun validateBoolean(value: Any): ValidationResult {
        val boolValue = when (value) {
            is Boolean -> value
            is String -> when (value.lowercase()) {
                "true", "yes", "on", "1" -> true
                "false", "no", "off", "0" -> false
                else -> return ValidationResult.Invalid("Value must be true or false")
            }
            else -> return ValidationResult.Invalid("Value must be true or false")
        }
        
        return ValidationResult.Valid(boolValue)
    }
    
    /**
     * Validate a string value
     */
    private fun validateString(value: Any, minLength: Int? = null, maxLength: Int? = null): ValidationResult {
        val strValue = value.toString()
        
        if (minLength != null && strValue.length < minLength) {
            return ValidationResult.Invalid("Value must be at least $minLength characters")
        }
        
        if (maxLength != null && strValue.length > maxLength) {
            return ValidationResult.Invalid("Value must be at most $maxLength characters")
        }
        
        return ValidationResult.Valid(strValue)
    }
    
    /**
     * Validate a double value
     */
    private fun validateDouble(value: Any, min: Double? = null, max: Double? = null): ValidationResult {
        val doubleValue = when (value) {
            is Double -> value
            is String -> value.toDoubleOrNull() ?: return ValidationResult.Invalid("Value must be a number")
            is Number -> value.toDouble()
            else -> return ValidationResult.Invalid("Value must be a number")
        }
        
        if (min != null && doubleValue < min) {
            return ValidationResult.Invalid("Value must be at least $min")
        }
        
        if (max != null && doubleValue > max) {
            return ValidationResult.Invalid("Value must be at most $max")
        }
        
        return ValidationResult.Valid(doubleValue)
    }
    
    /**
     * Validate a configuration value based on its path and expected type
     */
    fun validate(path: String, value: Any): ValidationResult {
        return when {
            // Prisoner settings
            path == "prisoner.offer-expiry-timeout" -> validateInt(value, min = 0, max = 3600)
            path == "prisoner.enable-persistence" -> validateBoolean(value)
            
            // Team settings
            path == "teams.allow-team-switching" -> validateBoolean(value)
            path == "teams.team-switch-cooldown" -> validateInt(value, min = 0, max = 86400)
            path == "teams.chat.enabled" -> validateBoolean(value)
            path == "teams.chat.prefix" -> validateString(value, maxLength = 20)
            path == "teams.chat.format" -> validateString(value, maxLength = 100)
            
            // Faction settings
            path == "factions.enabled" -> validateBoolean(value)
            path == "factions.max-factions-per-team" -> validateInt(value, min = 1, max = 10)
            path == "factions.creation.min-team-members" -> validateInt(value, min = 1, max = 50)
            path == "factions.members.default-max-members" -> validateInt(value, min = 2, max = 100)
            path == "factions.members.allow-expansion" -> validateBoolean(value)
            path == "factions.members.max-expansion-limit" -> validateInt(value, min = 2, max = 200)
            path == "factions.homes.enabled" -> validateBoolean(value)
            path == "factions.homes.teleport-warmup" -> validateInt(value, min = 0, max = 60)
            path == "factions.homes.teleport-cooldown" -> validateInt(value, min = 0, max = 86400)
            path == "factions.homes.cancel-on-damage" -> validateBoolean(value)
            path == "factions.chat.enabled" -> validateBoolean(value)
            path == "factions.chat.prefix" -> validateString(value, maxLength = 20)
            path == "factions.enable-persistence" -> validateBoolean(value)
            path == "factions.invite-expiry-seconds" -> validateInt(value, min = 10, max = 600)
            
            // Admin settings
            path == "admin.enabled" -> validateBoolean(value)
            path == "admin.audit.enabled" -> validateBoolean(value)
            path == "admin.audit.retention-days" -> validateInt(value, min = 1, max = 365)
            path == "admin.audit.log-player-actions" -> validateBoolean(value)
            path == "admin.audit.log-config-changes" -> validateBoolean(value)
            path == "admin.audit.log-mod-actions" -> validateBoolean(value)
            path == "admin.backups.enabled" -> validateBoolean(value)
            path == "admin.backups.auto-backup" -> validateBoolean(value)
            path == "admin.backups.auto-backup-interval" -> validateInt(value, min = 60, max = 86400)
            path == "admin.backups.max-backups" -> validateInt(value, min = 1, max = 100)
            path == "admin.commands.allow-admin-bypass" -> validateBoolean(value)
            path == "admin.permissions.use-luckperms" -> validateBoolean(value)
            path == "admin.permissions.fallback-to-internal" -> validateBoolean(value)
            
            // Discord settings
            path == "discord.enabled" -> validateBoolean(value)
            path == "discord.connection.websocket.enabled" -> validateBoolean(value)
            path == "discord.connection.websocket.host" -> validateString(value, maxLength = 255)
            path == "discord.connection.websocket.port" -> validateInt(value, min = 1, max = 65535)
            path == "discord.connection.websocket.reconnect-delay" -> validateInt(value, min = 1, max = 300)
            path == "discord.chat-bridge.enabled" -> validateBoolean(value)
            path == "discord.chat-bridge.team-chat.enabled" -> validateBoolean(value)
            path == "discord.notifications.prisoners.enabled" -> validateBoolean(value)
            path == "discord.notifications.wars.enabled" -> validateBoolean(value)
            path == "discord.notifications.server.enabled" -> validateBoolean(value)
            path == "discord.statistics.enabled" -> validateBoolean(value)
            path == "discord.statistics.sync-interval" -> validateInt(value, min = 30, max = 3600)
            
            // Permission settings
            path == "permissions.enabled" -> validateBoolean(value)
            
            else -> ValidationResult.Invalid("Unknown configuration path: $path")
        }
    }
    
    /**
     * Get the expected type for a configuration path
     */
    fun getExpectedType(path: String): ConfigValueType {
        return when {
            path.endsWith(".enabled") -> ConfigValueType.BOOLEAN
            path.contains("timeout") || path.contains("cooldown") || 
            path.contains("interval") || path.contains("delay") ||
            path.contains("warmup") || path.contains("max-") ||
            path.contains("min-") || path.contains("port") ||
            path.contains("retention-days") -> ConfigValueType.INTEGER
            path.endsWith(".prefix") || path.endsWith(".format") ||
            path.endsWith(".host") || path.endsWith(".message") -> ConfigValueType.STRING
            else -> ConfigValueType.UNKNOWN
        }
    }
    
    /**
     * Check if a configuration path exists and is valid
     */
    fun isValidPath(path: String): Boolean {
        val validPaths = setOf(
            // Prisoner
            "prisoner.offer-expiry-timeout",
            "prisoner.enable-persistence",
            // Teams
            "teams.allow-team-switching",
            "teams.team-switch-cooldown",
            "teams.chat.enabled",
            "teams.chat.prefix",
            "teams.chat.format",
            // Factions
            "factions.enabled",
            "factions.max-factions-per-team",
            "factions.creation.min-team-members",
            "factions.members.default-max-members",
            "factions.members.allow-expansion",
            "factions.members.max-expansion-limit",
            "factions.homes.enabled",
            "factions.homes.teleport-warmup",
            "factions.homes.teleport-cooldown",
            "factions.homes.cancel-on-damage",
            "factions.chat.enabled",
            "factions.chat.prefix",
            "factions.enable-persistence",
            "factions.invite-expiry-seconds",
            // Admin
            "admin.enabled",
            "admin.audit.enabled",
            "admin.audit.retention-days",
            "admin.audit.log-player-actions",
            "admin.audit.log-config-changes",
            "admin.audit.log-mod-actions",
            "admin.backups.enabled",
            "admin.backups.auto-backup",
            "admin.backups.auto-backup-interval",
            "admin.backups.max-backups",
            "admin.commands.allow-admin-bypass",
            "admin.permissions.use-luckperms",
            "admin.permissions.fallback-to-internal",
            // Discord
            "discord.enabled",
            "discord.connection.websocket.enabled",
            "discord.connection.websocket.host",
            "discord.connection.websocket.port",
            "discord.connection.websocket.reconnect-delay",
            "discord.chat-bridge.enabled",
            "discord.chat-bridge.team-chat.enabled",
            "discord.notifications.prisoners.enabled",
            "discord.notifications.wars.enabled",
            "discord.notifications.server.enabled",
            "discord.statistics.enabled",
            "discord.statistics.sync-interval",
            // Permissions
            "permissions.enabled"
        )
        
        return validPaths.contains(path)
    }
}

