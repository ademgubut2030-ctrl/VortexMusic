# 🎵 Vortex Music

> **Modern, Güçlü ve Akıllı Android Müzik Çalar**  
> *Müziği sadece dinlemeyin, detaylarını hissedin.*

---

## 🌟 Öne Çıkan Özellikler

### 🎛️ Gelişmiş Ses Engine & Ekolayzır (DSP)
- **5-Bant Hassas Ekolayzır:** 60 Hz, 230 Hz, 910 Hz, 3.6 kHz ve 14 kHz frekans ayarlarıyla müziğin her detayını kontrol edin.
- **Özel Hazır Ayarlar (Presets):** Düz, Doğal Ses, Bas Artır, Vokal ve daha fazlası.
- **DSP Efektleri:**
  - 🔊 **Bas Güçlendirici (Bass Boost)**
  - 📢 **Ses Yükseltme (Volume Gain)**
  - 🎧 **Sanal Surround (3D Audio)**
- **Kulaklık Profilleri:** Kulaklık modelinize göre equalizer ayarlarını kaydedin; Bluetooth bağlandığında otomatik yüklensin. Profile dışa/içe aktarma (Import/Export) desteği!

---

### 🧠 Akıllı Kütüphane & Düzenleme
- **Gözat & Arama:** Şarkı, sanatçı, albüm ve klasör bazlı hızlı erişim ve anlık arama.
- **Akıllı Listeler:**
  - ❤️ **Beğenilen Şarkılar**
  - 🆕 **Son Eklentiler**
  - 🔥 **En Çok Dinlenenler**
  - ⏳ **Unutulanlar** (Kütüphanenizde kalan gizli hazineleri yeniden keşfedin)
- **Klasör Yönetimi:** Şarkılarınızı cihazınızdaki klasör hiyerarşisine göre doğrudan oynatın.

---

### 🎨 Modern & Sezgisel Arayüz (UI)
- **Koyu Neon Tema:** Uzun süreli dinlemelerde gözü yormayan premium karanlık tasarım.
- **Mini Player & Alt Navigasyon Barı:** Şarkı çalarken her ekrandan hızlı kontrol ve akıcı sayfa geçişleri.
- **Görsel Tasarım:** Şık kart mimarisi ve dinamik renk geçişleri.

---

### 🛠️ Araçlar & Yedekleme
- **Yedekleme & Geri Yükleme:** Ayarlarınızı, profillerinizi ve çalma listelerinizi güvende tutun.

---

## 🛠️ Mimari & Proje Yapısı

Vortex Music, modüler ve sürdürülebilir bir kod mimarisi (`Kotlin`) üzerine inşa edilmiştir:

```text
VortexMusic/
├── Engine/     # DSP, Ses İşleme ve Ekolayzır Yönetimi
├── Ui/         # Jetpack / Modern Arayüz Bileşenleri ve Temalar
├── Library/    # Medya Taraması, Şarkı / Albüm / Sanatçı Yönetimi
├── Queue/      # Çalma Listesi ve Oynatma Kuyruğu
├── Backup/     # Profil ve Veri Yedekleme Araçları
├── Tools/      # Yardımcı Metotlar ve Araçlar
└── Models/     # Veri Modelleri
