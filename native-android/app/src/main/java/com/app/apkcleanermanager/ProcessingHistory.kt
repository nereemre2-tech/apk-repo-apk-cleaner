package com.app.apkcleanermanager

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ProcessingHistoryEntry(
  val id: Long,
  val sourceName: String,
  val profile: String,
  val status: String,
  val timestamp: Long,
  val outputPath: String?,
  val summary: String,
  val logs: List<String>,
)

class ProcessingHistory(context: Context) {
  private val preferences = context.getSharedPreferences("apk-cleaner-history", Context.MODE_PRIVATE)

  fun entries(): List<ProcessingHistoryEntry> = try {
    val array = JSONArray(preferences.getString(KEY, "[]"))
    (0 until array.length()).map { index ->
      val value = array.getJSONObject(index)
      ProcessingHistoryEntry(
        id = value.getLong("id"),
        sourceName = value.optString("sourceName", "Bilinmeyen paket"),
        profile = value.optString("profile", "balanced"),
        status = value.optString("status", "İşlem kaydı"),
        timestamp = value.optLong("timestamp", 0),
        outputPath = value.optString("outputPath").takeIf { it.isNotBlank() },
        summary = value.optString("summary"),
        logs = value.optJSONArray("logs")?.let { logs -> (0 until logs.length()).map(logs::getString) } ?: emptyList(),
      )
    }
  } catch (_: Throwable) {
    emptyList()
  }

  fun add(entry: ProcessingHistoryEntry) {
    val next = listOf(entry) + entries()
    val array = JSONArray()
    next.take(MAX_ENTRIES).forEach { item ->
      array.put(JSONObject().apply {
        put("id", item.id)
        put("sourceName", item.sourceName)
        put("profile", item.profile)
        put("status", item.status)
        put("timestamp", item.timestamp)
        put("outputPath", item.outputPath ?: "")
        put("summary", item.summary)
        put("logs", JSONArray(item.logs))
      })
    }
    preferences.edit().putString(KEY, array.toString()).apply()
  }

  fun clear() = preferences.edit().remove(KEY).apply()

  companion object {
    private const val KEY = "entries"
    private const val MAX_ENTRIES = 25
  }
}
