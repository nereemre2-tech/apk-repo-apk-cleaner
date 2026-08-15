# APK Cleaner Manager

**APK Cleaner Manager**, Android üzerinde çalışan yerel APK/APKS/APKM/XAPK analiz ve dönüşüm aracıdır. Seçilen paket uygulamanın özel alanında incelenir; bilinen reklam ağı işaretleri DEX düzeyinde taranabilir, uygun manifest girdileri düzenlenebilir, split paketler tek APK’da birleştirilebilir ve çıktı yeniden imzalanabilir.

> Bu proje yalnızca sahibi olduğunuz veya değiştirme/test yapmaya açıkça yetkili olduğunuz paketler için kullanılmalıdır. Lisans denetimlerini, ücretli özellikleri veya satın alımları atlamak amacıyla kullanılmamalıdır.

## İndirme, Kurulum ve Tarayıcı Önizlemesi

| Kaynak | Bağlantı |
|---|---|
| Güncel APK sürümü | [GitHub Releases](https://github.com/nereemre2-tech/apk-repo-apk-cleaner/releases/latest) |
| Doğrudan APK indirme | [ARM64 APK](https://github.com/nereemre2-tech/apk-repo-apk-cleaner/releases/latest/download/APK-Cleaner-Manager-v1.6.1-arm64-release.apk) |
| Android kurulum rehberi | [INSTALL.md](INSTALL.md) |
| Üretim imzalama yapılandırması | [PRODUCTION_SIGNING.md](PRODUCTION_SIGNING.md) |
| Etkileşimli tarayıcı önizlemesi | [docs/index.html](docs/index.html) |

Sürüm **1.6.1**, Android 8.0 (API 26) ve yeni ARM64-v8a cihazları hedefler. Kurulum, yeniden imzalama sınırları ve yetkili kullanım açıklamaları için [kurulum rehberini](INSTALL.md) izleyin.

## 1.6.1 Düzeltmesi

Android uygulamasının işlem ekranı artık gerçek paket adımlarından gelen **canlı ilerleme yüzdesini** ve son sekiz aşama olayını içeren **işlem günlüğünü** gösterir. Split birleştirme, paket tarama, her DEX yaması, manifest düzenlemesi, paketleme ve imzalama aşamalarının her biri ekrana ayrı durum satırı olarak yansır.

Araçlar ana kullanıcı arayüzünden ayrı `:apk_engine` Android sürecinde çalışır. Bir araç işlemi beklenmedik biçimde sonlanırsa ana uygulama süreci kapanmaz; işlem zaman aşımı ya da açıklayıcı hata sonucu kullanıcıya gösterilir.

İşlem ekranındaki **İşlemi iptal et** düğmesi, devam eden yerel araç sürecine iptal isteği gönderir. İptalden sonra geçici çalışma alanı temizlenir, orijinal paket korunur ve kullanıcı isterse işlem günlüğünü `TXT` dosyası olarak paylaşabilir. Başarılı işlemlerde de sonuç ekranındaki dışa aktarma eylemi aynı günlüğü Android paylaşım sayfasıyla aktarır.

Üst çubuktaki **Geçmiş** bölümü; son 25 başarılı, iptal edilen veya hata veren işlem için paket adını, profili, zaman bilgisini, özet günlüğü ve varsa APK çıktısı yolunu cihazda saklar. Her kayıt, günlüğün yeniden `TXT` olarak paylaşılmasına ve mevcut çıktının yeniden dışa aktarılmasına izin verir.

Uygulama arka plandayken işlem tamamlanır, iptal edilir veya hata oluşursa yerel cihaz bildirimi gönderilir. Android 13+ cihazlarda ilk açılışta bildirim izni istenir; bu izin verilmezse işlem geçmişi yine tutulur ancak sistem bildirimi gösterilmez.

Yeni **AdShield** seçeneği klasik reklam motorundan ayrı çalışır. Bir SDK ancak DEX içinde en az iki referansla birlikte manifest işareti veya iki ayrı DEX dosyasında görülürse yama kapsamına alınır. Mod, belirsiz SDK’ları, uygulama sınıflarını, asset ve yerel kütüphaneleri silmez; bu nedenle "tüm reklamları garantiyle kaldırma" iddiası yerine uygulamanın açılabilirliğini önceleyen doğrulanmış kapsam uygular.

AdShield işlemi artık başlamadan önce ayrı bir **etki özeti ve onay ekranı** gösterir. Bu ekranda değiştirilecek doğrulanmış SDK’lar, DEX referans sayıları, etkilenecek DEX dosyaları ve planlanan manifest düzenlemeleri görünür. Çifte kanıt eşiğini geçmeyen SDK’lar ayrıca korunacak olarak belirtilir; kullanıcı onay vermeden paket işleme başlamaz.

İşlenmiş APK’nın yeniden imzalanmasını engelleyen eksik çalışma zamanı imza varlığı düzeltildi. İmzalama aracı artık başarısız sonuç kodunu hata olarak iletir; manifest düzenlemesi doğrulama aşamasını geçemezse manifest güvenle geri alınır ve DEX değişiklikleri korunarak yeniden imzalama denenir. Hata oluşursa günlükte istisna zinciri gösterilir.

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
| `native-android/app/src/main/java/.../MainActivity.kt` | Koyu stüdyo arayüzü, paket seçimi, profil seçimi, AdShield etki özeti/onayı, canlı yüzde ve günlük görünümü |
| `native-android/app/src/main/java/.../CleanerEngine.kt` | Analiz, AdShield etki özeti verisi, split birleştirme, DEX/manifest düzenleme, paket yeniden yazma, doğrulanmış yerel çıktı imzalama ve geri alma akışı |
| `native-android/app/src/main/java/.../IsolatedToolService.kt` | Dış araçları `:apk_engine` sürecinde çalıştıran korumalı servis ve zaman aşımı katmanı |
| `native-android/app/src/main/java/.../ProcessingHistory.kt` | İşlem sonuçlarını cihazdaki `SharedPreferences` alanında saklayan geçmiş deposu |
| `native-android/app/src/main/java/.../ProcessingNotifier.kt` | Arka planda işlem sonucu için yerel Android bildirimleri |
| `native-android/app/src/main/java/.../AdShield.kt` | Çifte kanıt kuralıyla çalışan yeni korumalı reklam temizleme politikası |
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

Başarılı derlemede APK `native-android/app/build/outputs/apk/release/app-release.apk` konumunda oluşur. Dağıtım dosyası proje köküne `APK-Cleaner-Manager-v1.6.1-arm64-release.apk` adıyla kopyalanır ve GitHub Release varlığı olarak yayımlanır.

Uygulama içindeki çıktı APK’larını imzalamak için kullanılan gömülü JKS dosyası, yalnızca cihazdaki işlenmiş çıktının kurulabilir olmasını sağlayan yerel çıktı anahtarıdır. Uygulamanın kendisini yayımlamak için kullanılan üretim imzalama anahtarı ayrı tutulur ve depoya eklenmez.

## Bilinen Sınırlar

Otomatik tespit, gizlenmiş veya uygulamaya özgü işaretçileri her zaman algılamayabilir. İşlenmiş APK yerel sertifikayla yeniden imzalandığından, mağazadan yüklenmiş bir uygulamanın mevcut sürümü üzerine doğrudan kurulamayabilir. XML reklam görünümü temizleme ve ZIP hizalama özelliği ilk bağımsız sürümün kapsamı dışındadır.
