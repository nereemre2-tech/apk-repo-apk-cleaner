# APK Cleaner Manager Kurulum Rehberi

## APK’yı İndirme

Güncel ARM64 sürümünü [GitHub sürümler sayfasından](https://github.com/nereemre2-tech/apk-repo-apk-cleaner/releases/latest) indirebilirsiniz. Doğrudan dosya bağlantısı, sürüm yayınlandıktan sonra aşağıdaki biçimde çalışır:

`https://github.com/nereemre2-tech/apk-repo-apk-cleaner/releases/latest/download/APK-Cleaner-Manager-v1.6.1-arm64-release.apk`

Bu APK, **ARM64-v8a** mimarisine sahip Android cihazlar içindir. Android 8.0 (API 26) ve daha yeni sürümler gerekir.

## Kurulum Adımları

1. Telefonunuzda indirme bağlantısını açın ve APK dosyasını indirin.
2. İndirme tamamlandığında dosyaya dokunun. Android, tarayıcınız veya dosya yöneticiniz için uygulama yükleme izni isterse **Bu kaynaktan izin ver** seçeneğini etkinleştirin.
3. **Yükle** düğmesine dokunun. Kurulum tamamlandığında uygulamayı açın.
4. Uygulamada **Dosya seç** eylemiyle APK, APKS, APKM veya XAPK paketinizi seçin. Analiz tamamlandığında işlem türünü ve profilini belirleyin. AdShield seçildiğinde, işlem başlamadan önce doğrulanmış SDK’ları, etkilenecek DEX kayıtlarını ve manifest planını inceleyip onaylayın.
5. İşlem ekranında canlı yüzdeyi ve aşama günlüğünü izleyin. Gerekirse **İşlemi iptal et** düğmesiyle aktif yerel araç sürecini durdurun. Tamamlandığında oluşan APK’yı sistem paylaşım ekranından kaydedin veya paylaşın; işlem veya iptal günlüğünü TXT dosyası olarak dışa aktarabilirsiniz.
6. Üst çubuktaki **Geçmiş** düğmesiyle son 25 tamamlanan, iptal edilen veya hata veren işi; özetini, günlüğünü ve mevcutsa APK çıktısını yeniden paylaşabilirsiniz.
7. İşlem başladığında uygulamayı arka plana alabilirsiniz. Android 13 ve üzeri sürümlerde bildirim iznini verdiyseniz işlem tamamlandığında, iptal edildiğinde veya hata ile bittiğinde cihazınıza yerel bildirim gönderilir.

> Paket çıktıları yerel sertifikayla yeniden imzalanır. Bu nedenle mağazadan yüklenen uygulamanın mevcut sürümünün üzerine doğrudan kurulum mümkün olmayabilir; önce eski sürümü kaldırmanız gerekebilir.

> **v1.6.1 yükseltme notu:** Önceki v1.6.0 uygulama paketiyle farklı bir uygulama imza sertifikası kullanıldığından Android, güncellemeyi "paket çakışıyor" uyarısıyla engelleyebilir. Bu durumda eski APK Cleaner Manager uygulamasını kaldırıp v1.6.1’i yeniden kurun. Uygulamayı kaldırmadan önce ihtiyaç duyduğunuz işlem günlüklerini ve APK çıktılarını paylaşın; kaldırma, uygulamanın cihaz içi geçmişini siler.

## Güvenlik ve Yetki

Yalnızca sahibi olduğunuz veya açıkça değiştirme/test etme yetkiniz bulunan paketlerde kullanın. Uygulama lisans kontrollerini, satın alımları ya da ücretli özellikleri atlamak için tasarlanmamıştır.
