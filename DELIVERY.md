# APK Cleaner Manager — Teslim Notları

## Çıktı

Dağıtıma hazır Android paketi `native-android/app/build/outputs/apk/release/app-release.apk` konumunda üretilmiştir. Paket adı **APK Cleaner Manager**, uygulama kimliği `com.app.apkcleanermanager`, sürümü **1.0.0** ve en düşük Android sürümü **Android 8.0 (API 26)** olarak yapılandırılmıştır.

APK, Android’in v2 imza şemasıyla imzalanmış ve paket bütünlüğü derleme sonrasında doğrulanmıştır. Uygulama ARM64 cihazlar için derlenmiştir.

## Uygulama Akışı

Uygulama, Android belge seçicisinden **APK, APKS, APKM veya XAPK** paketi alır. Seçilen paket uygulamanın özel alanına kopyalanır, DEX dosyaları ve tanımlı reklam ağı işaretçileri yerel olarak analiz edilir. Split paketler, kaynakta bulunan APKEditor aracılığıyla tek APK hedefi için birleştirilir.

Kullanıcı güvenli, dengeli veya kapsamlı profillerden birini seçebilir. Reklam yaması seçeneği bilinen ağ işaretlerini taşıyan DEX dosyalarına uygulanır. DEX hata ayıklama temizliği isteğe bağlıdır. Dengeli ve kapsamlı profillerde uygun manifest işaretleri de yama kapsamına alınır. Kapsamlı profil yalnızca doğrulanmış asset ve kütüphane kalıntılarını kaldırmayı dener.

Sonuç APK’sı uygulamaya özel Belgeler dizinine yazılır ve Android paylaşım sayfasıyla kaydedilebilir ya da paylaşılabilir. Oluşturulan her çıktı yeniden imzalanır; bu nedenle mağazadan yüklenmiş uygulamanın mevcut sürümünün üzerine doğrudan kurulamayabilir.

## Kurulum

1. `app-release.apk` dosyasını Android cihaza aktarın.
2. Dosya yöneticisinden dosyaya dokunun ve, Android sorarsa, bu kaynak için uygulama kurma iznini verin.
3. Uygulamayı açın, **paket seç** düğmesiyle desteklenen dosyayı belirleyin, analiz sonucu geldikten sonra işlem profilini seçin ve işlemi başlatın.

> Yalnızca sahibi olduğunuz veya değiştirme/test etme yetkiniz bulunan paketleri işleyin. Bu araç, lisans kontrollerini, satın alımları veya ücretli özellikleri atlamak için kullanılmamalıdır.

## Bilinen Sınırlar

İlk bağımsız Android sürümü, XML tabanlı reklam görünümü gizleme ve ZIP hizalama işlemlerini içermez. Otomatik analiz, özel veya gizlenmiş işaretçileri algılamayabilir; uygulamaya özgü test yapılması gerekir. DEX yama motoru, riskli nesne döndüren çağrıları değiştirmemek üzere kaynak güvenlik kurallarıyla çalışır.

## Kaynak Yapısı

Kullanıcı arayüzü ve Android yerel motoru, düşük bağımlılıklı `native-android/` projesinde bulunur. Bu tercih, Termux kaynak projesindeki Java araçlarının doğrudan Android APK’sına gömülmesini ve React Native’in yüksek bellekli yerel derleme zincirine bağlı kalınmamasını sağlar.
