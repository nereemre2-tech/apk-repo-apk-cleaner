package com.app.apkcleanermanager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class ProcessingNotifier(private val context: Context) {
  fun createChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(CHANNEL_ID, "APK Cleaner işlemleri", NotificationManager.IMPORTANCE_DEFAULT).apply {
        description = "APK işleme tamamlandığında, iptal edildiğinde veya hata oluştuğunda bildirim gösterir."
      }
      context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
  }

  fun completed(sourceName: String, outputName: String) = show(
    "APK işlemi tamamlandı",
    "$sourceName işlendi. Çıktı: $outputName",
    android.R.drawable.stat_sys_download_done,
  )

  fun cancelled(sourceName: String) = show(
    "APK işlemi iptal edildi",
    "$sourceName için geçici dosyalar temizlendi; kaynak paket korunuyor.",
    android.R.drawable.ic_menu_close_clear_cancel,
  )

  fun failed(sourceName: String) = show(
    "APK işlemi tamamlanamadı",
    "$sourceName için hata kaydı uygulama içinde saklandı.",
    android.R.drawable.stat_notify_error,
  )

  private fun show(title: String, detail: String, icon: Int) {
    createChannel()
    if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
    val intent = Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP }
    val pending = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(icon)
      .setContentTitle(title)
      .setContentText(detail)
      .setStyle(NotificationCompat.BigTextStyle().bigText(detail))
      .setContentIntent(pending)
      .setAutoCancel(true)
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .build()
    NotificationManagerCompat.from(context).notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
  }

  companion object { private const val CHANNEL_ID = "apk_cleaner_processing" }
}
