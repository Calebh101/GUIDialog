package org.calebh101.gUIDialog
import com.google.gson.Gson
import com.google.gson.JsonObject

data class Dialog(val title: String, val body: String, val actions: Map<String, String>) {
    fun build(): String {
        val json = JsonObject()

        json.addProperty("title", title)
        json.addProperty("body", body)
        json.add("actions", Gson().toJsonTree(actions))

        return json.toString()
    }
}