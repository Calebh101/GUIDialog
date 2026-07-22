package org.calebh101.gUIDialog

import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class ActionStore(private val plugin: GUIDialog) {
    private val file = File(plugin.dataFolder, "data.yml")
    private val config = YamlConfiguration.loadConfiguration(file)

    fun save(map: Map<String, String>) {
        for ((key, value) in map) config.set(key, value)
        config.save(file)
    }

    fun get(key: String): String? {
        return config.getString(key)
    }

    fun delete(key: String) {
        config.set(key, null)
        config.save(file)
    }

    fun contains(path: String): Boolean {
        return config.contains(path)
    }

    fun load(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        for (key in config.getKeys(false)) map[key] = config.getString(key) ?: continue
        return map
    }
}