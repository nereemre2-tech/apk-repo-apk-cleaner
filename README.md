# APK Cleaner Manager

Android üzerinde çalışan, yerel APK/APKS/APKM/XAPK analiz ve dönüştürme aracı. Uygulama, paketi cihazın uygulamaya özel alanında analiz eder; bilinen reklam ağı işaretlerini DEX düzeyinde inceleyebilir, uygun manifest girdilerini düzenleyebilir, split paketleri tek APK çıktısında birleştirebilir ve sonucu yeniden imzalayabilir.

> Bu proje yalnızca sahibi olduğunuz veya değişiklik/test yapmaya açıkça yetkili olduğunuz Android paketleri için kullanılmalıdır. Lisans denetimlerini, ücretli özellikleri veya satın alımları atlamak amacıyla kullanılmamalıdır.

## Sürüm 1.1

Sürüm 1.1, koyu **APK Cleaner Studio** arayüzünü, AC ve APK Repo görsel varlıklarını, dört adımlı işlem akışını ve APK araçları için izole Android servis sürecini içerir. DEX, manifest, split birleştirme ve imzalama araçları `:apk_engine` sürecinde çalışır; bu sayede araç kaynaklı bir süreç hatası ana kullanıcı arayüzünü kapatmaz ve uygulama kontrollü hata mesajı gösterebilir.

| Özellik | Açıklama |
|---|---|
| Paket türleri | APK, APKS, APKM ve XAPK |
| İşlem profilleri | Güvenli, Dengeli ve Kapsamlı |
| Yerel çalışma | Paketler uzak sunucuya gönderilmeden uygulama alanında işlenir |
| Split dönüşümü | Uygun paketlerde tek APK çıktısı |
| Çıktı | Android paylaşım sayfasıyla kaydetme veya paylaşma |

## Hazır APK

ARM64 hedefli derleme dosyası proje kökünde `APK-Cleaner-Manager-v1.1.0-arm64-release.apk` adıyla bulunur. APK v2 imza şemasıyla doğrulanmıştır.

## Kaynaktan Derleme

Bağımsız Android projesi `native-android/` dizinindedir. Android SDK yolunuzu ayarlamak için `native-android/local.properties.example` dosyasını `native-android/local.properties` adıyla kopyalayın ve `sdk.dir` değerini değiştirin. Ardından aşağıdaki komutu çalıştırın:

```bash
cd native-android
./gradlew :app:assembleRelease
```

Derleme çıktısı `native-android/app/build/outputs/apk/release/app-release.apk` konumunda oluşturulur. Uygulama içindeki çıktı paketlerini imzalamak için kullanılan JKS dosyası yalnızca geliştirme amaçlıdır; üretim dağıtımında kendi imzalama anahtarınızı kullanın.

## Proje Düzeni

| Dizin veya dosya | Amaç |
|---|---|
| `native-android/` | Bağımsız, düşük bağımlılıklı Android uygulaması |
| `native-android/app/src/main/java/` | Yerel arayüz, işlem motoru ve izole araç servisi |
| `native-android/app/libs/` | Kaynak projeden taşınan DEX, manifest, split ve imzalama araçları |
| `APK-Cleaner-Manager-v1.1.0-arm64-release.apk` | Hazır ARM64 APK çıktısı |
| `DELIVERY.md` | Derleme ve kullanım notları |

## Sınırlar

Otomatik tespit, gizlenmiş veya uygulamaya özel işaretçileri her zaman algılayamaz. İşlenmiş APK’lar yerel sertifikayla yeniden imzalandığından, mağazadan yüklenmiş bir uygulamanın mevcut sürümü üzerine doğrudan kurulamayabilir. İlk bağımsız sürüm, XML reklam görünümü temizleme ve ZIP hizalama özelliğini kapsamaz.
