# APK Cleaner Manager

**APK Cleaner Manager**, Android üzerinde çalışan yerel APK/APKS/APKM/XAPK analiz ve dönüşüm aracıdır. Seçilen paket uygulamanın özel alanında incelenir; bilinen reklam ağı işaretleri DEX düzeyinde taranabilir, uygun manifest girdileri düzenlenebilir, split paketler tek APK’da birleştirilebilir ve çıktı yeniden imzalanabilir.

> Bu proje yalnızca sahibi olduğunuz veya değiştirme/test yapmaya açıkça yetkili olduğunuz paketler için kullanılmalıdır. Lisans denetimlerini, ücretli özellikleri veya satın alımları atlamak amacıyla kullanılmamalıdır.

## İndirme, Kurulum ve Tarayıcı Önizlemesi

| Kaynak | Bağlantı |
|---|---|
| Güncel APK sürümü | [GitHub Releases](https://github.com/nereemre2-tech/apk-repo-apk-cleaner/releases/latest) |
| Doğrudan APK indirme | [ARM64 APK](https://github.com/nereemre2-tech/apk-repo-apk-cleaner/releases/latest/download/APK-Cleaner-Manager-v1.4.0-arm64-release.apk) |
| Android kurulum rehberi | [INSTALL.md](INSTALL.md) |
| Üretim imzalama yapılandırması | [PRODUCTION_SIGNING.md](PRODUCTION_SIGNING.md) |
| Etkileşimli tarayıcı önizlemesi | [docs/index.html](docs/index.html) |

Sürüm **1.4.0**, Android 8.0 (API 26) ve yeni ARM64-v8a cihazları hedefler. Kurulum, yeniden imzalama sınırları ve yetkili kullanım açıklamaları için [kurulum rehberini](INSTALL.md) izleyin.

## 1.4.0 Yenilikleri

Android uygulamasının işlem ekranı artık gerçek paket adımlarından gelen **canlı ilerleme yüzdesini** ve son sekiz aşama olayını içeren **işlem günlüğünü** gösterir. Split birleştirme, paket tarama, her DEX yaması, manifest düzenlemesi, paketleme ve imzalama aşamalarının her biri ekrana ayrı durum satırı olarak yansır.

Araçlar ana kullanıcı arayüzünden ayrı `:apk_engine` Android sürecinde çalışır. Bir araç işlemi beklenmedik biçimde sonlanırsa ana uygulama süreci kapanmaz; işlem zaman aşımı ya da açıklayıcı hata sonucu kullanıcıya gösterilir.

İşlem ekranındaki **İşlemi iptal et** düğmesi, devam eden yerel araç sürecine iptal isteği gönderir. İptalden sonra geçici çalışma alanı temizlenir, orijinal paket korunur ve kullanıcı isterse işlem günlüğünü `TXT` dosyası olarak paylaşabilir. Başarılı işlemlerde de sonuç ekranındaki dışa aktarma eylemi aynı günlüğü Android paylaşım sayfasıyla aktarır.

Üst çubuktaki **Geçmiş** bölümü; son 25 başarılı, iptal edilen veya hata veren işlem için paket adını, profili, zaman bilgisini, özet günlüğü ve varsa APK çıktısı yolunu cihazda saklar. Her kayıt, günlüğün yeniden `TXT` olarak paylaşılmasına ve mevcut çıktının yeniden dışa aktarılmasına izin verir.

## Özellikler

| Özellik | Açıklama |
|---|---|
| Paket türleri | APK, APKS, APKM ve XAPK |
| İşlem profilleri | Güvenli, Dengeli ve Kapsamlı |
| Yerel çalışma | Paketler uzak sunucuya gönderilmeden uygulama alanında işlenir |
| Split dönüşümü | Uygun paketlerde tek APK çıktısı |
| Görünür ilerleme | Canlı yüzde, aşama mesajı ve işlem günlüğü |
| Çıktı | Android paylaşım sayfası ile kaydetme veya paylaşma |

## Kod Yapısı

| Konum | Sorumluluk |
|---|---|
| `native-android/` | Bağımsız ve düşük bağımlılıklı Android Gradle projesi |
| `native-android/app/src/main/java/.../MainActivity.kt` | Koyu stüdyo arayüzü, paket seçimi, profil seçimi, canlı yüzde ve günlük görünümü |
| `native-android/app/src/main/java/.../CleanerEngine.kt` | Analiz, split birleştirme, DEX/manifest düzenleme, paket yeniden yazma ve çıktı akışı |
| `native-android/app/src/main/java/.../IsolatedToolService.kt` | Dış araçları `:apk_engine` sürecinde çalıştıran korumalı servis ve zaman aşımı katmanı |
| `native-android/app/src/main/java/.../ProcessingHistory.kt` | İşlem sonuçlarını cihazdaki `SharedPreferences` alanında saklayan geçmiş deposu |
| `native-android/app/libs/` | DEX, manifest, split ve imzalama araçları |
| `native-android/app/src/main/assets/profiles.json` | Reklam ağı tespit profilleri |
| `docs/` | Tarayıcıda test edilebilen etkileşimli işlem akışı önizlemesi |
| `INSTALL.md` | İndirme ve Android kurulum kılavuzu |
| `PRODUCTION_SIGNING.md` | Özel keystore, gizli yapılandırma ve üretim derlemesi yönergeleri |

## Derleme Süreci

Önce Android SDK yolunuzu ayarlamak için `native-android/local.properties.example` dosyasını `native-android/local.properties` adıyla kopyalayın ve `sdk.dir` değerini güncelleyin. Ardından JDK 17 ve Android SDK ortam değişkenleriyle aşağıdaki komutu çalıştırın:

```bash
cd native-android
./gradlew :app:assembleRelease
```

Başarılı derlemede APK `native-android/app/build/outputs/apk/release/app-release.apk` konumunda oluşur. Dağıtım dosyası proje köküne `APK-Cleaner-Manager-v1.4.0-arm64-release.apk` adıyla kopyalanır ve GitHub Release varlığı olarak yayımlanır.

Uygulama içindeki çıktı APK’larını imzalamak için kullanılan JKS dosyası **geliştirme amaçlıdır**. Üretim dağıtımında kendi imzalama anahtarınızı kullanın.

## Bilinen Sınırlar

Otomatik tespit, gizlenmiş veya uygulamaya özgü işaretçileri her zaman algılamayabilir. İşlenmiş APK yerel sertifikayla yeniden imzalandığından, mağazadan yüklenmiş bir uygulamanın mevcut sürümü üzerine doğrudan kurulamayabilir. XML reklam görünümü temizleme ve ZIP hizalama özelliği ilk bağımsız sürümün kapsamı dışındadır.
