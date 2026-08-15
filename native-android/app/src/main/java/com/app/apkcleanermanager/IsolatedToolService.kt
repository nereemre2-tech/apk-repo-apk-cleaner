package com.app.apkcleanermanager

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Process
import android.os.ResultReceiver
import com.reandroid.apkeditor.Main
import local.apkcleaner.dex.DirectDexPatcher
import local.apkcleaner.xml.BinaryManifestPatcher
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * CLI kökenli araçlar, ana kullanıcı arayüzü sürecinden ayrı bir Android sürecinde çalışır.
 * Böylece üçüncü taraf bir araç ağır hata verirse uygulama ekranı kapanmaz; ana süreç zaman
 * aşımı ya da hata sonucunu kullanıcıya gösterir.
 */
class IsolatedToolService : Service() {
  private val executor = Executors.newSingleThreadExecutor()
  @Volatile private var activeReceiver: ResultReceiver? = null

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val command = intent?.action
    val arguments = intent?.getStringArrayListExtra(EXTRA_ARGUMENTS)?.toTypedArray() ?: emptyArray()
    val receiver = intent?.getParcelableExtra<ResultReceiver>(EXTRA_RECEIVER)
    if (command == COMMAND_CANCEL) {
      activeReceiver?.send(RESULT_CANCELLED, Bundle().apply { putString("message", "İşlem kullanıcı tarafından iptal edildi.") })
      executor.shutdownNow()
      stopSelf()
      Handler(Looper.getMainLooper()).postDelayed({ Process.killProcess(Process.myPid()) }, 150)
      return START_NOT_STICKY
    }
    activeReceiver = receiver
    executor.execute {
      try {
        when (command) {
          COMMAND_DEX -> DirectDexPatcher.main(arguments)
          COMMAND_MANIFEST -> BinaryManifestPatcher.main(arguments)
          COMMAND_SPLIT -> {
            val code = Main.execute(arguments)
            require(code == 0) { "Split birleştirici $code durum koduyla tamamlandı." }
          }
          COMMAND_SIGN -> {
            val tool = Class.forName("at.favre.tools.apksigner.SignTool")
            val execute = tool.getDeclaredMethod("mainExecute", Array<String>::class.java).apply { isAccessible = true }
            val result = execute.invoke(null, arguments as Any)
            if (result != null) {
              val resultClass = result.javaClass
              val error = resultClass.getDeclaredField("error").apply { isAccessible = true }.getBoolean(result)
              val unsuccessful = resultClass.getDeclaredField("unsuccessful").apply { isAccessible = true }.getInt(result)
              require(!error && unsuccessful == 0) {
                "APK imzalama aracı imza veya doğrulama hatası bildirdi ($unsuccessful başarısız paket)."
              }
            }
          }
          else -> error("Bilinmeyen yerel motor görevi.")
        }
        receiver?.send(RESULT_OK, Bundle())
      } catch (error: Throwable) {
        receiver?.send(RESULT_ERROR, Bundle().apply {
          putString("message", errorDetail(error))
        })
      } finally {
        activeReceiver = null
        stopSelf(startId)
      }
    }
    return START_NOT_STICKY
  }

  override fun onDestroy() {
    executor.shutdownNow()
    super.onDestroy()
  }

  private fun errorDetail(error: Throwable): String = generateSequence(error) { it.cause }
    .take(4)
    .joinToString(" → ") { item -> item.message?.takeIf { it.isNotBlank() } ?: item.javaClass.simpleName }

  companion object {
    const val COMMAND_DEX = "com.app.apkcleanermanager.tool.DEX"
    const val COMMAND_MANIFEST = "com.app.apkcleanermanager.tool.MANIFEST"
    const val COMMAND_SPLIT = "com.app.apkcleanermanager.tool.SPLIT"
    const val COMMAND_SIGN = "com.app.apkcleanermanager.tool.SIGN"
    const val COMMAND_CANCEL = "com.app.apkcleanermanager.tool.CANCEL"
    const val EXTRA_ARGUMENTS = "arguments"
    const val EXTRA_RECEIVER = "receiver"
    const val RESULT_OK = 1
    const val RESULT_ERROR = 2
    const val RESULT_CANCELLED = 3
  }
}

class IsolatedToolRunner(private val context: Context) {
  fun cancelActive() {
    context.startService(Intent(context, IsolatedToolService::class.java).apply { action = IsolatedToolService.COMMAND_CANCEL })
  }

  fun run(command: String, arguments: List<String>, timeoutSeconds: Long = 90) {
    val latch = CountDownLatch(1)
    var resultCode = IsolatedToolService.RESULT_ERROR
    var failure: String? = null
    val receiver = object : ResultReceiver(null) {
      override fun onReceiveResult(code: Int, data: Bundle?) {
        resultCode = code
        failure = data?.getString("message")
        latch.countDown()
      }
    }
    val started = context.startService(Intent(context, IsolatedToolService::class.java).apply {
      action = command
      putStringArrayListExtra(IsolatedToolService.EXTRA_ARGUMENTS, ArrayList(arguments))
      putExtra(IsolatedToolService.EXTRA_RECEIVER, receiver)
    })
    require(started != null) { "Yerel işlem motoru başlatılamadı." }
    if (!latch.await(timeoutSeconds, TimeUnit.SECONDS)) {
      cancelActive()
      error("Yerel motor ${timeoutSeconds / 60} dakika içinde yanıt vermedi; güvenli biçimde durduruldu. Daha küçük veya daha güvenli profil ile tekrar deneyin.")
    }
    require(resultCode == IsolatedToolService.RESULT_OK) { failure ?: "Yerel motor görevi tamamlanamadı." }
  }
}
