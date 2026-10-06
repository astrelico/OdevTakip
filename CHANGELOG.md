# Değişiklikler

Ödev Takip'teki tüm önemli değişiklikler bu dosyada belgelenir.

Biçim [Keep a Changelog][keep-a-changelog] düzenindedir; sürüm adları
[Semantic Versioning][semver] ile uyumludur (büyük.küçük.yama).

## [1.2] - 2026-10-06

### Eklendi

- **Ders Programı** — Alt gezinmede üçüncü sekme: haftanın yedi günü için
  ders saatleri ızgarası. Gün çipleriyle gezinilir, satıra basınca alttan
  açılan panelden ders seçilir. Saat yok; satırlar "1. ders", "2. ders"
  … diye numaralanır ve seçilen ders haftanın her günü aynı yerde
  tekrar eder.
- **Programa ders ekleme / kaldırma** — Program ekranının altındaki
  **+ Ders ekle** satırı 9., 10., … dersi ekler. Yanındaki **Son dersi
  kaldır** satırı da 8. satıra kadar inebilir ve gün **0'a kadar
  boşaltılabilir**; boş gün artık metinle uyarılır. Dolu bir satır
  kaldırılacaksa önce onay istenir. Ders kaldırma **ilk 8 ders için de**
  geçerlidir.
- **Dosya / fotoğraf eki** — Ödev formundaki **Ek ekle** düğmesi tek bir
  menüyle iki yolu da açar: **Fotoğraf seç** sistem Photo Picker'ını
  (izin gerektirmez), **Dosya seç** de belge seçicisini. Dosya
  uygulamanın **özel** deposuna kopyalanır — ekran kapatılsa da okunur —
  ve ödev silinince ek de silinir, değiştirilince eskisi temizlenir.
  Detay ekranının altında **görsel önizleme** görünür (görsel olmayanda
  ad + tür + boyut kartı), fotoğrafın üzerine basılınca **tam ekran**
  açılır. Ana ekrandaki kartta ise ataş ikonuyla **Ekli** etiketi durur.

### Değişti

- **+ (Yeni ödev) düğmesi** yalnızca Ödevler ve Takvim'de görünür; Ders
  Programı ekranında gizlenir ki program satırlarıyla çakışmasın.
- Veritabanı sürümü **2 → 4**: `program` tablosu açıldı ve `odevler`
  tablosuna `ek` kolonu eklendi. Yükseltme mevcut ödev ve ders verisini
  olduğu gibi korur.

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
[1.2]: https://github.com/astrelico/OdevTakip/releases/tag/v1.2
[1.1]: https://github.com/astrelico/OdevTakip/releases/tag/v1.1
