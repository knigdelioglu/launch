# Seyir

Seyir; Android TV ve Android tabanlı TV box cihazları için geliştirilen, sade, hızlı, reklamsız ve kumanda odaklı bir launcher projesidir.

Amaç, Google TV'nin işlevselliğini Apple TV'nin görsel sakinliğiyle birleştiren; ancak reklam, sponsorlu içerik, öneri kalabalığı, zorunlu hesap veya telemetri içermeyen bir ana ekran oluşturmaktır.

> TV'yi aç → istediğin uygulamayı seç → izle.

## Hedefler

- Android TV / TV box cihazlarında varsayılan `HOME` launcher olarak çalışmak
- D-pad ve TV kumandasıyla kusursuz focus navigasyonu
- Hızlı açılış, düşük RAM ve düşük CPU kullanımı
- Favori uygulamalar, tüm uygulamalar ve uygulama gizleme/sıralama
- İsteğe bağlı **Bugün ne var** satırıyla günün öne çıkan futbol maçlarını göstermek
- **Takımlarım** ile kullanıcının seçtiği takımların maçlarını önceliklendirmek
- Yerel ayarlar; zorunlu hesap ve bulut bağımlılığı olmaması
- Reklam, sponsorlu öneri ve takip mekanizması içermemesi
- İlerleyen fazda AirPlay alıcısı entegrasyonu

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
│  Bugün ne var                                                │
│  Süper Lig     Şampiyonlar Ligi     Premier League           │
│  20:00 ...     22:00 ...             22:30 ...               │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

## Bugün ne var

Bu özellik **opsiyoneldir**. Etkinleştirilmediğinde Seyir maç verisi için internete istek göndermez ve ana ekranda ilgili satır görünmez.

Veri kaynağı **API-Football (api-sports.io)** servisidir.

- Kullanıcı kendi API-Football anahtarını **Ayarlar → Bugün ne var** ekranından girer veya `local.properties` üzerinden derlemeye ekler.
- Anahtar kaynak koda veya GitHub reposuna yüklenmez.
- Android yedekleme kapalıdır; anahtar ve launcher tercihleri cihazdaki uygulama verisinde kalır.
- Seyir bir takvim gününde **en fazla 1 otomatik API isteği** yapar.
- Günün ilk uygun açılışında API-Football'dan günün fikstürü alınır.
- İstek başlatılmadan önce `dailyMatchAttemptDate` kalıcı yazılır. İlk istek hata verse bile uygulamayı veya TV'yi yeniden açmak aynı gün ikinci otomatik isteği oluşturmaz.
- Başarılı yanıt tarih + ham JSON + alınma zamanı ile DataStore'da kalıcı saklanır.
- Aynı gün sonraki tüm açılışlarda yalnız yerel cache okunur; süreç/cihaz yeniden başlatılsa dahi yeni API isteği yapılmaz.
- Yeni takvim gününde ilk açılış yeni günlük sorguyu tetikler.
- Home ekranında yalnızca Premier League Big Six (Arsenal, Chelsea, Liverpool, Manchester City, Manchester United, Tottenham), Galatasaray, Fenerbahçe, Beşiktaş, Real Madrid ve Barcelona'nın maçları gösterilir.
- Maçlar gün içindeki başlangıç saatine göre sıralanır; canlı veya bitmiş maçlarda skor/dakika yerine programdaki başlangıç saati gösterilir.
- Maç saatleri cihaz saat diliminden bağımsız olarak `Europe/Istanbul` (Türkiye saati) ile gösterilir.

### Takımlarım

**Ayarlar → Bugün ne var → Takımlarım** ekranı tamamen yereldir; takım eklemek API isteği üretmez.

- Kullanıcı takım adını yazar ve cihazda saklar.
- Seçilen takım bu sabit öne çıkan takım listesinde yer alıyorsa aynı başlangıç saatindeki eşleşmelerde yerel olarak öne alınır.
- Sabit listenin dışındaki takımlar günlük maç kartında gösterilmez.
- Takımlarım gün içinde değiştirilirse yeni API sorgusu yapılmaz; cache'de bulunan aynı saatli eşleşmeler yeniden sıralanır.

Bu tasarım kişisel kullanımda API kotasını korur: özellik açıkken normal koşullarda **günde 1 sorgu**.

## Teknoloji Yığını

- Kotlin
- Jetpack Compose
- Compose for TV
- Android `PackageManager`
- Jetpack DataStore
- Kotlin Coroutines / Flow
- `HttpURLConnection` + Android JSON
- API-Football REST API (opsiyonel günlük maç verisi)
- MediaSession entegrasyonu (gerektiğinde)
- AirPlay fazında JNI + native C/C++ katmanı

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
- Opsiyonel **Bugün ne var** günlük maç fikstürü
- **Takımlarım** yerel seçim ve günlük maç önceliklendirmesi
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

Ağ erişimi yalnızca kullanıcının açıkça etkinleştirdiği ağ gerektiren özellikler için kullanılır. `Bugün ne var` kapalıysa hiçbir ağ/API isteği yapılmaz.

## Dokümantasyon

- [SCOPE.md](SCOPE.md) — proje kapsamı, gereksinimler ve sınırlar
- [ROADMAP.md](ROADMAP.md) — geliştirme fazları ve kabul kriterleri

## Durum

**Aşama:** M3 / M4 geçişi — günlük kullanılabilir launcher çekirdeği ve kişiselleştirme.

Kod tarafındaki sıradaki büyük hedefler onboarding, gerçek TV box doğrulaması ve ardından AirPlay teknik spike'ıdır.

## Lisans

Proje lisansı henüz seçilmedi. AirPlay/UxPlay entegrasyon yöntemi netleşmeden önce ana proje lisansı kesinleştirilmelidir.
