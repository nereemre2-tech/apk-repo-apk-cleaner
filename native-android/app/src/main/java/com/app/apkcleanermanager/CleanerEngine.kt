package com.app.apkcleanermanager

import android.content.Context
import android.os.Environment
import com.reandroid.apkeditor.Main
import local.apkcleaner.dex.DirectDexPatcher
import local.apkcleaner.xml.BinaryManifestPatcher
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

data class CleanerProfile(
  val id: String,
  val label: String,
  val descriptors: List<String>,
  val manifest: List<String>,
  val metadata: List<String>,
  val assets: List<String>,
  val libraries: List<String>,
)

data class Detection(val id: String, val label: String, val references: Int)
data class DexRow(val name: String, val profiles: Set<String>)
data class Analysis(
  val filename: String,
  val bytes: Long,
  val sha256: String,
  val dexCount: Int,
  val detections: List<Detection>,
  val split: Boolean,
  val modules: List<String>,
)

data class ProcessingResult(
  val output: File,
  val dexPatched: Int,
  val manifestPatched: Int,
  val removedFiles: Int,
  val splitMerged: Boolean,
)

private data class Scan(
  val dexRows: List<DexRow>,
  val detectionCounts: Map<String, Int>,
  val manifestHits: Map<String, Int>,
)

class CleanerEngine(private val context: Context) {
  private val splitExtensions = setOf("apks", "apkm", "xapk")
  private val profiles: List<CleanerProfile> by lazy { loadProfiles() }
  private val toolRunner = IsolatedToolRunner(context.applicationContext)

  fun analyze(input: File, displayName: String): Analysis {
    require(isZip(input)) { "Dosya geçerli bir APK/ZIP paketi değil." }
    val isSplit = extensionOf(displayName) in splitExtensions
    val modules = if (isSplit) splitModules(input) else emptyList()
    val scan = if (isSplit) scanSplit(input) else scanApk(input)
    val detected = scan.detectionCounts.entries.mapNotNull { (id, count) ->
      profiles.firstOrNull { it.id == id }?.let { Detection(id, it.label, count) }
    }.sortedByDescending { it.references }
    return Analysis(displayName, input.length(), sha256(input), scan.dexRows.size, detected, isSplit, modules)
  }

  fun process(input: File, sourceName: String, profile: String, patchAds: Boolean, stripDebug: Boolean): ProcessingResult {
    require(profile in setOf("safe", "balanced", "deep")) { "Bilinmeyen temizlik profili." }
    val work = File(context.cacheDir, "apk-cleaner-${System.currentTimeMillis()}").apply { mkdirs() }
    try {
      val sourceIsSplit = extensionOf(sourceName) in splitExtensions
      var working = input
      if (sourceIsSplit) {
        val merged = File(work, "universal.apk")
        toolRunner.run(IsolatedToolService.COMMAND_SPLIT, listOf("m", "-i", input.absolutePath, "-o", merged.absolutePath, "-clean-meta"), 180)
        require(merged.isFile) { "Split paket tek APK’ya dönüştürülemedi." }
        working = merged
      }
      val scan = scanApk(working)
      val selectedProfiles = scan.detectionCounts.keys
      if (patchAds && selectedProfiles.isEmpty()) error("Bilinen reklam SDK’sı bulunamadı; reklam yaması uygulanmadı.")

      val replacements = linkedMapOf<String, File>()
      var dexPatched = 0
      if (patchAds || stripDebug) {
        ZipFile(working).use { archive ->
          scan.dexRows.filter { stripDebug || (patchAds && it.profiles.isNotEmpty()) }.forEachIndexed { index, row ->
            val dexIn = File(work, "input-$index.dex")
            archive.getInputStream(archive.getEntry(row.name)).use { it.copyTo(dexIn.outputStream()) }
            val dexOut = File(work, "patched-$index.dex")
            val args = mutableListOf("--input", dexIn.absolutePath, "--output", dexOut.absolutePath, "--mode", profile, "--strip-debug", stripDebug.toString())
            if (patchAds) profiles.filter { it.id in selectedProfiles }.flatMap { it.descriptors }.distinct().forEach { descriptor ->
              args.add("--descriptor")
              args.add(descriptor)
            }
            toolRunner.run(IsolatedToolService.COMMAND_DEX, args)
            require(dexOut.isFile) { "DEX yama motoru çıktı dosyası üretmedi." }
            replacements[row.name] = dexOut
            dexPatched += 1
          }
        }
      }

      var manifestPatched = 0
      if (patchAds && profile != "safe" && scan.manifestHits.isNotEmpty()) {
        val manifestIn = File(work, "AndroidManifest-input.bin")
        ZipFile(working).use { archive ->
          archive.getEntry("AndroidManifest.xml")?.let { entry -> archive.getInputStream(entry).use { it.copyTo(manifestIn.outputStream()) } }
        }
        if (manifestIn.isFile) {
          val manifestOut = File(work, "AndroidManifest-output.bin")
          val args = mutableListOf("--input", manifestIn.absolutePath, "--output", manifestOut.absolutePath)
          profiles.filter { it.id in selectedProfiles }.forEach { item ->
            item.manifest.forEach { args.addAll(listOf("--prefix", it)) }
            item.metadata.forEach { args.addAll(listOf("--metadata", it)) }
          }
          listOf(
            "com.google.android.gms.permission.AD_ID", "android.permission.AD_ID",
            "android.permission.ACCESS_ADSERVICES_AD_ID", "android.permission.ACCESS_ADSERVICES_ATTRIBUTION",
            "android.permission.ACCESS_ADSERVICES_TOPICS", "android.permission.AD_SERVICES_CONFIG",
          ).forEach { args.addAll(listOf("--permission", it)) }
          toolRunner.run(IsolatedToolService.COMMAND_MANIFEST, args)
          if (manifestOut.isFile) {
            replacements["AndroidManifest.xml"] = manifestOut
            manifestPatched = 1
          }
        }
      }

      val removed = if (patchAds && profile == "deep") deepRemovals(working, selectedProfiles) else emptySet()
      val unsigned = if (replacements.isNotEmpty() || removed.isNotEmpty()) {
        File(work, "unsigned.apk").also { rewriteApk(working, it, replacements, removed) }
      } else working
      val outputDirectory = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "APK Cleaner").apply { mkdirs() }
      val base = safeName(sourceName.substringBeforeLast('.', sourceName))
      val label = when {
        sourceIsSplit && !patchAds && !stripDebug -> "universal"
        patchAds -> "clean-$profile"
        else -> "optimized"
      }
      val output = signApk(unsigned, outputDirectory, "$base-$label.apk", work)
      return ProcessingResult(output, dexPatched, manifestPatched, removed.size, sourceIsSplit)
    } finally {
      work.deleteRecursively()
    }
  }

  private fun scanApk(file: File): Scan {
    require(isZip(file)) { "Dosya geçerli bir APK/ZIP paketi değil." }
    val detections = mutableMapOf<String, Int>()
    val manifestHits = mutableMapOf<String, Int>()
    val dexRows = mutableListOf<DexRow>()
    ZipFile(file).use { archive ->
      val entries = archive.entries().asSequence().toList()
      val dexEntries = entries.filter { Regex("(^|.*/)classes\\d*\\.dex$", RegexOption.IGNORE_CASE).matches(it.name) }
      require(dexEntries.isNotEmpty()) { "APK içinde classes*.dex bulunamadı." }
      dexEntries.forEach { entry ->
        val bytes = archive.getInputStream(entry).use { it.readBytes() }
        val matched = profiles.filter { profile ->
          val count = profile.descriptors.sumOf { countOccurrences(bytes, it.toByteArray()) }
          if (count > 0) detections[profile.id] = (detections[profile.id] ?: 0) + count
          count > 0
        }.map { it.id }.toSet()
        dexRows.add(DexRow(entry.name, matched))
      }
      archive.getEntry("AndroidManifest.xml")?.let { entry ->
        val bytes = archive.getInputStream(entry).use { it.readBytes() }
        profiles.forEach { profile ->
          val markers = profile.manifest + profile.metadata
          val count = markers.sumOf { marker -> countOccurrences(bytes, marker.toByteArray()) + countOccurrences(bytes, marker.toByteArray(Charsets.UTF_16LE)) }
          if (count > 0) manifestHits[profile.id] = count
        }
      }
    }
    return Scan(dexRows, detections, manifestHits)
  }

  private fun scanSplit(file: File): Scan {
    val rows = mutableListOf<DexRow>()
    val detectionCounts = mutableMapOf<String, Int>()
    val manifestHits = mutableMapOf<String, Int>()
    val scanFolder = File(context.cacheDir, "apk-cleaner-split-scan-${System.currentTimeMillis()}").apply { mkdirs() }
    try {
      ZipFile(file).use { archive ->
        archive.entries().asSequence().filter { !it.isDirectory && it.name.endsWith(".apk", true) }.forEachIndexed { index, entry ->
          val extracted = File(scanFolder, "$index-${safeName(entry.name)}")
          archive.getInputStream(entry).use { it.copyTo(extracted.outputStream()) }
          if (isZip(extracted)) {
            val scan = scanApk(extracted)
            rows += scan.dexRows.map { it.copy(name = "${entry.name}:${it.name}") }
            scan.detectionCounts.forEach { (id, count) -> detectionCounts[id] = (detectionCounts[id] ?: 0) + count }
            scan.manifestHits.forEach { (id, count) -> manifestHits[id] = (manifestHits[id] ?: 0) + count }
          }
        }
      }
    } finally { scanFolder.deleteRecursively() }
    require(rows.isNotEmpty()) { "Split paket içinde incelenebilir APK modülü bulunamadı." }
    return Scan(rows, detectionCounts, manifestHits)
  }

  private fun splitModules(file: File): List<String> = ZipFile(file).use { archive ->
    archive.entries().asSequence().filter { !it.isDirectory && it.name.endsWith(".apk", true) }.map { it.name }.toList()
  }

  private fun deepRemovals(file: File, ids: Set<String>): Set<String> {
    val markers = profiles.filter { it.id in ids }.flatMap { it.assets + it.libraries }.map(String::lowercase)
    if (markers.isEmpty()) return emptySet()
    return ZipFile(file).use { archive -> archive.entries().asSequence().filter { entry ->
      val name = entry.name.lowercase()
      (name.startsWith("assets/") || name.startsWith("lib/")) && markers.any(name::contains)
    }.map(ZipEntry::getName).toSet() }
  }

  private fun rewriteApk(source: File, destination: File, replacements: Map<String, File>, removals: Set<String>) {
    ZipFile(source).use { archive ->
      ZipOutputStream(FileOutputStream(destination)).use { output ->
        archive.entries().asSequence().forEach { entry ->
          val upper = entry.name.uppercase()
          val signature = upper.startsWith("META-INF/") && upper.endsWith(".RSA", true) || upper.endsWith(".DSA", true) || upper.endsWith(".EC", true) || upper.endsWith(".SF", true) || upper.endsWith("MANIFEST.MF", true)
          if (signature || entry.name in removals) return@forEach
          output.putNextEntry(ZipEntry(entry.name).apply { time = entry.time })
          (replacements[entry.name]?.inputStream() ?: archive.getInputStream(entry)).use { it.copyTo(output) }
          output.closeEntry()
        }
      }
    }
  }

  private fun signApk(unsigned: File, destinationDirectory: File, outputName: String, work: File): File {
    val keyStore = File(work, "output-key.jks")
    context.assets.open("apk-cleaner-output.jks").use { input -> keyStore.outputStream().use { input.copyTo(it) } }
    val signedDirectory = File(work, "signed").apply { mkdirs() }
    val args = arrayOf(
      "--apks", unsigned.absolutePath, "--out", signedDirectory.absolutePath, "--allowResign", "--skipZipAlign",
      "--ks", keyStore.absolutePath, "--ksAlias", "androiddebugkey", "--ksPass", "android", "--ksKeyPass", "android",
    )
    toolRunner.run(IsolatedToolService.COMMAND_SIGN, args.toList(), 180)
    val signed = signedDirectory.listFiles()?.filter { it.extension.equals("apk", true) }?.maxByOrNull { it.lastModified() }
      ?: error("APK imzalama aracı çıktı üretmedi.")
    return File(destinationDirectory, safeName(outputName)).also { target -> FileInputStream(signed).use { input -> target.outputStream().use { input.copyTo(it) } } }
  }

  private fun loadProfiles(): List<CleanerProfile> {
    val root = JSONObject(context.assets.open("profiles.json").bufferedReader().use { it.readText() })
    return root.keys().asSequence().asIterable().map { id ->
      val item = root.getJSONObject(id)
      fun values(name: String) = item.optJSONArray(name)?.let { array -> (0 until array.length()).map { array.getString(it) } } ?: emptyList()
      CleanerProfile(id, item.getString("label"), values("descriptors"), values("manifest"), values("metadata"), values("assets"), values("libraries"))
    }.toList()
  }

  private fun sha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
      val buffer = ByteArray(1024 * 1024)
      while (true) { val read = input.read(buffer); if (read <= 0) break; digest.update(buffer, 0, read) }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
  }

  private fun countOccurrences(haystack: ByteArray, needle: ByteArray): Int {
    if (needle.isEmpty() || haystack.size < needle.size) return 0
    var result = 0; var index = 0
    while (index <= haystack.size - needle.size) {
      var equal = true
      for (offset in needle.indices) if (haystack[index + offset] != needle[offset]) { equal = false; break }
      if (equal) { result++; index += needle.size } else index++
    }
    return result
  }

  private fun isZip(file: File) = try { ZipFile(file).use { true } } catch (_: Throwable) { false }
  private fun extensionOf(name: String) = name.substringAfterLast('.', "").lowercase()
  private fun safeName(value: String) = value.replace(Regex("[^A-Za-z0-9._-]+"), "_").takeLast(120)
}
