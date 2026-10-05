# Değişiklikler

Ödev Takip'teki tüm önemli değişiklikler bu dosyada belgelenir.

Biçim [Keep a Changelog][keep-a-changelog] düzenindedir; sürüm adları
[Semantic Versioning][semver] ile uyumludur (büyük.küçük.yama).

## [1.1] - 2026-10-05

### Eklendi

- **Ders seçimi** — Ödev ekleme/düzenleme formunda "Ders" kartına basınca
  açılan alt panelden ders seçilir. Ders zorunludur: seçilmeden kaydetme
  çalışmaz, hata satırı görünür.
- **Filtreler düğmesi** — Ödevler sayfasında çiplerin sağında daima görünür.
  Açılan alt panelden "Tüm dersler" ya da tek bir ders seçilerek hem liste
  hem üstteki sayaçlar süzülür; seçilen ders silinirse filtre
  kendiliğinden sıfırlanır.
- **Dersler ekranı** — Ayarlar → *Ders ekle, değiştir*. Ders ekleme,
  büyük/küçük harf duyarsız yinelenen adı reddetme ve onay diyaloğuyla
  silme. Silinen dersin ödevleri etkilenmez; varsayılan 9 ders ilk açılışta
  eklenir.
- **Kartta ders etiketi** — Ödev kartında başlığın altında ders adı görünür.
- **Aciliyet rozeti ve kırmızı çerçeve** — Teslim gününe girilmiş ya da
  **yarın** teslimi olan ve hâlâ tamamlanmamış ödev, kırmızı çerçeveli bir
  kutucuğa dönüşür ve durum rozetinin soluna **Acil** / **Yarın teslim**
  rozeti düşer. Teslim tarihi geçmiş (geciken) ödev ikinci uyarı almaz —
  onun zaten kırmızı tarih metni ve "Gecikti" rozeti vardır. Tamamlanan
  ödev asla acil sayılmaz. Rozet Ödevler ve Takvim kartlarında ortaktır.

### Değişti

- Veritabanı sürümü **1 → 2**: `odevler` tablosuna `ders` kolonu eklendi
  (eski kayıtlar boş bırakılır) ve `dersler` tablosu açıldı. Yükseltme mevcut
  ödev verisini olduğu gibi korur.
- Ders filtresi yalnızca Ödevler listesini etkiler; **Takvim**, seçilen
  günün tüm ödevlerini göstermeye devam eder.

## [1.0] - 2026-10-04

İlk sürüm.

### Eklendi

- Ödev ekleme, düzenleme ve silme; **bekliyor / gecikti / tamamlandı**
  durumu teslim tarihinden otomatik hesaplanır.
- Filtre çipleri: **Tümü / Geciken / Bugün / Yaklaşan**.
- **Takvim** ekranı: güne göre ödev listesi ve günlük sayaçlar.
- **Room** ile yerel veritabanı, **WorkManager** ile arka plan durum
  güncellemesi.
- **Geciken ödev bildirimleri**; aynı ödev 12 saatte bir tekrar bildirilmez.
- **Türkçe yerel ayarı** sabit: tarih ve saat biçimleri cihaz dilinden
  bağımsız olarak Türkçe.
- **"Odak Mavisi" teması**; sistem dinamik rengi kapalı.
- **Arayüz yenilemesi**: alt gezinme, kart / çip / rozet / form yeniden
  tasarımı ve kaydırılabilir tarih şeridi.
- **Ayarlar**: tema seçici (Açık / Koyu / Sistem) ve "tamamlananları gizle".
- **Kaydırarak tamamla**: sağa = tamamla, sola = geri al; aynı eylem
  TalkBack erişilebilirlik menüsünden de verilebilir.
- **Teslim öncesi hatırlatma**: 3 saat önce / teslim günü sabahı / 1 gün
  önce — bunlardan birden fazlası aynı anda seçilebilir.
- Ayarlarda **bildirim izni durumu** satırı ve sistem ayarlarına kısayol.
- **Uygulama ikonu** (hiçbir hazır varlık içe aktarılmadan çizildi).
- README'ye tasarım atfı (Figma topluluk dosyası, **CC BY 4.0**).

[keep-a-changelog]: https://keepachangelog.com/tr/1.1.0/
[semver]: https://semver.org/lang/tr/
