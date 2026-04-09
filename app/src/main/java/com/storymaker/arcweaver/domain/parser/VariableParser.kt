package com.storymaker.arcweaver.domain.parser

import org.json.JSONObject

class VariableParser {

    fun parse(json: String?): Map<String, Any> {
        if (json.isNullOrBlank()) return emptyMap()

        val result = mutableMapOf<String, Any>()

        return try {
            val jsonObject = JSONObject(json)

            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = jsonObject.get(key)

                result[key] = value
            }

            result
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun toJson(map: Map<String, Any>): String {
        return try {
            val jsonObject = JSONObject()

            map.forEach { (key, value) ->
                jsonObject.put(key, value)
            }

            jsonObject.toString()
        } catch (e: Exception) {
            "{}"
        }
    }
}