package org.calebh101.gUIDialog

import com.google.gson.Gson
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

class DialogStore(private val plugin: GUIDialog) {
    private val file = File(plugin.dataFolder, "dialogs.yml")
    private val config = YamlConfiguration.loadConfiguration(file)

    fun save(map: Map<String, DialogPayload>) {
        for ((key, value) in map) config.set(key, value.build())
        config.save(file)
    }

    fun get(key: String): DialogPayload? {
        val data = config.getString(key)
        return if (data == null) null else Gson().fromJson(data, DialogPayload::class.java)
    }

    fun delete(key: String) {
        config.set(key, null)
        config.save(file)
    }

    fun contains(path: String): Boolean {
        return config.contains(path)
    }

    fun load(): Map<String, DialogPayload> {
        val map = mutableMapOf<String, DialogPayload>()
        for (key in config.getKeys(false)) map[key] = if (config.contains(key)) Gson().fromJson(config.getString(key), DialogPayload::class.java) else continue
        return map
    }
}