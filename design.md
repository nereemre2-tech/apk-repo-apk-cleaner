# APK Cleaner Manager — Mobil Arayüz Tasarımı

## Tasarım İlkeleri

Uygulama, tek elle kullanılabilen **9:16 dikey Android ekranı** için tasarlanacaktır. Kaynak Termux arayüzündeki çok adımlı iş akışı korunacak, ancak yerel dosya seçme ve işlem durumu Android’in yerel etkileşim kalıplarına göre düzenlenecektir. Birincil işlem, kullanıcının sahip olduğu veya değiştirmeye yetkili olduğu APK/APKS/APKM/XAPK paketini cihazdan seçmesi, analiz etmesi ve izin verilen yerel dönüştürme işlemini başlatmasıdır.

## Ekran Listesi

| Ekran | Ana içerik | İşlev |
|---|---|---|
| Ana ekran | Dosya seçici kartı, desteklenen uzantılar, güvenlik notu, son işlemler | Paket seçme ve önceki işleme erişme |
| Analiz ekranı | Paket adı, boyut, DEX sayısı, algılanan reklam ağları, split bileşenleri | Analiz özetini gösterme ve işlem seçeneklerini belirleme |
| İşlem seçenekleri | Güvenli/Dengeli/Kapsamlı profil, reklam yaması, split seçimi, hata ayıklama temizliği ve ZIP hizalama anahtarları | Dönüştürme ya da temizleme işlem parametrelerini yapılandırma |
| İşlem ilerleme ekranı | Aşamalı ilerleme çubuğu, işlem günlüğü, iptal denetimi | Uzun süren yerel işlemde durum geri bildirimi ve güvenli iptal |
| Sonuç ekranı | Çıktı durumu, değişiklik sayıları, kaydet/paylaş eylemleri, rapor özeti | Oluşan APK’yı dışarı aktarma veya paylaşma |
| Geçmiş ekranı | Yerel işlem kayıtları, durum, rapor/çıktı erişimi, silme | Cihazdaki önceki işleri yeniden görüntüleme ve yönetme |
| Ayarlar ve güvenlik | Yerel depolama kullanımı, çıktı klasörü, araç durumu, yasal kullanım bildirimi | Yerel çalışma ilkelerini açıklama ve uygulama durumunu gösterme |

## Temel Kullanıcı Akışları

Kullanıcı ana ekranda **Paket seç** eylemine dokunur, Android dosya seçicisinden desteklenen bir paketi seçer ve uygulama bunu özel uygulama depolamasına alır. Ardından analiz ekranı DEX ve paket bileşenleri özetini gösterir. Kullanıcı profil ve isteğe bağlı işlemleri seçtikten sonra **İşlemi başlat** düğmesine dokunur. İlerleme ekranı işlemin aşamasını gösterir; tamamlandığında sonuç ekranından çıktı APK’sını kaydedebilir veya Android paylaşım sayfasına gönderebilir.

Split paketlerde kullanıcı, analiz ekranındaki bileşen seçiminden bir işlemci mimarisi ve uygun dil/donanım seçeneklerini belirler. Uygulama bu tercihi işlem yapılandırmasına ekler; ardından sonuç ekranı tek APK çıktısını sunar. İşlem tamamlanamadığında, aynı ekran hata bağlamını ve güvenli yeniden deneme seçeneğini gösterir.

## Renkler ve Görsel Dil

Uygulamanın marka rengi koyu lacivert zemin üzerinde **elektrik turkuazı** vurgudur. Açık temada arka plan `#F6F8FB`, yüzeyler `#FFFFFF`, ana metin `#14213D`, ikincil metin `#5D6B82`, vurgu `#007E8A`, olumlu durum `#16794A`, uyarı `#A85E00` ve hata `#B3261E` kullanılacaktır. Koyu temada arka plan `#0B1220`, yüzey `#141E2D`, ana metin `#E8EEF8`, sınır `#2D3B50` ve turkuaz vurgu `#38C7D5` olacaktır.

İkonografi; dosya paketi, kalkan/doğrulama, katmanlı split paket, zaman çizelgesi ve paylaşma eylemlerinden oluşacaktır. Ana eylem düğmeleri ekran altına yakın, en az 48 dp yüksekliğinde ve erişilebilir metin karşıtlığıyla yerleştirilecektir. Kritik işlemlerden önce uygulama, kullanım yetkisi ve yeniden imzalanmış APK sınırını açık bir onay metniyle hatırlatacaktır.

