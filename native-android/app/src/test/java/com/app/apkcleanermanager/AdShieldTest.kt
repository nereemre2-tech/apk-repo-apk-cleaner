package com.app.apkcleanermanager

import org.junit.Assert.assertEquals
import org.junit.Test

class AdShieldTest {
  @Test
  fun `cifte kanitli ag secilir`() {
    val result = AdShield.verifiedProfileIds(
      detectionCounts = mapOf("google_ads" to 4),
      manifestHits = mapOf("google_ads" to 1),
      dexRows = listOf(DexRow("classes.dex", setOf("google_ads"))),
    )
    assertEquals(setOf("google_ads"), result)
  }

  @Test
  fun `tek zayif isaret uygulama guvenligi icin reddedilir`() {
    val result = AdShield.verifiedProfileIds(
      detectionCounts = mapOf("google_ads" to 1, "applovin" to 3),
      manifestHits = emptyMap(),
      dexRows = listOf(DexRow("classes.dex", setOf("google_ads", "applovin"))),
    )
    assertEquals(emptySet<String>(), result)
  }

  @Test
  fun `iki dexte gorunen ag manifest olmadan da secilir`() {
    val result = AdShield.verifiedProfileIds(
      detectionCounts = mapOf("unity_ads" to 3),
      manifestHits = emptyMap(),
      dexRows = listOf(DexRow("classes.dex", setOf("unity_ads")), DexRow("classes2.dex", setOf("unity_ads"))),
    )
    assertEquals(setOf("unity_ads"), result)
  }
}
