# Seyir — Project Scope

Bu belge Seyir'in ürün sınırlarını, teknik ilkelerini ve v1.0 kapsamını tanımlar. Amaç, geliştirme sırasında projenin gereksiz özelliklerle büyümesini engellemek ve her faz için net kabul kriterleri oluşturmaktır.

## 1. Ürün Tanımı

Seyir, Android TV ve Android tabanlı TV box cihazlarında varsayılan ana ekran (`HOME`) olarak çalışacak, kumanda odaklı, hızlı, reklamsız ve yerel çalışan bir launcher'dır.

Temel ürün vaadi:

> TV'yi aç → uygulamanı seç → izle.

Seyir bir içerik platformu değildir. Kullanıcının kurulu uygulamalarına hızlı, öngörülebilir ve estetik erişim sağlayan bir sistem arayüzüdür.

## 2. Hedef Platform

### Birincil

- Android TV
- Google TV tabanlı ancak üçüncü taraf launcher'a izin veren cihazlar
- AOSP/Android tabanlı TV box cihazları
- D-pad / IR / Bluetooth kumanda kullanan cihazlar

### İkincil

- HDMI üzerinden kullanılan Android medya kutuları
- Dokunmatik olmayan büyük ekran Android cihazları

### Destek dışı

- Telefon/tablet launcher deneyimi
- Wear OS
- Fire TV için özel optimizasyon (ileride değerlendirilebilir)
- Apple tvOS

`minSdk` değeri hedef cihaz doğrulamasından sonra kesinleştirilecektir. Başlangıç varsayımı API 26'dır; gerçek donanım daha düşük API gerektirirse yeniden değerlendirilebilir.

## 3. Temel Ürün İlkeleri

### 3.1 Reklamsız

Seyir şunları içermez:

- banner/interstitial reklam
- sponsorlu uygulama satırı
- sponsorlu içerik önerisi
- reklam SDK'sı
- Advertising ID kullanımı

### 3.2 Yerel-first

Temel launcher işlevleri internet bağlantısı olmadan çalışmalıdır.

Yerel tutulacak veriler:

- favoriler
- uygulama sırası
- gizlenen uygulamalar
- tema tercihi
- launcher ayarları
- AirPlay ayarları (özellik eklendiğinde)

### 3.3 Focus-first

Her ekran TV kumandası için tasarlanır.

Zorunlu davranışlar:

- görünür focus state
- öngörülebilir sağ/sol/yukarı/aşağı geçişleri
- focus kaybı olmaması
- BACK davranışının tutarlı olması
- dokunmatik giriş gerektirmemesi

### 3.4 Hafiflik

Launcher arka planda içerik feed'i indirmemeli ve gereksiz servis çalıştırmamalıdır.

Performans hedefleri:

- idle RAM: `< 100 MB` hedef
- idle CPU: yaklaşık `%0`
- UI: 60 FPS hedef
- cold start: `< 1 sn` hedef
- focus tepki süresi: `< 50 ms` hedef

Bu değerler garanti değil, ölçülebilir mühendislik hedefleridir.

## 4. v1.0 Kapsamı

### 4.1 HOME Launcher

- `ACTION_MAIN`
- `CATEGORY_HOME`
- `CATEGORY_DEFAULT`
- Android tarafından varsayılan Home uygulaması olarak seçilebilme
- HOME tuşuyla Seyir'e dönme
- boot sonrası cihaz davranışının hedef donanımda doğrulanması

Üretici ROM'u üçüncü taraf launcher seçimini engelliyorsa bu durum uygulama hatası sayılmaz; desteklenen ADB tabanlı kurulum yöntemi ayrıca belgelenebilir.

### 4.2 Ana Ekran

Ana ekran en fazla birkaç yüksek değerli yüzey içerir:

1. üst durum alanı
2. favori uygulamalar
3. isteğe bağlı son kullanılanlar
4. Tüm Uygulamalar girişi
5. Ayarlar girişi
6. AirPlay durumu (özellik aktif olduğunda)

Varsayılan ekranda şunlar bulunmaz:

- haber
- reklam
- film/dizi öneri feed'i
- sponsorlu içerik
- sonsuz carousel

### 4.3 Uygulama Keşfi

Seyir `PackageManager` üzerinden launch edilebilir uygulamaları bulmalıdır.

Gereksinimler:

- kurulu uygulamaları listeleme
- TV uyumlu activity'leri tercih etme
- geçerli launch intent'i olmayan paketleri göstermeme
- kaldırılan uygulamaları yerel favori kayıtlarından güvenli biçimde temizleme
- yeni yüklenen uygulamaları yeniden taramada bulma

### 4.4 Favoriler

Kullanıcı:

- uygulamayı favoriye ekleyebilir
- favoriden çıkarabilir
- sırasını değiştirebilir
- favori sayısını makul sınırlar içinde özgürce belirleyebilir

Sıralama DataStore üzerinde kalıcı olmalıdır.

### 4.5 Uygulama Gizleme

Kullanıcı belirli uygulamaları launcher'da gizleyebilir.

Gizlenen uygulamalar:

- ana ekranda görünmez
- Tüm Uygulamalar ekranında normal listede görünmez
- Ayarlar > Gizlenen uygulamalar üzerinden geri getirilebilir

Sistem açısından paket kaldırılmaz veya devre dışı bırakılmaz.

### 4.6 Tüm Uygulamalar

- grid tabanlı TV düzeni
- hızlı D-pad navigasyonu
- uygulama adı + ikon
- alfabetik veya kullanıcı tarafından seçilen deterministik sıralama
- focus görünürlüğü
- uzun OK ile context menu

### 4.7 Context Menu

Uzun OK ile açılabilecek ilk seçenekler:

- Aç
- Favoriye ekle / Favoriden çıkar
- Taşı (favorilerdeyse)
- Gizle
- Uygulama bilgisi

`Kaldır` yalnız Android'in resmi uninstall intent'i üzerinden çağrılabilir; launcher sessiz kaldırma yapmaz.

### 4.8 Ayarlar

v1.0 ayar kategorileri:

- Görünüm
- Favoriler
- Gizlenen uygulamalar
- Başlangıç
- Sistem
- Hakkında

AirPlay özelliği hazır olduğunda:

- AirPlay

### 4.9 Görsel Modlar

#### Minimal

Varsayılan mod.

- koyu arka plan
- sınırlı efekt
- uygulama ikonlarına odak
- düşük GPU maliyeti

#### Cinematic

v1.0 için opsiyoneldir; tamamlanmazsa post-1.0'a ertelenebilir.

- daha büyük artwork/background kullanımı
- kontrollü blur/gradient
- focus alanında görsel zenginlik

Cinematic mod ağdan reklam veya içerik feed'i çekmez.

### 4.10 Tema

İlk sürüm:

- Dark
- Black
- accent color seçimi için altyapı

Açık tema ürünün önceliği değildir.

### 4.11 Son Kullanılanlar

Mümkün olduğu ölçüde launcher üzerinden açılan uygulamalar yerel olarak takip edilebilir.

Kapsam:

- yalnız Seyir üzerinden açılan uygulamaların sıralanması güvenilir kabul edilir
- sistem genelindeki kesin usage history için özel izin zorunlu kılınmaz
- `UsageStats` gerektiren özellik v1.0 temel gereksinimi değildir

## 5. AirPlay Kapsamı

AirPlay v1.0 için zorunlu değildir; ayrı bir faz olarak geliştirilir.

Hedef deneyim:

1. Seyir açılır.
2. AirPlay servisi etkinse cihaz ağda ilan edilir.
3. iPhone/iPad/Mac cihazı hedefi görür.
4. bağlantı geldiğinde receiver activity tam ekran açılır.
5. bağlantı bittiğinde kullanıcı Seyir ana ekranına döner.

### Planlanan teknik bileşenler

- background/foreground service gereksinim analizi
- mDNS / Bonjour ilanı
- native C/C++ AirPlay protokol katmanı
- JNI köprüsü
- Android MediaCodec
- ses playback katmanı
- MediaSession entegrasyonu

### AirPlay sınırları

Proje hedefi değildir:

- FairPlay DRM kırma
- Netflix/Apple TV+/benzeri DRM bypass
- Apple protokollerini taklit ederek güvenlik mekanizması aşma

### Lisans kapısı

UxPlay GPL-3.0 lisanslıdır. Bu nedenle UxPlay kaynak kodunun uygulama içine statik/dinamik entegrasyonu yapılmadan önce dağıtım lisansı ve türev eser yükümlülükleri incelenmelidir.

AirPlay fazının `GO/NO-GO` kriterlerinden biri lisans uyumluluğudur.

## 6. Ambient Mode

Post-MVP özellik.

Planlanan davranış:

- belirli süre kullanılmadığında devreye girer
- saat/tarih gösterebilir
- OLED yanığı riskini azaltmak için statik öğeler yavaşça hareket eder
- kullanıcı tamamen kapatabilir

Hava durumu gibi internet gerektiren özellikler varsayılan olarak kapalı ve opsiyonel olmalıdır.

## 7. Mimari Sınırlar

Tercih edilen yapı:

```text
UI (Compose for TV)
        ↓
ViewModel / State
        ↓
Repository
        ↓
PackageManager / DataStore / Android APIs
```

İlkeler:

- mümkün olduğunca tek yönlü veri akışı
- UI'dan doğrudan PackageManager çağrısı yapılmaması
- launcher state'in process restart sonrası yeniden kurulabilmesi
- Android framework bağımlılıklarının test edilebilir katmanlarla çevrelenmesi
- gereksiz dependency eklenmemesi

## 8. Veri Modeli

İlk veri ihtiyaçları basittir ve SQL veritabanı gerektirmez.

DataStore için örnek state:

```text
LauncherPreferences
├── favoritePackages[]
├── hiddenPackages[]
├── theme
├── homeMode
├── animationsEnabled
├── ambientEnabled
└── airPlayEnabled
```

Gerçek schema geliştirme sırasında versionlanmalıdır.

## 9. Güvenlik ve Gizlilik

### Zorunlu

- secret/keystore repo içine commit edilmez
- gereksiz Android permission eklenmez
- internet izni yalnız ağ özelliği gerçekten gerektiğinde eklenir
- üçüncü taraf SDK eklenecekse veri toplama davranışı incelenir
- uygulama listesi verisi cihaz dışına gönderilmez

### Varsayılan olarak yasak

- analytics SDK
- crash telemetry servisi
- advertising SDK
- cihaz fingerprinting
- uzaktan feature flag bağımlılığı

Hata raporlama eklenirse opt-in veya yerel export yaklaşımı tercih edilir.

## 10. Erişilebilirlik

- yeterli kontrast
- büyük okunabilir metin
- focus yalnız renkle ifade edilmemeli; ölçek/çerçeve gibi ikinci sinyal bulunmalı
- reduced motion seçeneği
- kritik bilgiler ikon tek başına kullanılmadan gerektiğinde metinle desteklenmeli

## 11. Performans Doğrulama

Gerçek cihaz üzerinde en az şu testler yapılmalıdır:

- cold start
- warm start
- 50+ uygulama ile grid kaydırma
- hızlı D-pad spam
- uygulama aç/kapa döngüsü
- düşük RAM koşulu
- 1080p ekran
- 4K ekran
- process death sonrası state restore

AirPlay fazında ayrıca:

- H.264 uzun oturum
- HEVC destekli cihaz testi
- ses/görüntü senkronu
- bağlantı kopması
- Wi-Fi değişimi
- sender yeniden bağlanması

## 12. v1.0 Dışında Tutulanlar

Aşağıdaki özellikler açıkça v1.0 kapsamı dışındadır:

- Google TV benzeri film/dizi recommendation engine
- launcher içinde streaming katalog araması
- kullanıcı hesabı
- cloud sync
- çoklu profil
- ebeveyn kontrolü
- IPTV player
- medya player
- dosya yöneticisi
- uygulama mağazası
- voice assistant geliştirme
- üçüncü taraf uygulamaların izleme geçmişini scraping
- DRM bypass
- root gerektiren sistem modifikasyonları

Bunlardan herhangi biri gelecekte ancak ayrı bir scope kararıyla eklenebilir.

## 13. v1.0 Kabul Kriterleri

Seyir v1.0 ancak aşağıdakilerin tamamı sağlandığında yayınlanabilir:

- [ ] Desteklenen cihazda varsayılan HOME launcher seçilebiliyor.
- [ ] HOME tuşu güvenilir biçimde Seyir'e dönüyor.
- [ ] Kurulu launch edilebilir uygulamalar doğru listeleniyor.
- [ ] Uygulamalar D-pad ile açılabiliyor.
- [ ] Focus hiçbir ana ekranda kaybolmuyor.
- [ ] Favori ekleme/çıkarma/sıralama kalıcı.
- [ ] Gizleme/geri getirme kalıcı.
- [ ] BACK davranışı tüm ekranlarda deterministik.
- [ ] Process restart ayarları bozmuyor.
- [ ] İnternet olmadan temel launcher işlevleri çalışıyor.
- [ ] Reklam/telemetri/analytics bulunmuyor.
- [ ] Release build secret içermiyor.
- [ ] 1080p ve 4K hedef ekranlarda layout taşmıyor.
- [ ] Kritik crash/blocker bug bulunmuyor.
- [ ] README ve kurulum dokümantasyonu güncel.

AirPlay v1.0'a dahil edilirse ayrıca AirPlay kabul kriterleri ROADMAP içinde tamamlanmalıdır.
