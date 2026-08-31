# Build Configuration Recommendations for Arclight

## Current Status

The current [`build.gradle.kts`](../build.gradle.kts) is already compatible with Arclight. No changes are strictly required.

## Optional Enhancements

If you want to add explicit Arclight API support for enhanced compatibility features, you can optionally add the Arclight repository:

### Recommended Changes (Optional)

```kotlin
repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    // Optional: Arclight API repository for enhanced hybrid server compatibility
    maven("https://maven.izzel.io/releases/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    // Optional: Add Arclight API for enhanced Forge integration
    // compileOnly("io.izzel.arclight:arclight-api:1.21-1.0.0")
}
```

### Why This is Optional

1. **Current plugin works without it**: The plugin uses only standard Bukkit API, which Arclight already translates
2. **Arclight API is for advanced features**: Only needed if you want to:
   - Directly access Forge events from the plugin
   - Use Arclight-specific APIs for mod detection
   - Implement custom Bukkit-to-Forge translations

3. **Keep it simple**: For most use cases, the current configuration is sufficient

### When to Add Arclight API

Consider adding the Arclight API dependency when implementing:
- **Phase 4: Combat Integration Module** - Direct Forge event listeners
- **Mod detection features** - Checking which Forge mods are loaded
- **Custom event bridges** - Translating between Bukkit and Forge events

### Current Configuration Analysis

✅ **Already correct:**
- Kotlin JVM plugin with version 1.9.24
- Shadow plugin for creating fat jars
- Paper API 1.21.1 (compatible with Arclight)
- JVM toolchain set to Java 21
- Proper resource processing for plugin.yml
- Shadow jar configuration

❌ **Removed from README:**
- CommandAPI references (not actually used in the code)

## Conclusion

**No changes needed to build.gradle.kts at this time.** The current configuration is fully compatible with both Paper and Arclight servers. The optional Arclight API can be added later when implementing Phase 4 features.
