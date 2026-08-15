# Üretim İmzalama Yapılandırması

`native-android/signing.properties` dosyası mevcut olduğunda release derlemesi, bu dosyada tanımlı özel keystore’u otomatik kullanır. Keystore dosyası ve parola dosyası `.gitignore` ile genel depodan hariç tutulur.

## Yerel Anahtar Düzeni

| Dosya | Amaç | Git durumu |
|---|---|---|
| `native-android/keystore/apk-cleaner-release.jks` | Özel üretim imzalama anahtarı | Hariç tutulur |
| `native-android/signing.properties` | Anahtar konumu, alias ve parolalar | Hariç tutulur |
| `native-android/signing.properties.example` | Gizli bilgi içermeyen örnek şablon | İzlenir |

## Derleme

```bash
cd native-android
./gradlew :app:assembleRelease
```

`signing.properties` mevcutsa `app-release.apk` özel üretim sertifikasıyla imzalanır. Bu dosya bulunmuyorsa kaynak projeyi derlenebilir tutmak için Android debug imzası kullanılır; dağıtımdan önce bunu üretim imzasıyla değiştirmek gerekir.

## Anahtar Koruması

Keystore ve `signing.properties` dosyasının yedeğini şifreli bir parola yöneticisi ya da erişimi sınırlı güvenli depoda tutun. Aynı uygulama kimliği için kaybedilen özel anahtar daha sonra güncelleme yayımlamayı engeller; bu nedenle anahtar dosyasını paylaşmayın veya genel depoya eklemeyin.
