package org.calebh101.gUIDialog
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlin.random.Random

data class Dialog(val title: String, val body: String, val actions: Map<String, String>, val id: Long) {
    fun build(): String {
        val json = JsonObject()

        json.addProperty("title", title)
        json.addProperty("body", body)
        json.add("actions", Gson().toJsonTree(actions))
        json.addProperty("id", id)

        return json.toString()
    }
}

data class DialogPayload(val title: String, val body: String, val actions: Map<String, String>) {
    fun toDialog(): Dialog {
        return Dialog(title, body, actions, id = Random.nextLong())
    }
}