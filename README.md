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

## Planlanan Ana Ekran

```text
┌──────────────────────────────────────────────────────────────┐
│  21:42                                      Wi‑Fi ●   ⚙      │
│                                                              │
│                    İyi akşamlar                              │
│                                                              │
│       ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐            │
│       │ NUVIO  │ │YOUTUBE │ │ IPTV   │ │ KODI   │            │
│       └────────┘ └────────┘ └────────┘ └────────┘            │
│                                                              │
│       Netflix    Spotify    Dosyalar    Tüm Uygulamalar       │
│                                                              │
│  AirPlay ● Salon TV hazır                                    │
└──────────────────────────────────────────────────────────────┘
```

## Teknoloji Yığını

- Kotlin
- Jetpack Compose
- Compose for TV
- Android `PackageManager`
- Jetpack DataStore
- Kotlin Coroutines / Flow
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
└── airplay                # sonraki faz
    ├── service
    ├── jni
    └── native
```

Kesin modül yapısı ilk Android prototipi sırasında ihtiyaçlara göre sadeleştirilebilir.

## MVP

İlk günlük kullanılabilir sürüm şu kapsamla sınırlandırılır:

- Varsayılan Android `HOME` activity
- Kurulu TV uygulamalarını listeleme
- Uygulama açma
- Favoriler
- Favori sıralama
- Uygulama gizleme
- Tüm Uygulamalar ekranı
- Ayarlar
- Kumanda/D-pad focus sistemi
- Minimal koyu tema
- Kalıcı yerel ayarlar

AirPlay, içerik sağlayıcı entegrasyonları ve Ambient Mode MVP sonrasındadır.

## Performans Hedefleri

| Metrik | Hedef |
|---|---:|
| Idle RAM | `< 100 MB` hedef |
| Idle CPU | yaklaşık `%0` |
| UI | `60 FPS` |
| Cold launch | `< 1 sn` hedef |
| Focus tepkisi | `< 50 ms` hedef |
| Temel kullanım için internet | Gerekmez |

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

Ağ erişimi yalnızca açıkça ağ gerektiren özellikler için kullanılacaktır.

## Dokümantasyon

- [SCOPE.md](SCOPE.md) — proje kapsamı, gereksinimler ve sınırlar
- [ROADMAP.md](ROADMAP.md) — geliştirme fazları ve kabul kriterleri

## Durum

**Aşama:** Planlama / bootstrap

İlk hedef: çalışan bir Android TV `HOME` prototipi ve güvenilir D-pad focus davranışı.

## Lisans

Proje lisansı henüz seçilmedi. AirPlay/UxPlay entegrasyon yöntemi netleşmeden önce ana proje lisansı kesinleştirilmelidir.
