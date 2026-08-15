package com.app.apkcleanermanager

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.Space
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.util.concurrent.Executors

class MainActivity : Activity() {
  private val engine by lazy { CleanerEngine(this) }
  private val worker = Executors.newSingleThreadExecutor()
  private lateinit var container: LinearLayout
  private var selectedFile: File? = null
  private var sourceName = ""
  private var analysis: Analysis? = null
  private var processing: ProcessingResult? = null
  private var selectedProfile = "balanced"
  private var patchAds = true
  private var stripDebug = false
  private var busy = false
  private var cancellationRequested = false
  private var processingPercent = 0
  private var processingMessage = "Yerel işlem motoru hazırlanıyor"
  private val processingLogs = mutableListOf<String>()
  private var progressValueView: TextView? = null
  private var progressBar: ProgressBar? = null
  private var progressMessageView: TextView? = null
  private var progressLogView: TextView? = null
  private var cancelButton: Button? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    render()
  }

  override fun onDestroy() {
    worker.shutdownNow()
    super.onDestroy()
  }

  private fun render() {
    val scroll = ScrollView(this).apply { setBackgroundColor(COLOR_BACKGROUND); isFillViewport = true }
    container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(15), dp(16), dp(30)) }
    scroll.addView(container)
    setContentView(scroll)
    topbar()
    gap(22)
    badge("◆  YEREL ANDROID PAKET İŞLEME STÜDYOSU")
    heading("Paketini bırak.", 32, COLOR_INK)
    heading("Temiz, tek APK olarak al.", 24, COLOR_LIME)
    paragraph("APK reklam izlerini cihazında temizle; APKS, APKM ve XAPK paketlerini tek kurulabilir APK’ya dönüştür.")
    metrics()
    gap(18)
    when {
      analysis == null -> { stepper(1); dropzone() }
      busy -> working()
      else -> analysisScreen()
    }
    processing?.let { resultScreen(it) }
    if (!busy && processing == null && processingLogs.isNotEmpty()) {
      actionButton("İşlem günlüğünü TXT olarak paylaş", COLOR_GREEN, true) { exportProcessingLog() }
    }
    gap(14)
    communityCard()
    gap(10)
    card(COLOR_WARNING, 16) {
      addView(label("Yetkili kullanım", 14, COLOR_WARNING_TEXT, true))
      addView(label("Yalnızca sahibi olduğunuz veya değiştirme/test etme yetkiniz bulunan paketleri işleyin. Çıktı APK’sı yerel sertifikayla yeniden imzalanır.", 12, COLOR_WARNING_TEXT))
    }
  }

  private fun topbar() {
    val bar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; background = shape(COLOR_SURFACE, COLOR_LINE, 15); setPadding(dp(10), dp(10), dp(10), dp(10)) }
    val logo = ImageView(this).apply { setImageResource(R.drawable.logo_ac); scaleType = ImageView.ScaleType.CENTER_CROP; background = shape(COLOR_DARK, COLOR_GREEN, 10) }
    bar.addView(logo, LinearLayout.LayoutParams(dp(40), dp(40)))
    val names = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(10), 0, 0, 0) }
    names.addView(label("APK Cleaner", 15, COLOR_INK, true)); names.addView(label("STUDIO", 9, COLOR_MUTED, true))
    bar.addView(names, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
    bar.addView(label("●  HAZIR", 10, COLOR_GREEN, true))
    add(bar)
  }

  private fun metrics() {
    val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(0, dp(13), 0, 0) }
    listOf("18\nreklam ağı", "4\npaket türü", "%100\ncihazda işlem").forEach { value ->
      row.addView(label(value, 10, COLOR_MUTED, true).apply { gravity = Gravity.CENTER; background = shape(COLOR_SURFACE, COLOR_LINE, 99); setPadding(dp(8), dp(7), dp(8), dp(7)) }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(3), 0, dp(3), 0) })
    }
    add(row)
  }

  private fun stepper(active: Int) {
    val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; background = shape(COLOR_SOFT, COLOR_LINE, 16); setPadding(dp(5), dp(8), dp(5), dp(8)) }
    listOf("Paket", "Yapılandır", "İşle", "İndir").forEachIndexed { index, title ->
      val filled = index + 1 <= active
      val part = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER }
      part.addView(label("${index + 1}", 11, if (filled) COLOR_DARK else COLOR_MUTED, true).apply { gravity = Gravity.CENTER; background = shape(if (filled) COLOR_LIME else COLOR_SURFACE, if (filled) COLOR_LIME else COLOR_LINE, 99); setPadding(dp(8), dp(5), dp(8), dp(5)) })
      part.addView(label(title, 9, if (filled) COLOR_INK else COLOR_MUTED, true).apply { gravity = Gravity.CENTER; setPadding(0, dp(4), 0, 0) })
      row.addView(part, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
    }
    add(row)
  }

  private fun dropzone() {
    gap(11)
    card(COLOR_SOFT, 20) {
      addView(label("●  GÜVENLİ YEREL İŞLEM", 10, COLOR_GREEN, true))
      addView(label("↑", 42, COLOR_LIME, true).apply { setPadding(0, dp(12), 0, 0) })
      addView(label(if (busy) "Paket hazırlanıyor ve taranıyor…" else "İşlenecek paketi seç", 22, COLOR_INK, true))
      addView(label("APK · APKS · APKM · XAPK\nEn fazla 1 GB · Dosyan yalnızca bu cihazda işlenir", 13, COLOR_MUTED))
    }
    actionButton(if (busy) "Analiz sürüyor…" else "Dosya seç", COLOR_LIME, !busy) { pickPackage() }
  }

  private fun analysisScreen() {
    val report = analysis ?: return
    stepper(2)
    gap(15)
    heading("Uygulanacak işlemi seç", 21, COLOR_INK)
    paragraph("${humanSize(report.bytes)} · ${report.dexCount} DEX · SHA-256 ${report.sha256.take(12)}…")
    card(COLOR_SOFT, 16) {
      addView(label(if (report.split) "Split paket bulundu" else "Standart APK bulundu", 15, COLOR_INK, true))
      addView(label(if (report.split) "${report.modules.size} modül tek APK çıktısı için birleştirilecektir. Modül seçimi otomatik uygulanır." else "Paket yerel arşiv yapısı üzerinden güvenli biçimde tarandı.", 12, COLOR_MUTED))
    }
    operation("✦", "Reklam izlerini temizle", "Doğrulanmış DEX çağrılarını ve uygun manifest kayıtlarını düzenler.", patchAds, report.detections.isNotEmpty()) { patchAds = true; render() }
    if (report.split) operation("⇄", "Tek APK oluştur", "Split modülleri tek kurulabilir APK’da birleştirir; reklam yaması isteğe bağlıdır.", !patchAds, true) { patchAds = false; render() }
    if (report.detections.isEmpty()) {
      card(COLOR_NOTICE, 15) {
        addView(label("Bilinen reklam ağı algılanmadı", 14, COLOR_NOTICE_TEXT, true))
        addView(label("İsterseniz DEX hata ayıklama verisini temizleyebilir veya split paketi tek APK’ya dönüştürebilirsiniz.", 12, COLOR_NOTICE_TEXT))
      }
    } else {
      heading("Algılanan ağlar", 17, COLOR_INK)
      card(COLOR_SURFACE, 15) { report.detections.forEach { row(it.label, "${it.references} referans", COLOR_GREEN) } }
    }
    heading("Temizlik kapsamını belirle", 20, COLOR_INK)
    val profiles = RadioGroup(this).apply { orientation = RadioGroup.VERTICAL; background = shape(COLOR_SURFACE, COLOR_LINE, 16); setPadding(dp(8), dp(6), dp(8), dp(6)) }
    listOf(
      Triple("safe", "Güvenli", "Yalnızca doğrulanmış çağrıları etkisizleştirir."),
      Triple("balanced", "Dengeli", "DEX düzenlemesine manifest adaylarını ekler."),
      Triple("deep", "Kapsamlı", "Doğrulanmış asset ve kütüphane kalıntılarını hedefler."),
    ).forEach { (id, title, detail) ->
      val button = RadioButton(this).apply { this.id = View.generateViewId(); tag = id; text = "$title\n$detail"; textSize = 13f; setTextColor(COLOR_INK); buttonTintList = ColorStateList.valueOf(COLOR_LIME); setPadding(dp(6), dp(10), dp(6), dp(10)) }
      profiles.addView(button); if (id == selectedProfile) profiles.check(button.id)
    }
    profiles.setOnCheckedChangeListener { group, checked -> selectedProfile = group.findViewById<RadioButton>(checked)?.tag as? String ?: "balanced" }
    add(profiles)
    option(if (report.split) "Dönüştürme sırasında reklam yaması" else "Reklam yaması", "Bilinen ağ referanslarında güvenli DEX yaması uygular.", patchAds, report.detections.isNotEmpty()) { patchAds = it }
    option("DEX hata ayıklama verisi", "Kaynak, satır ve yerel değişken kayıtlarını temizler.", stripDebug, true) { stripDebug = it }
    val caption = if (report.split && !patchAds && !stripDebug) "APK oluşturmayı başlat" else if (patchAds) "Temizlemeyi başlat" else "Paketi yeniden imzala"
    actionButton(caption, COLOR_LIME, !patchAds || report.detections.isNotEmpty()) { startProcessing() }
  }

  private fun operation(symbol: String, title: String, detail: String, selected: Boolean, enabled: Boolean, action: () -> Unit) {
    gap(9)
    val item = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; alpha = if (enabled) 1f else .45f; background = shape(if (selected) COLOR_SELECTED else COLOR_SURFACE, if (selected) COLOR_LIME else COLOR_LINE, 16); setPadding(dp(14), dp(14), dp(14), dp(14)); setOnClickListener { if (enabled) action() } }
    item.addView(label(symbol, 25, if (selected) COLOR_DARK else COLOR_GREEN, true).apply { gravity = Gravity.CENTER; background = shape(if (selected) COLOR_LIME else COLOR_SOFT, COLOR_LINE, 12) }, LinearLayout.LayoutParams(dp(46), dp(46)))
    val copy = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), 0, dp(8), 0) }
    copy.addView(label(title, 14, COLOR_INK, true)); copy.addView(label(detail, 11, COLOR_MUTED))
    item.addView(copy, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
    item.addView(label(if (selected) "●" else "○", 20, if (selected) COLOR_LIME else COLOR_MUTED, true))
    add(item)
  }

  private fun working() {
    stepper(3)
    gap(12)
    card(COLOR_SOFT, 20) {
      addView(label("YEREL İŞLEM SÜRÜYOR", 10, COLOR_GREEN, true))
      val progressRow = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(12), 0, 0) }
      val indicator = ProgressBar(this@MainActivity, null, android.R.attr.progressBarStyleHorizontal).apply { isIndeterminate = false; max = 100; progress = processingPercent; progressTintList = ColorStateList.valueOf(COLOR_LIME); progressBackgroundTintList = ColorStateList.valueOf(COLOR_LINE) }
      progressRow.addView(indicator, LinearLayout.LayoutParams(0, dp(10), 1f))
      val value = label("%$processingPercent", 18, COLOR_LIME, true).apply { gravity = Gravity.CENTER; setPadding(dp(12), 0, 0, 0) }
      progressRow.addView(value)
      addView(progressRow)
      addView(label(processingMessage, 23, COLOR_INK, true).also { progressMessageView = it })
      addView(label("DEX, manifest ve paket imzalama adımları tamamlanana kadar bu ekranı açık bırak. Orijinal paket değiştirilmez.", 13, COLOR_MUTED))
      addView(label("●  İşlem motoru ayrı bir güvenli süreçte çalışıyor", 11, COLOR_GREEN, true).apply { setPadding(0, dp(12), 0, 0) })
      addView(label(processingLogs.joinToString("\n"), 11, COLOR_CONSOLE).apply { typeface = Typeface.MONOSPACE; background = shape(COLOR_DARK, COLOR_LINE, 11); setPadding(dp(12), dp(10), dp(12), dp(10)); setLineSpacing(dp(3).toFloat(), 1f); setTextIsSelectable(true); also { progressLogView = it } }.also { log -> log.setTextColor(COLOR_CONSOLE) })
      progressValueView = value
      progressBar = indicator
    }
    val cancel = Button(this).apply {
      text = if (cancellationRequested) "İptal ediliyor…" else "İşlemi iptal et"
      textSize = 14f
      typeface = Typeface.DEFAULT_BOLD
      isAllCaps = false
      isEnabled = !cancellationRequested
      setTextColor(COLOR_WARNING_TEXT)
      background = shape(COLOR_WARNING, COLOR_WARNING_TEXT, 12)
      setOnClickListener { requestCancellation() }
    }
    add(cancel, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(50)).apply { setMargins(0, dp(12), 0, 0) })
    cancelButton = cancel
  }

  private fun resultScreen(result: ProcessingResult) {
    stepper(4)
    gap(12)
    card(COLOR_DARK, 20) {
      addView(label("✓  İŞLEM TAMAMLANDI", 11, COLOR_LIME, true))
      addView(label(result.output.name, 17, Color.WHITE, true))
      addView(label("${result.dexPatched} DEX · ${result.manifestPatched} manifest · ${result.removedFiles} dosya değişikliği", 12, Color.rgb(184, 196, 214)))
    }
    actionButton("APK’yı kaydet / paylaş", COLOR_LIME, true) { share(result.output) }
    actionButton("İşlem günlüğünü TXT olarak paylaş", COLOR_GREEN, true) { exportProcessingLog() }
  }

  private fun communityCard() {
    val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; background = shape(COLOR_SURFACE, COLOR_LINE, 16); setPadding(dp(12), dp(12), dp(12), dp(12)) }
    val icon = ImageView(this).apply { setImageResource(R.drawable.apk_repo_icon); scaleType = ImageView.ScaleType.CENTER_CROP; background = shape(COLOR_SOFT, COLOR_GREEN, 12) }
    row.addView(icon, LinearLayout.LayoutParams(dp(48), dp(48)))
    val text = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(11), 0, 0, 0) }
    text.addView(label("APK Repo Grubu", 14, COLOR_INK, true))
    text.addView(label("Yerel araçlar · görünür işlem akışı", 11, COLOR_MUTED))
    row.addView(text, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
    add(row)
  }

  private fun pickPackage() {
    startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "*/*" }, REQUEST_PICK)
  }

  @Deprecated("Deprecated in Java")
  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode != REQUEST_PICK || resultCode != RESULT_OK) return
    val uri = data?.data ?: return
    val name = queryName(uri) ?: "selected.apk"
    if (name.substringAfterLast('.', "").lowercase() !in setOf("apk", "apks", "apkm", "xapk")) { Toast.makeText(this, "APK, APKS, APKM veya XAPK uzantılı bir paket seçin.", Toast.LENGTH_LONG).show(); return }
    busy = true; analysis = null; processing = null; render()
    worker.execute {
      try {
        val destination = File(filesDir, "jobs/${System.currentTimeMillis()}-${name.replace(Regex("[^A-Za-z0-9._-]+"), "_")}").apply { parentFile?.mkdirs() }
        contentResolver.openInputStream(uri)?.use { input -> destination.outputStream().use { input.copyTo(it) } } ?: error("Seçilen dosya okunamadı.")
        val report = engine.analyze(destination, name)
        runOnUiThread { selectedFile = destination; sourceName = name; analysis = report; patchAds = report.detections.isNotEmpty(); busy = false; render() }
      } catch (error: Throwable) { runOnUiThread { busy = false; render(); showError("Analiz tamamlanamadı", error.message) } }
    }
  }

  private fun startProcessing() {
    val file = selectedFile ?: return
    busy = true
    processing = null
    cancellationRequested = false
    processingPercent = 4
    processingMessage = "Yerel işlem motoru hazırlanıyor"
    processingLogs.clear()
    processingLogs += "✓ Orijinal paket korunuyor"
    processingLogs += "● Yerel motor için ayrı çalışma süreci başlatıldı"
    render()
    worker.execute {
      try {
        val output = engine.process(file, sourceName, selectedProfile, patchAds, stripDebug) { update ->
          runOnUiThread { updateProcessingViews(update.percent, update.message) }
        }
        runOnUiThread { processing = output; busy = false; render() }
      } catch (error: Throwable) {
        runOnUiThread {
          busy = false
          if (engine.wasCancellationRequested()) {
            processingLogs += "■ İşlem kullanıcı tarafından iptal edildi; geçici dosyalar temizlendi"
            render()
            showError("İşlem iptal edildi", "Çıktı APK oluşturulmadı. Orijinal paket korunmuştur.")
          } else {
            render()
            showError("İşlem tamamlanamadı", error.message)
          }
        }
      }
    }
  }

  private fun requestCancellation() {
    if (!busy || cancellationRequested) return
    cancellationRequested = true
    cancelButton?.isEnabled = false
    cancelButton?.text = "İptal ediliyor…"
    updateProcessingViews(processingPercent, "İptal isteği alındı; aktif araç güvenli biçimde durduruluyor")
    engine.cancelActiveWork()
  }

  private fun exportProcessingLog() {
    try {
      val folder = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "APK Cleaner/Logs").apply { mkdirs() }
      val base = sourceName.substringBeforeLast('.', "package").replace(Regex("[^A-Za-z0-9._-]+"), "_")
      val report = File(folder, "$base-islem-gunlugu-${System.currentTimeMillis()}.txt")
      val content = buildString {
        appendLine("APK Cleaner Manager — İşlem Günlüğü")
        appendLine("Kaynak paket: ${if (sourceName.isBlank()) "Bilinmiyor" else sourceName}")
        appendLine("Profil: $selectedProfile")
        appendLine("Reklam yaması: ${if (patchAds) "Etkin" else "Kapalı"}")
        appendLine("DEX hata ayıklama temizliği: ${if (stripDebug) "Etkin" else "Kapalı"}")
        appendLine("Durum: ${if (processing != null) "Tamamlandı" else if (cancellationRequested) "İptal edildi" else "İşlem kaydı"}")
        appendLine()
        processingLogs.forEach(::appendLine)
      }
      report.writeText(content)
      val uri = FileProvider.getUriForFile(this, "com.app.apkcleanermanager.files", report)
      startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TITLE, report.name)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }, "İşlem günlüğünü kaydet veya paylaş"))
    } catch (error: Throwable) {
      showError("Günlük dışa aktarılamadı", error.message)
    }
  }

  private fun updateProcessingViews(percent: Int, message: String) {
    processingPercent = percent.coerceAtLeast(processingPercent).coerceAtMost(100)
    processingMessage = message
    processingLogs += "● $message"
    while (processingLogs.size > 8) processingLogs.removeAt(0)
    progressValueView?.text = "%$processingPercent"
    progressBar?.progress = processingPercent
    progressMessageView?.text = processingMessage
    progressLogView?.text = processingLogs.joinToString("\n")
  }

  private fun share(file: File) {
    try {
      val uri = FileProvider.getUriForFile(this, "com.app.apkcleanermanager.files", file)
      startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "application/vnd.android.package-archive"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "Oluşturulan APK’yı kaydet veya paylaş"))
    } catch (error: Throwable) { showError("Paylaşım kullanılamıyor", error.message) }
  }

  private fun queryName(uri: Uri): String? = contentResolver.query(uri, null, null, null, null)?.use { cursor -> val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME); if (cursor.moveToFirst() && index >= 0) cursor.getString(index) else null }
  private fun option(title: String, detail: String, value: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    gap(9)
    val row = LinearLayout(this).apply {
      orientation = LinearLayout.HORIZONTAL
      gravity = Gravity.CENTER_VERTICAL
      background = shape(COLOR_SURFACE, COLOR_LINE, 15)
      setPadding(dp(14), dp(12), dp(14), dp(12))
    }
    val copy = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    copy.addView(label(title, 14, COLOR_INK, true))
    copy.addView(label(detail, 11, COLOR_MUTED))
    row.addView(copy, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
    row.addView(Switch(this).apply {
      isChecked = value
      isEnabled = enabled
      thumbTintList = ColorStateList.valueOf(COLOR_LIME)
      setOnCheckedChangeListener { _, checked -> onChange(checked) }
    })
    add(row)
  }
  private fun row(title: String, side: String, sideColor: Int) { val item = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(7), 0, dp(7)) }; item.addView(label(title, 14, COLOR_INK, true), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)); item.addView(label(side, 11, sideColor, true)); add(item) }
  private fun card(color: Int, radius: Int, content: LinearLayout.() -> Unit) { gap(9); val view = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; background = shape(color, COLOR_LINE, radius); setPadding(dp(15), dp(14), dp(15), dp(14)); content() }; add(view) }
  private fun actionButton(title: String, color: Int, enabled: Boolean, action: () -> Unit) { gap(10); add(Button(this).apply { text = title; textSize = 14f; typeface = Typeface.DEFAULT_BOLD; isAllCaps = false; isEnabled = enabled; setTextColor(COLOR_DARK); background = shape(color, color, 12); setOnClickListener { action() } }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(52))) }
  private fun heading(value: String, size: Int, color: Int) { add(label(value, size, color, true)) }
  private fun paragraph(value: String, color: Int = COLOR_MUTED) { add(label(value, 13, color)) }
  private fun badge(value: String) { add(label(value, 10, COLOR_GREEN, true)) }
  private fun label(value: String, size: Int, color: Int, bold: Boolean = false) = TextView(this).apply { text = value; textSize = size.toFloat(); setTextColor(color); if (bold) typeface = Typeface.DEFAULT_BOLD; setLineSpacing(dp(3).toFloat(), 1f) }
  private fun add(view: View, params: LinearLayout.LayoutParams? = null) { container.addView(view, params ?: LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)) }
  private fun gap(height: Int) { add(Space(this), LinearLayout.LayoutParams(1, dp(height))) }
  private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
  private fun shape(fill: Int, stroke: Int, radius: Int) = GradientDrawable().apply { setColor(fill); setStroke(dp(1), stroke); cornerRadius = dp(radius).toFloat() }
  private fun humanSize(bytes: Long) = if (bytes < 1024 * 1024) "%.1f KB".format(bytes / 1024.0) else "%.1f MB".format(bytes / 1024.0 / 1024.0)
  private fun showError(title: String, message: String?) { AlertDialog.Builder(this).setTitle(title).setMessage(message ?: "Beklenmeyen bir hata oluştu.").setPositiveButton("Tamam", null).show() }

  companion object {
    private const val REQUEST_PICK = 1001
    private val COLOR_DARK = Color.rgb(13, 21, 21)
    private val COLOR_BACKGROUND = Color.rgb(8, 14, 12)
    private val COLOR_SURFACE = Color.rgb(16, 24, 21)
    private val COLOR_SOFT = Color.rgb(20, 30, 26)
    private val COLOR_SELECTED = Color.rgb(22, 42, 32)
    private val COLOR_LINE = Color.rgb(45, 59, 54)
    private val COLOR_INK = Color.rgb(238, 245, 241)
    private val COLOR_MUTED = Color.rgb(156, 170, 165)
    private val COLOR_GREEN = Color.rgb(80, 216, 172)
    private val COLOR_LIME = Color.rgb(215, 255, 63)
    private val COLOR_NOTICE = Color.rgb(16, 39, 31)
    private val COLOR_NOTICE_TEXT = Color.rgb(146, 225, 190)
    private val COLOR_WARNING = Color.rgb(43, 31, 18)
    private val COLOR_WARNING_TEXT = Color.rgb(244, 197, 121)
    private val COLOR_CONSOLE = Color.rgb(155, 228, 198)
  }
}
