package com.app.apkcleanermanager

/**
 * Yeni AdShield modu, klasik araçtan ayrı bir karar katmanıdır. Bir reklam ağı ancak
 * DEX içinde görüldüğünde ve ayrıca manifestte veya birden fazla DEX dosyasında doğrulandığında
 * yama kapsamına alınır. Bu çifte kanıt kuralı, uygulamaya ait belirsiz sınıfların değiştirilmesini
 * önler; asset/kütüphane silme işlemi bilinçli olarak uygulanmaz.
 */
enum class AdCleaningMode { LEGACY, AD_SHIELD }

object AdShield {
  fun verifiedProfileIds(detectionCounts: Map<String, Int>, manifestHits: Map<String, Int>, dexRows: List<DexRow>): Set<String> {
    val dexCoverage = mutableMapOf<String, Int>()
    dexRows.forEach { row -> row.profiles.forEach { id -> dexCoverage[id] = (dexCoverage[id] ?: 0) + 1 } }
    return detectionCounts.keys.filter { id ->
      val references = detectionCounts[id] ?: 0
      val manifestEvidence = manifestHits[id] ?: 0
      val dexEvidence = dexCoverage[id] ?: 0
      references >= 2 && (manifestEvidence > 0 || dexEvidence >= 2)
    }.toSet()
  }
}
