# İmzalama Sonrası Hata İnceleme Notu

Kullanıcının sağladığı işlem günlüğünde DEX ve manifest düzenlemesinin tamamlandığı, hatanın `Çıktı APK’sı yerel sertifikayla imzalanıyor` aşamasından sonra oluştuğu görülmektedir. Uygulama, gömülü Uber APK Signer aracının `mainExecute` metodunu yansıma ile çağırmaktadır.

Bu aracın kaynak kodu, imza ya da doğrulama sırasında oluşan bazı hataları istisna fırlatmak yerine sonuç nesnesindeki hata/başarısızlık alanlarıyla bildirir. Mevcut çağrı dönüş değerini denetlemediği için araç başarısız olduğunda açıklayıcı hata nedenini kaybedebilir. Düzeltme; dönüş sonucunu incelemeli, araç çıktısını ve imzalı APK varlığını açıkça doğrulamalı, geçici dosyayı yalnızca doğrulama tamamlandığında dışa aktarmalıdır.

Kaynaklar: Uber APK Signer `SignTool` kaynak kodu ve proje hata kaydı #35.
