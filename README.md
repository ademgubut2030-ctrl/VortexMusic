# Vortex Music

Telefonundaki müziği çalan, ücretsiz ve açık kaynaklı bir Android müzik çalar.
Hesap yok, reklam yok, takip yok, internet izni bile yok.

> **Durum: beta (1.0.0-beta.1).** Arayüz şimdilik sadece Türkçe. İngilizce çeviri planlanıyor.

## Neden Vortex

- **Gizlilik için tasarlandı.** Her şey telefonunda kalır. Uygulama İNTERNET iznini hiç istemez.
- **Müzik arşivine önem verenler için.** Şarkılarını istediğin gibi sırala, grupla, düzenle. Düzen sadece uygulamanın içinde durur, dosyalarına dokunulmaz.
- **Şekillendirebildiğin ses.** Bir eğrinin üzerindeki noktaları sürükleyerek ayarlanan ekolayzır, kulaklığın bağlanınca kendiliğinden devreye giren profiller ve daha fazlası.

## Özellikler

**Kütüphane**
- Şarkılar, sanatçılar, albümler, klasörler, beğenilenler ve akıllı listeler (en çok dinlenen, son eklenen, hiç dinlenmeyen, unutulanlar)
- Her albüme kendi kapak resmini ekleme
- Bir sanatçının tüm şarkılarını tek albümde toplama ya da dosyalardaki albüm adlarını kullanma
- 14 sıralama ölçütü, liste ve ızgara görünümü, "sadece başlıklar" modu ve ↑ ↓ ile elle sıralama (her liste için ayrı, sadece uygulamada saklanır)
- Yinelenen dosya bulucu ve güvenli silme (Android 11 ve üstü)

**Çalma**
- Arka planda çalma ve bildirim kontrolleri (bildirime dokununca uygulama açılır)
- Kuyruk ekranı: sırayı değiştirme, çıkarma, "sıradaki olarak çal", "kuyruğa ekle"
- Karıştır, döngü, hız ayarı (0.75x - 1.5x) ve isteğe bağlı perde hızla değişen "plak" modu
- Yumuşak başlangıç ve son kuyruğunu ve konumunu hatırlama
- Tam ekran oynatıcı, çalan şarkının kapak rengini kullanır

**Ses**
- Sürüklenebilir eğri grafikli ekolayzır, hazır ayarlar, bas güçlendirici, ses yükseltme ve sanal surround
- Ayarlarını profil olarak kaydet, dosya olarak dışa ve içe aktar
- Bluetooth ya da kablolu kulaklık bağlanınca profilin otomatik devreye girmesi

**Arama**
- Büyük/küçük harf ve Türkçe karakterleri yok sayan arama ("sezen" yazınca "Şezen" bulunur)
- Şarkı, sanatçı ya da albüme göre arama, sıralama ve son aramalar

**Dinleme istatistikleri**
- Toplam süre, günlük eğri, sanatçı payları, günün saatleri, en çok dinlenen şarkılar (sadece telefonunda saklanır)

**Şarkı sözleri**
- `.lrc` ya da `.txt` dosyası içe aktarma veya yapıştırma. `.lrc` sözleri şarkıyla birlikte satır satır parlar, bir satıra dokununca şarkı oraya sarar

**Görünüm**
- Sistem, açık ya da koyu mod
- Renk şemaları (Vortex, Gece Yarısı / İlk Kar ve birkaç tane daha), Android 12 ve üstünde dinamik şema, isteğe bağlı bulanık renk ve neon çizgileri

**Yedekleme**
- Beğeniler, klasörler, sözler, kapaklar, sıralamalar ve ayarların yedeklenmesi ve geri yüklenmesi (aynı telefonda geri yüklemek için tasarlandı)

## Kurulum

1. En son APK'yı [Releases](https://github.com/<kullanici-adin>/<depo-adi>/releases) sayfasından indir.
2. Android sorarsa tarayıcından ya da dosya yöneticinden kuruluma izin ver.
3. Vortex'i aç ve müzik iznini ver.

**Gereksinimler:** Android 8.0 (API 26) ve üstü. Yinelenen dosya silme Android 11+, dinamik renk Android 12+ ister.

**Güncelleme:** Tüm sürümler aynı anahtarla imzalanır, yani yeni APK eskisinin üstüne kurulur ve verilerin kalır. Otomatik bildirim almak için bu depoyu [Obtainium](https://github.com/ImranR98/Obtainium)'a ekle: Obtainium'u aç, "Uygulama ekle"yi seç, depo adresini yapıştır, Vortex Releases sayfasından izlenir.

## Gizlilik

Vortex hiçbir veri toplamaz, göndermez ya da paylaşmaz. İnternet erişimi, analiz aracı ve reklamı yoktur.

İstediği izinler ve nedenleri:

| İzin | Neden |
|---|---|
| Müzik ve ses (`READ_MEDIA_AUDIO`, Android 12 ve altında `READ_EXTERNAL_STORAGE`) | Şarkılarını listelemek ve çalmak için |
| Bildirimler | Çalma kontrollerini göstermek için |
| Ön plan hizmeti (medya oynatma), uyanık tutma | Ekran kapalıyken çalmaya devam etmek için |
| Bluetooth bağlantısı | Kulaklığının adını okuyup kayıtlı ses profilini otomatik uygulamak için |

Yedek dosyaları yalnızca sen istediğinde oluşturulur ve nereye kaydedileceğini sen seçersin.

## Kaynaktan derleme

Vortex tamamen telefonda, [CodeAssist](https://github.com/tyron12233/CodeAssist) IDE'si ile yazıldı.

- Dil: Kotlin, klasik Android Views (Compose yok)
- Oynatıcı: AndroidX Media3 (ExoPlayer ve MediaSession)
- Paket: `io.github.vortexmusic.player`, minSdk 26, targetSdk 36

Proje klasörünü CodeAssist'e içe aktarabilirsin. CodeAssist projeyi Gradle derlemesi olarak da dışa aktarabiliyor. Bu yol bu projeyle henüz denenmedi.

**İmza hakkında:** Resmî sürümler, bu depoda bulunmayan geliştiricinin özel anahtarıyla imzalanır. Kendin derlersen kendi anahtarınla imzala. Farklı anahtarla imzalanmış bir APK, resmî sürümün üstüne kurulamaz, tersi de geçerli.

## Bilinen sorunlar

- Arayüz şimdilik sadece Türkçe.
- Ekolayzır Android'in hazır efektlerini kullanır. Bas, ses yükseltme ve surround gücü telefon markasına ve kulaklığa göre değişir, bant sayısı (genelde 5) ve frekanslar telefonun verdiği gibidir.
- Şarkı etiketlerini (başlık, sanatçı, albüm) düzenleme henüz yok.
- Yedekler aynı telefonda geri yüklemek içindir, çünkü şarkılara Android'in verdiği numaralarla bağlanır.

## Yol haritası

- İngilizce çeviri
- Hakkında ekranı ve güncelleme sonrası "Yenilikler" ekranı
- Cam görünümlü bir tema
- Etiket düzenleme

Planlar değişebilir. Fikirler ve hata bildirimleri çok değerli, lütfen bir issue aç.

## Katkı

Hata bildirmek ya da özellik önermek için issue aç. Kod değişikliği için pull request aç, ne yaptığını ve nasıl denediğini anlat.

## Lisans ve katkılar

Vortex Music, **GNU Genel Kamu Lisansı v3.0** (`LICENSE` dosyasına bak) ile lisanslıdır.

[AndroidX Media3](https://developer.android.com/media/media3) kullanır, o da Apache Lisansı 2.0 ile lisanslıdır.

Uygulama logosu yapay zekâ yardımıyla üretildi.
