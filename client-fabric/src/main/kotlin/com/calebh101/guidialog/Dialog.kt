package com.calebh101.guidialog

import com.google.gson.Gson
import com.google.gson.JsonObject

data class Dialog(val title: String, val body: String, val actions: Map<String, String> /* pretty to ID */) {}