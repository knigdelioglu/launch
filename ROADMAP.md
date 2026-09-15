# Seyir — Roadmap

Bu roadmap, Seyir'i boş repodan günlük kullanılabilir ve yayınlanabilir bir Android TV launcher'a götüren geliştirme sırasını tanımlar.

Her faz bir öncekinin kabul kriterlerini korumalıdır. Yeni özellik uğruna focus kararlılığı, açılış süresi veya idle kaynak tüketimi gerilememelidir.

## Durum Etiketleri

- `[ ]` başlanmadı
- `[~]` devam ediyor
- `[x]` tamamlandı
- `[!]` bloke / karar gerekli

---

## M0 — Bootstrap ve HOME Prototipi

**Amaç:** Android TV'de gerçekten launcher olarak seçilebilen en küçük çalışan uygulamayı üretmek.

### İşler

- [ ] Android/Kotlin proje iskeletini oluştur
- [ ] Jetpack Compose + Compose for TV kur
- [ ] package/application id belirle
- [ ] `minSdk` hedef cihaz üzerinden doğrula
- [ ] güncel stable `compileSdk/targetSdk` seç
- [ ] `ACTION_MAIN + CATEGORY_HOME + CATEGORY_DEFAULT` manifest tanımı
- [ ] TV banner/icon placeholder ekle
- [ ] landscape-only davranışı doğrula
- [ ] tek ekranlık Home prototipi
- [ ] D-pad ile en az 4 test kartında focus hareketi
- [ ] HOME tuşuyla geri dönüş testi
- [ ] debug APK üret
- [ ] gerçek TV box üzerinde varsayılan launcher seçimini test et

### Çıkış kriterleri

- [ ] Cihaz Seyir'i Home uygulaması olarak seçebiliyor.
- [ ] Boot/Home sonrası launcher crash olmadan açılıyor.
- [ ] D-pad focus görünür ve kaybolmuyor.
- [ ] En az bir gerçek cihazda test edildi.

---

## M1 — Uygulama Keşfi ve Açma

**Amaç:** Kurulu uygulamaları güvenilir şekilde bulmak ve launch etmek.

### İşler

- [ ] `InstalledAppRepository` oluştur
- [ ] `PackageManager` sorgularını UI katmanından ayır
- [ ] TV launch intent'lerini tercih et
- [ ] normal launch intent fallback davranışını tanımla
- [ ] launch edilemeyen paketleri filtrele
- [ ] uygulama adı ve ikonunu normalize et
- [ ] sistem/self paket filtreleri
- [ ] uygulama yükleme/kaldırma değişikliklerini algılama stratejisi
- [ ] Tüm Uygulamalar grid'i
- [ ] OK ile uygulama açma
- [ ] başarısız launch için kullanıcı dostu hata

### Testler

- [ ] 10 uygulamalı cihaz
- [ ] 50+ uygulamalı cihaz/emülatör
- [ ] kaldırılan uygulama
- [ ] devre dışı paket
- [ ] launch intent'i olmayan paket
- [ ] ikon yükleme hatası

### Çıkış kriterleri

- [ ] Görünen her kart geçerli şekilde açılabiliyor veya kontrollü hata veriyor.
- [ ] Grid hızlı D-pad kullanımında focus kaybetmiyor.
- [ ] Paket kaldırma launcher state'ini bozmuyor.

---

## M2 — Favoriler, Sıralama ve Gizleme

**Amaç:** Kullanıcıya ana ekranın gerçek kontrolünü vermek.

### İşler

- [ ] Jetpack DataStore ekle
- [ ] versionlanabilir launcher preferences modeli
- [ ] favoriye ekle
- [ ] favoriden çıkar
- [ ] favori sıralama
- [ ] uygulama gizleme
- [ ] gizleneni geri getirme
- [ ] kaldırılmış paketleri stale state'ten temizleme
- [ ] uzun OK context menu
- [ ] Uygulama Bilgisi intent'i
- [ ] Android uninstall intent'i (opsiyonel menü aksiyonu)

### UX

- [ ] context menu focus trap doğru çalışıyor
- [ ] menü kapanınca focus kaynak karta dönüyor
- [ ] taşıma modu kumandayla anlaşılır
- [ ] destructive aksiyonlarda yanlış tetikleme önleniyor

### Çıkış kriterleri

- [ ] Favori/gizli state process restart sonrası korunuyor.
- [ ] Uygulama kaldırıldığında stale kayıt sorun yaratmıyor.
- [ ] Tüm yönetim işlemleri yalnız kumandayla yapılabiliyor.

---

## M3 — Seyir Design System ve Premium TV UI

**Amaç:** Prototipi Apple TV/Google TV seviyesinde sakin ve profesyonel bir arayüze dönüştürmek.

### Design system

- [ ] spacing scale
- [ ] typography scale
- [ ] corner radius tokenları
- [ ] focus scale tokenları
- [ ] elevation/shadow yaklaşımı
- [ ] motion duration/easing tokenları
- [ ] Dark palette
- [ ] Black palette
- [ ] accent altyapısı

### Home UI

- [ ] üst durum alanı
- [ ] saat
- [ ] favori satırı
- [ ] Tüm Uygulamalar girişi
- [ ] Ayarlar girişi
- [ ] boş state
- [ ] ilk focus davranışı
- [ ] focus restore

### Focus kalitesi

- [ ] sağ/sol geçişleri deterministik
- [ ] yukarı/aşağı geçişleri deterministik
- [ ] focus off-screen kalmıyor
- [ ] scroll sırasında focus zıplamıyor
- [ ] hızlı D-pad spam testinden geçiyor
- [ ] dialog/menu kapanınca doğru focus geri geliyor

### Motion

- [ ] focus scale yaklaşık 1.06–1.08 aralığında tune et
- [ ] 150–200 ms sınıfında animasyonları tune et
- [ ] reduced motion seçeneği için temel altyapı
- [ ] gereksiz blur/glow kaldır

### Çıkış kriterleri

- [ ] Ana ekran ürün kalitesinde görünüyor.
- [ ] Focus animasyonları düşük güçlü cihazı belirgin yavaşlatmıyor.
- [ ] 1080p ve 4K'da layout taşmıyor.
- [ ] Dokunmatik olmadan tüm ana akış tamamlanabiliyor.

> **M3 tamamlandığında Seyir günlük kullanılabilir ilk gerçek sürüm sayılır.**

---

## M4 — Ayarlar ve Onboarding

**Amaç:** Launcher'ın kurulumunu ve kişiselleştirmesini uygulama içinden tamamlamak.

### Onboarding

- [ ] kısa hoş geldiniz ekranı
- [ ] favori seçim ekranı
- [ ] varsayılan launcher yönlendirmesi
- [ ] ilk kurulum tamamlandı state'i
- [ ] onboarding'i tekrar açma

### Ayarlar

- [ ] Görünüm
- [ ] Favoriler
- [ ] Gizlenen uygulamalar
- [ ] Başlangıç
- [ ] Sistem
- [ ] Hakkında

### Görünüm

- [ ] Dark
- [ ] Black
- [ ] kart boyutu
- [ ] animasyon normal/azaltılmış
- [ ] arka plan seçimi için altyapı

### Sistem kısayolları

- [ ] Android Settings
- [ ] Wi-Fi settings intent'i
- [ ] Bluetooth settings intent'i (cihaz destekliyorsa)
- [ ] app details

### Çıkış kriterleri

- [ ] İlk kurulum kumandayla tamamlanabiliyor.
- [ ] Ayarlar restart sonrası korunuyor.
- [ ] Sistem ekranlarına geçiş vendor ROM farklarında kontrollü fallback sağlıyor.

---

## M5 — Recent Apps ve Home İyileştirmeleri

**Amaç:** Sadelikten ödün vermeden günlük erişimi hızlandırmak.

### İşler

- [ ] Seyir üzerinden açılan uygulamaları yerel recent listesine yaz
- [ ] favorilerde olan uygulamayı recent satırında tekrar göstermeme seçeneği
- [ ] recent limit belirle
- [ ] recent temizleme
- [ ] feature toggle
- [ ] ana ekran boşluk/focus tuning

### Opsiyonel araştırma

- [ ] `UsageStatsManager` değerini değerlendir
- [ ] gereksiz özel izin gerekiyorsa kullanma

### Çıkış kriterleri

- [ ] Recent özelliği ekstra sistem izni olmadan anlamlı çalışıyor veya kapsam dışına alınıyor.
- [ ] Ana ekran bilgi kalabalığına dönüşmüyor.

---

## M6 — AirPlay Teknik Spike ve Lisans Kapısı

**Amaç:** UxPlay tabanlı receiver'ın Seyir'e teknik ve hukuki olarak nasıl eklenebileceğini kanıtlamak.

Bu faz doğrudan production entegrasyonu değildir.

### Lisans

- [ ] UxPlay GPL-3.0 yükümlülüklerini incele
- [ ] Android AirPlay server referans projelerinin lisanslarını incele
- [ ] JNI/native entegrasyonun türev eser etkisini değerlendir
- [ ] Seyir'in dağıtım lisansı için karar oluştur
- [ ] kaynak kod dağıtım yükümlülüklerini dokümante et
- [ ] `GO / NO-GO` kararı

### Teknik spike

- [ ] native library Android build
- [ ] ARM64 hedefi
- [ ] gerekirse ARMv7 hedefi
- [ ] JNI minimal bridge
- [ ] mDNS discovery
- [ ] iPhone'da receiver'ın görünmesi
- [ ] H.264 tek oturum
- [ ] ses prototipi
- [ ] session lifecycle

### Çıkış kriterleri

- [ ] Lisans kararı yazılı.
- [ ] Test cihazı iPhone tarafından AirPlay hedefi olarak görülebiliyor.
- [ ] En az bir başarılı gerçek ekran yansıtma oturumu var.
- [ ] Spike'ın production mimarisi için risk listesi hazır.

---

## M7 — AirPlay Production Entegrasyonu

**Ön koşul:** M6 `GO`.

### Service

- [ ] AirPlay enable/disable ayarı
- [ ] receiver adı (`Salon TV` vb.)
- [ ] servis lifecycle
- [ ] Android background execution kurallarına uyum
- [ ] ağ değişimi yönetimi
- [ ] yeniden ilan/discovery

### Video

- [ ] H.264 MediaCodec
- [ ] HEVC capability detection
- [ ] Surface lifecycle
- [ ] orientation/aspect handling
- [ ] 1080p test
- [ ] destekliyorsa 4K test

### Audio

- [ ] desteklenen codec akışı
- [ ] audio focus
- [ ] A/V sync
- [ ] mute/volume davranışı

### UX

- [ ] Home üzerinde AirPlay durum göstergesi
- [ ] bağlantı ekranı
- [ ] receiver activity
- [ ] disconnect sonrası Home'a güvenli dönüş
- [ ] bağlantı hatası UI
- [ ] opsiyonel PIN

### Dayanıklılık

- [ ] sender bağlantıyı kesiyor
- [ ] Wi-Fi kopuyor
- [ ] uygulama process'i yeniden yaratılıyor
- [ ] ekran kapanıyor/açılıyor
- [ ] arka arkaya bağlantılar
- [ ] uzun süreli playback

### Çıkış kriterleri

- [ ] iPhone/iPad/Mac temel mirroring doğrulandı.
- [ ] Disconnect sonrası launcher sağlam kalıyor.
- [ ] AirPlay kapalıyken gereksiz native/service maliyeti yok.
- [ ] DRM bypass yapılmıyor.
- [ ] lisans bildirimleri release paketinde doğru.

---

## M8 — Ambient Mode ve Görsel Kişiselleştirme

**Amaç:** TV boşta kaldığında sade bir ekran sunmak.

### İşler

- [ ] inactivity timer
- [ ] saat/tarih
- [ ] düşük hareketli layout
- [ ] OLED için pixel drift yaklaşımı
- [ ] kullanıcının kapatabilmesi
- [ ] yerel wallpaper
- [ ] özel wallpaper seçimi
- [ ] Cinematic mode prototipi
- [ ] GPU/memory profiling

### Opsiyonel

- [ ] hava durumu plugin'i

Hava durumu eklenirse:

- default kapalı
- kullanıcı açıkça etkinleştirir
- launcher temel işlevi API'ye bağımlı olmaz

### Çıkış kriterleri

- [ ] Ambient Mode launcher performansını etkilemiyor.
- [ ] statik OLED öğeleri uzun süre aynı yerde kalmıyor.
- [ ] ağ olmadan temel ambient deneyim çalışıyor.

---

## M9 — Stabilizasyon, Profiling ve Release Engineering

**Amaç:** Feature-complete build'i v1.0 seviyesine getirmek.

### Performans

- [ ] cold start ölçümü
- [ ] warm start ölçümü
- [ ] idle RAM ölçümü
- [ ] idle CPU ölçümü
- [ ] frame jank ölçümü
- [ ] 50+ app grid stress
- [ ] hızlı D-pad stress
- [ ] memory leak kontrolü

### Uyumluluk matrisi

En az:

- [ ] hedef kişisel TV box
- [ ] Android TV/Google TV referans cihaz veya emulator
- [ ] 1080p
- [ ] 4K
- [ ] farklı density değerleri

### Güvenlik / gizlilik

- [ ] manifest permission audit
- [ ] dependency audit
- [ ] secret scan
- [ ] network call audit
- [ ] analytics/advertising dependency bulunmadığını doğrula
- [ ] release signing prosedürü

### Release engineering

- [ ] versioning stratejisi
- [ ] changelog
- [ ] signed APK
- [ ] checksum
- [ ] GitHub Release workflow
- [ ] temiz cihaz kurulum testi
- [ ] upgrade testi
- [ ] rollback dokümantasyonu

### Dokümantasyon

- [ ] README güncel
- [ ] SCOPE güncel
- [ ] ROADMAP güncel
- [ ] kurulum adımları
- [ ] varsayılan launcher seçimi
- [ ] ADB fallback (gerekiyorsa)
- [ ] AirPlay sınırlamaları (varsa)
- [ ] lisans bildirimleri

---

# v1.0 — Stable

v1.0 adayının minimum özellik seti:

- [ ] Home launcher
- [ ] uygulama discovery/launch
- [ ] favoriler
- [ ] sıralama
- [ ] gizleme
- [ ] Tüm Uygulamalar
- [ ] Ayarlar
- [ ] premium focus-first UI
- [ ] kalıcı local preferences
- [ ] offline temel kullanım
- [ ] gerçek donanım testleri
- [ ] sıfır reklam
- [ ] sıfır zorunlu analytics/telemetry

AirPlay, M6/M7'nin risk ve lisans sonuçlarına göre v1.0'a dahil edilebilir veya v1.1'e ertelenebilir. Launcher'ın release'i AirPlay'e bağımlı değildir.

## v1.0 Release Gate

- [ ] `SCOPE.md` içindeki v1.0 kabul kriterleri tamamlandı.
- [ ] blocker/critical bug yok.
- [ ] bilinen önemli vendor uyumsuzlukları dokümante.
- [ ] release APK gerçek cihazda temiz kurulumdan geçti.
- [ ] Home seçimi ve HOME tuşu davranışı doğrulandı.
- [ ] performans hedeflerinde kritik regresyon yok.

---

# Post-1.0 Backlog

Bunlar taahhüt değildir; v1.0 sonrasında ayrı değerlendirilir.

- [ ] uygulama arama
- [ ] sesli uygulama arama
- [ ] plugin/provider API
- [ ] Kodi/Plex/Nuvio gibi uygulamalarla kontrollü deep-link entegrasyonları
- [ ] gelişmiş wallpaper sistemi
- [ ] farklı Home layout preset'leri
- [ ] çoklu profil
- [ ] ebeveyn kontrolü
- [ ] local backup/export-import
- [ ] launcher ayarlarını cihazlar arasında opsiyonel senkronlama
- [ ] Fire TV uyumluluk araştırması

## Bilinçli Olarak Eklenmeyecek Özellikler

- reklam
- sponsorlu feed
- zorunlu hesap
- kullanıcı davranışı takibi
- DRM bypass
- üçüncü taraf servislerden izinsiz veri scraping

---

## İlk Uygulama Sırası

Kodlamaya başlarken önerilen sıra:

```text
M0 HOME prototype
   ↓
M1 app discovery
   ↓
M2 favorites + persistence
   ↓
M3 design system + focus polish
   ↓
M4 settings/onboarding
   ↓
M5 recent apps
   ↓
M6 AirPlay spike + license gate
```

M0–M3 tamamlanmadan AirPlay entegrasyonuna girilmemelidir. Seyir'in ana ürün değeri önce sağlam bir launcher olmaktır.
