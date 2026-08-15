package com.app.apkcleanermanager

/**
 * Yeni AdShield modu, klasik araçtan ayrı bir karar katmanıdır. Bir reklam ağı ancak
 * DEX içinde görüldüğünde ve ayrıca manifestte veya birden fazla DEX dosyasında doğrulandığında
 * yama kapsamına alınır. Bu çifte kanıt kuralı, uygulamaya ait belirsiz sınıfların değiştirilmesini
 * önler; asset/kütüphane silme işlemi bilinçli olarak uygulanmaz.
 */
enum class AdCleaningMode { LEGACY, AD_SHIELD }

data class AdShieldSdkImpact(
  val id: String,
  val label: String,
  val references: Int,
  val dexFiles: List<String>,
  val manifestReferences: Int,
)

data class AdShieldPreview(
  val verified: List<AdShieldSdkImpact>,
  val rejectedDetectedCount: Int,
  val affectedDexCount: Int,
  val manifestWillBePatched: Boolean,
)

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

  fun preview(profiles: List<CleanerProfile>, detectionCounts: Map<String, Int>, manifestHits: Map<String, Int>, dexRows: List<DexRow>): AdShieldPreview {
    val verifiedIds = verifiedProfileIds(detectionCounts, manifestHits, dexRows)
    val verified = verifiedIds.mapNotNull { id ->
      val profile = profiles.firstOrNull { it.id == id } ?: return@mapNotNull null
      val dexFiles = dexRows.filter { id in it.profiles }.map { it.name }
      AdShieldSdkImpact(id, profile.label, detectionCounts[id] ?: 0, dexFiles, manifestHits[id] ?: 0)
    }.sortedByDescending { it.references }
    return AdShieldPreview(
      verified = verified,
      rejectedDetectedCount = (detectionCounts.keys - verifiedIds).size,
      affectedDexCount = dexRows.count { row -> row.profiles.any { it in verifiedIds } },
      manifestWillBePatched = verified.any { it.manifestReferences > 0 },
    )
  }
}
