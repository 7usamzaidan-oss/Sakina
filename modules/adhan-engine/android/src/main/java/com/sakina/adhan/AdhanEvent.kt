package com.sakina.adhan

import org.json.JSONArray
import org.json.JSONObject

/** One scheduled adhan: when it fires and what the notification says. */
data class AdhanEvent(val id: Int, val triggerAtMillis: Long, val title: String, val body: String) {
  fun toJson(): JSONObject = JSONObject()
    .put("id", id).put("triggerAtMillis", triggerAtMillis).put("title", title).put("body", body)

  companion object {
    fun fromJson(o: JSONObject) = AdhanEvent(
      o.getInt("id"), o.getLong("triggerAtMillis"), o.getString("title"), o.optString("body", "")
    )

    fun listToJson(events: List<AdhanEvent>): String =
      JSONArray().apply { events.forEach { put(it.toJson()) } }.toString()

    fun listFromJson(json: String?): List<AdhanEvent> {
      if (json.isNullOrEmpty()) return emptyList()
      val arr = JSONArray(json)
      return (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
    }
  }
}
