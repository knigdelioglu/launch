# Seyir

Seyir; Android TV ve Android tabanlı TV box cihazları için geliştirilen, sade, hızlı, reklamsız ve kumanda odaklı bir launcher projesidir.

Amaç, Google TV'nin işlevselliğini Apple TV'nin görsel sakinliğiyle birleştiren; ancak reklam, sponsorlu içerik, öneri kalabalığı, zorunlu hesap veya telemetri içermeyen bir ana ekran oluşturmaktır.

> TV'yi aç → istediğin uygulamayı seç → izle.

## Hedefler

- Android TV / TV box cihazlarında varsayılan `HOME` launcher olarak çalışmak
- D-pad ve TV kumandasıyla kusursuz focus navigasyonu
- Hızlı açılış, düşük RAM ve düşük CPU kullanımı
- Favori uygulamalar, tüm uygulamalar ve son kullanılanlar
- Uygulama gizleme ve sıralama
- İsteğe bağlı **Bugün ne var** satırıyla günün öne çıkan futbol maçlarını göstermek
- **Takımlarım** ile kullanıcının seçtiği takımların maçlarını önceliklendirmek
- Minimal ve Cinematic görünüm modları
- Yerel ayarlar; zorunlu hesap ve bulut bağımlılığı olmaması
- Reklam, sponsorlu öneri ve takip mekanizması içermemesi
- İlerleyen fazda AirPlay alıcısı entegrasyonu
- Düşük güçlü Android TV box cihazlarında da akıcı çalışma

## Tasarım İlkeleri

1. **İçerikten önce kontrol:** Ana ekranın sahibi kullanıcıdır.
2. **Sıfır reklam:** Sponsorlu satır, promosyon kartı veya reklam SDK'sı yoktur.
3. **Focus-first UI:** Her ekran kumanda ve D-pad için tasarlanır.
4. **Hız:** Launcher boşta neredeyse hiç CPU tüketmemelidir.
5. **Yerel çalışma:** Temel işlevler internet olmadan çalışmalıdır.
6. **Görsel sakinlik:** Büyük boşluklar, güçlü tipografi, sınırlı animasyon ve kontrollü efektler.
7. **Gizlilik:** Telemetri, reklam kimliği ve davranış takibi yoktur.

## Ana Ekran

Seyir'in ana ekranı uygulama odaklı kalır. Ağ tabanlı özellikler varsayılan olarak kapalıdır ve temel launcher kullanımını etkilemez.

```text
┌──────────────────────────────────────────────────────────────┐
│  SEYİR                                      ⚙        21:42   │
│                                                              │
│                    İyi akşamlar                              │
│                                                              │
│  Favoriler                                                   │
│  Nuvio   YouTube   IPTV   Kodi   Netflix   Tüm Uygulamalar   │
│                                                              │
│  Bugün ne var                                      Yenile    │
│  Süper Lig     Şampiyonlar Ligi     Premier League           │
│  20:00 ...     CANLI 1–0             22:00 ...               │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

## Bugün ne var

Bu özellik **opsiyoneldir**. Etkinleştirilmediğinde Seyir internete maç verisi için istek göndermez ve ana ekranda ilgili satır görünmez.

Veri kaynağı olarak [API-Football](https://www.api-football.com/) kullanılır.

- Kullanıcı kendi API-Football anahtarını **Ayarlar → Bugün ne var** ekranından girer.
- Anahtar kaynak koda, GitHub reposuna veya APK içine sabitlenmez.
- Android yedekleme kapalıdır; launcher tercihleri ve API anahtarı uygulamanın cihazdaki özel verisinde kalır.
- Fikstür cihazın saat dilimiyle istenir.
- Sonuçlar 30 dakika bellek önbelleğinde tutulur; arka planda sürekli polling yapılmaz.
- Home ekranında en fazla 12 maç gösterilir.
- Öncelik sırası: Takımlarım → Süper Lig / Türkiye Kupası / UEFA kupaları → diğer Türkiye ligleri → büyük Avrupa ligleri → diğer karşılaşmalar.
- Maç başlamadıysa saat, canlıysa dakika + skor, bittiyse maç sonu skoru gösterilir.

### Takımlarım

**Ayarlar → Bugün ne var → Takımlarım** ekranından takım adına göre arama yapılabilir.

- Arama kullanıcı `Ara` düğmesine bastığında çalışır; her klavye tuşunda API isteği yapılmaz.
- Takımlar API-Football team ID'siyle saklanır; yalnız isim eşleştirmesine güvenilmez.
- Seçimler DataStore'da cihazda kalıcıdır.
- Seçilen takımın o gün maçı varsa karşılaşma, 12 kartlık günlük listenin önüne taşınır.
- Art arda takım ekleme/çıkarma işlemleri fixture yenilemesinde kısa debounce ile birleştirilir; gereksiz API tüketimi azaltılır.

API-Football ücretsiz planı kişisel kullanım ve geliştirme için yeterli bir başlangıç noktasıdır; kota ve kullanım koşulları sağlayıcı tarafından değiştirilebilir.

## Teknoloji Yığını

- Kotlin
- Jetpack Compose
- Compose for TV
- Android `PackageManager`
- Jetpack DataStore
- Kotlin Coroutines / Flow
- `HttpURLConnection` + Android JSON (opsiyonel maç verisi)
- MediaSession entegrasyonu (gerektiğinde)
- AirPlay fazında JNI + native C/C++ katmanı

## Modüller

Planlanan üst seviye yapı:

```text
Seyir
├── app
│   ├── home
│   ├── apps
│   ├── settings
│   ├── onboarding
│   └── ambient
├── core
│   ├── model
│   ├── datastore
│   ├── launcher
│   └── design-system
├── sports                 # opsiyonel günlük maç verisi
└── airplay                # sonraki faz
    ├── service
    ├── jni
    └── native
```

Kesin modül yapısı geliştirme sırasında ihtiyaçlara göre sade tutulur.

## Güncel Çekirdek Özellikler

- Android `HOME` activity
- Kurulu TV uygulamalarını listeleme ve açma
- Favoriler ve D-pad ile favori sıralama
- Uygulama gizleme / geri getirme
- Tüm Uygulamalar ekranı
- Focus restore ve off-screen scroll-before-focus
- Dark / Black tema
- Nötr / Mavi / Zümrüt accent
- Reduced Motion
- Kalıcı yerel ayarlar
- Opsiyonel **Bugün ne var** futbol fikstürü
- **Takımlarım** takım arama, kalıcı seçim ve günlük maç önceliklendirmesi
- CI üzerinden debug APK artifact üretimi

AirPlay ve Ambient Mode sonraki fazlardadır.

## Performans Hedefleri

| Metrik | Hedef |
|---|---:|
| Idle RAM | `< 100 MB` hedef |
| Idle CPU | yaklaşık `%0` |
| UI | `60 FPS` |
| Cold launch | `< 1 sn` hedef |
| Focus tepkisi | `< 50 ms` hedef |
| Temel launcher kullanımı için internet | Gerekmez |

Bu değerler geliştirme sırasında gerçek donanım üzerinde ölçülerek doğrulanacaktır.

## AirPlay

İleri fazda launcher içine AirPlay receiver eklenmesi planlanmaktadır. Amaç, Seyir çalışırken TV'nin iPhone/iPad/Mac tarafında bir AirPlay hedefi olarak görünmesi ve bağlantı geldiğinde tam ekran receiver deneyimine geçmesidir.

UxPlay ve UxPlay tabanlı Android uygulamaları teknik referans olarak değerlendirilecektir. UxPlay GPL-3.0 lisanslı olduğundan, doğrudan kaynak kod entegrasyonu yapılmadan önce lisans uyumluluğu ve dağıtım modeli kesinleştirilecektir.

DRM/FairPlay korumalı servislerin desteklenmesi proje hedefi değildir.

## Gizlilik

Seyir'in varsayılan politikası:

- reklam yok
- reklam SDK'sı yok
- analytics yok
- telemetri yok
- Advertising ID yok
- zorunlu hesap yok
- zorunlu bulut servisi yok
- Android app backup kapalı

Ağ erişimi yalnızca kullanıcının açıkça etkinleştirdiği ağ gerektiren özellikler için kullanılır. `Bugün ne var` kapalıysa maç verisi isteği yapılmaz.

## Dokümantasyon

- [SCOPE.md](SCOPE.md) — proje kapsamı, gereksinimler ve sınırlar
- [ROADMAP.md](ROADMAP.md) — geliştirme fazları ve kabul kriterleri

## Durum

**Aşama:** M3 / M4 geçişi — günlük kullanılabilir launcher çekirdeği ve kişiselleştirme.

Kod tarafındaki sıradaki büyük hedefler onboarding, gerçek TV box doğrulaması ve ardından AirPlay teknik spike'ıdır.

## Lisans

Proje lisansı henüz seçilmedi. AirPlay/UxPlay entegrasyon yöntemi netleşmeden önce ana proje lisansı kesinleştirilmelidir.
