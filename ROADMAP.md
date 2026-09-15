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

- [x] Android/Kotlin proje iskeletini oluştur
- [x] Jetpack Compose + Compose for TV kur
- [x] package/application id belirle (`io.github.knigdelioglu.seyir`)
- [ ] `minSdk` hedef cihaz üzerinden doğrula
- [x] güncel stable `compileSdk/targetSdk` seç
- [x] `ACTION_MAIN + CATEGORY_HOME + CATEGORY_DEFAULT` manifest tanımı
- [~] TV banner/icon placeholder ekle (ikon mevcut, TV banner bekliyor)
- [~] landscape-only davranışı doğrula (manifest kilidi mevcut, cihaz testi bekliyor)
- [x] tek ekranlık Home prototipi
- [~] D-pad focus davranışı (kodlandı, cihaz testi bekliyor)
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

- [x] `InstalledAppRepository` oluştur
- [x] `PackageManager` sorgularını UI katmanından ayır
- [x] TV launch intent'lerini tercih et
- [x] normal launch intent fallback davranışını tanımla
- [x] launch edilemeyen paketleri filtrele
- [x] uygulama adı ve ikonunu normalize et
- [~] sistem/self paket filtreleri (self filtrelendi; sistem uygulaması politikası M1 içinde netleştirilecek)
- [ ] uygulama yükleme/kaldırma değişikliklerini algılama stratejisi
- [ ] Tüm Uygulamalar grid'i
- [x] OK ile uygulama açma
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

- [~] üst durum alanı (isim + saat ilk prototipte mevcut)
- [x] saat
- [ ] favori satırı
- [ ] Tüm Uygulamalar girişi
- [ ] Ayarlar girişi
- [x] boş state
- [x] ilk focus davranışı
- [ ] focus restore

### Focus kalitesi

- [~] sağ/sol geçişleri deterministik (tek satır prototip, cihaz testi bekliyor)
- [ ] yukarı/aşağı geçişleri deterministik
- [ ] focus off-screen kalmıyor
- [ ] scroll sırasında focus zıplamıyor
- [ ] hızlı D-pad spam testinden geçiyor
- [ ] dialog/menu kapanınca doğru focus geri geliyor

### Motion

- [x] focus scale yaklaşık 1.06–1.08 aralığında tune et (başlangıç: 1.06)
- [x] 150–200 ms sınıfında animasyonları tune et (başlangıç: 160 ms)
- [ ] reduced motion seçeneği için temel altyapı
- [x] gereksiz blur/glow kaldır

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

- [ ] görünüm ayarları
- [ ] favori yönetimi
- [ ] gizli uygulamalar yönetimi
- [ ] başlangıç davranışı
- [ ] sistem ayarlarına güvenli kısayollar
- [ ] hakkında / sürüm ekranı

### Görünüm

- [ ] Dark / Black tema
- [ ] wallpaper: minimal / özel
- [ ] kart boyutu seçenekleri
- [ ] animasyon: normal / azaltılmış
- [ ] overscan-safe padding ayarı gerekiyorsa ekle

### Çıkış kriterleri

- [ ] İlk kurulum yalnız D-pad ile tamamlanabiliyor.
- [ ] Ayar değişiklikleri anında uygulanıyor ve restart sonrası korunuyor.

---

## M5 — Recent Apps ve Yaşam Döngüsü

**Amaç:** Sade kalırken günlük erişimi hızlandırmak ve launcher yaşam döngüsünü sağlamlaştırmak.

### İşler

- [ ] güvenilir recent apps veri kaynağını belirle
- [ ] yalnız uygulanabilir cihaz/API davranışını kullan
- [ ] favori tekrarlarını filtrele
- [ ] paket değişiklik receiver/observer stratejisi
- [ ] ekran açma/kapatma yaşam döngüsü
- [ ] process recreation testi
- [ ] low-memory recreation testi
- [ ] boot sonrası davranış

### Çıkış kriterleri

- [ ] Recent satırı yanlış veya stale uygulama göstermiyor.
- [ ] Launcher uzun süre açık kaldığında state bozulmuyor.

---

## M6 — AirPlay Teknik Spike ve Lisans Kapısı

**Amaç:** AirPlay entegrasyonunun teknik ve lisans açısından Seyir'e uygunluğunu kanıtlamak; launcher geliştirmesini buna kilitlememek.

### Spike

- [ ] güncel UxPlay lisansını ve Android portlarının lisanslarını yeniden doğrula
- [ ] GPL-3.0 yükümlülüklerinin dağıtım modelimize etkisini dokümante et
- [ ] entegrasyon seçeneklerini değerlendir:
  - [ ] ayrı uygulama / companion receiver
  - [ ] ayrı process/service
  - [ ] native kütüphane/JNI bundle
- [ ] iOS 27 ile discovery testi
- [ ] iPhone ekran yansıtma testi
- [ ] H.264 testi
- [ ] HEVC testi
- [ ] ses testi
- [ ] disconnect/reconnect testi
- [ ] idle güç/RAM ölçümü
- [ ] DRM/FairPlay sınırlarını kullanıcıya doğru ifade et

### Karar kapısı

- [ ] **GO:** lisans + teknik model kabul edildi
- [ ] **NO-GO:** AirPlay core launcher'dan ayrı tutulacak

> UxPlay veya GPL uyumlu başka bir implementasyon doğrudan uygulamaya bundle edilmeden önce bu faz kapanmalıdır.

---

## M7 — AirPlay Entegrasyonu

**Önkoşul:** M6 = GO.

### İşler

- [ ] AirPlay receiver service
- [ ] mDNS/Bonjour advertisement
- [ ] cihaz adı ayarı (`Salon TV` vb.)
- [ ] receiver lifecycle
- [ ] bağlantı durum modeli
- [ ] incoming session UI
- [ ] MediaCodec video decode
- [ ] audio output
- [ ] session bitince Home'a güvenli dönüş
- [ ] PIN seçeneği
- [ ] foreground-service gereksinimlerini güncel Android kurallarına göre uygula
- [ ] AirPlay kapalıyken sıfır/çok düşük idle maliyet

### Çıkış kriterleri

- [ ] iPhone/iPad/Mac cihaz listesinde Seyir görünür.
- [ ] Mirroring bağlantısı tekrar tekrar güvenilir kurulabilir.
- [ ] Session bitince launcher focus/state'i bozulmaz.

---

## M8 — Performans, Uyumluluk ve Sertleştirme

**Amaç:** Seyir'i gerçek TV box çeşitliliğinde güvenilir hale getirmek.

### Performans hedefleri

- [ ] idle CPU yaklaşık `%0`
- [ ] launcher idle RAM hedefi `< 100 MB`
- [ ] 60 fps focus/scroll
- [ ] cold launch hedefi `< 1 s`
- [ ] focus response hedefi `< 50 ms`
- [ ] gereksiz wake lock yok
- [ ] background polling yok

### Cihaz matrisi

- [ ] Android TV sertifikalı cihaz
- [ ] AOSP TV box
- [ ] Android 8 sınıfı eski cihaz
- [ ] güncel Android TV sürümü
- [ ] 1080p
- [ ] 4K
- [ ] düşük RAM cihaz

### Kalite

- [ ] StrictMode/debug kontrolleri
- [ ] baseline profile değerlendir
- [ ] Macrobenchmark
- [ ] startup benchmark
- [ ] Compose recomposition incelemesi
- [ ] memory leak kontrolü
- [ ] crash-free uzun süreli test

---

## M9 — Dağıtım ve Güncelleme

**Amaç:** Seyir'i güvenli ve sürdürülebilir şekilde dağıtmak.

### İşler

- [ ] release signing
- [ ] reproducible/reviewable release süreci
- [ ] GitHub Actions CI
- [ ] lint + unit test + assemble
- [ ] release APK artifact
- [ ] GitHub Releases
- [ ] SHA-256 checksum
- [ ] changelog
- [ ] isteğe bağlı uygulama içi update kontrolü
- [ ] update kontrolü kapatılabilir olmalı

### Gizlilik kuralı

Update kontrolü eklenirse yalnız sürüm metadata'sı sorgulanmalı; telemetry veya analytics'e dönüşmemelidir.

---

## v1.0 — Kabul Kriterleri

Seyir 1.0 aşağıdakilerin tamamı sağlanmadan yayınlanmış sayılmaz:

- [ ] Android TV/TV box üzerinde varsayılan HOME launcher olarak kullanılabiliyor
- [ ] yalnız D-pad ile tam kullanım
- [ ] favori ekleme/çıkarma/sıralama
- [ ] uygulama gizleme/geri getirme
- [ ] deterministic focus
- [ ] hızlı ve stabil uygulama açma
- [ ] 1080p + 4K doğrulaması
- [ ] reklam yok
- [ ] telemetry yok
- [ ] zorunlu hesap yok
- [ ] launcher core için internet zorunluluğu yok
- [ ] release APK + checksum
- [ ] lisanslar ve üçüncü taraf bildirimleri doğru
- [ ] AirPlay dahilse M6 lisans kapısı tamamlanmış
