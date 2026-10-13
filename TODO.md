# TODO
- tandai x untuk semua task yang sudah selesai

## Segera
- [x] Isi `.env` dengan URL session pooler Supabase yang asli (`aws-0-ap-southeast-1`, Singapura → region Vercel `sin1`)
- [x] `mvn spring-boot:run` → pastikan Flyway membuat tabel di schema `bioskop` dan `ddl-auto=validate` lolos di PostgreSQL
    - [x] Perbaiki filter `?date=` yang 500 di PostgreSQL (lolos di H2): parameter tanggal butuh `cast`
- [ ] Tes alur tulis di PostgreSQL (register, booking, batal, bentrok jadwal): baru dites di H2
- [ ] Tes PostgreSQL otomatis (Testcontainers di CI) supaya beda H2 vs PostgreSQL ketahuan sebelum deploy
- [ ] Buat admin pertama: `update bioskop.users set role = 'ADMIN' where username = '<username>';`
- [x] Commit semua perubahan backend (refactor, Flyway, JWT, tiket, jadwal, pagination)
- [x] Update `tiket-bioskop-diagram.png`: tambah tabel `tickets`, kolom `users.role`, `schedules.studio_name` (sumber: `tiket-bioskop-diagram.html`)

## Fitur
- [x] Batalkan tiket: `DELETE /tickets/{id}`, hanya pemilik tiket (atau admin), paling lambat 2 jam sebelum tayang
- [x] Booking beberapa kursi dalam satu request
- [x] Update / hapus schedule (admin)
- [x] Tolak booking untuk jadwal yang sudah mulai / lewat
- [x] Filter schedule: `GET /schedules?filmId=&date=`
- [x] Cegah dua schedule bentrok di studio yang sama pada jam yang sama
- [x] Dukung pertunjukan lewat tengah malam (jam selesai < jam mulai = selesai besoknya, durasi maks 6 jam)

## Reservasi 5 menit + pembayaran
Alur baru: pilih kursi → kursi di-reserve 5 menit → halaman booking (ringkasan + bayar) → tiket aktif.
Tidak dibayar dalam 5 menit = reservasi batal dan kursi kosong lagi.

Keputusan dulu
- [x] Cara bayar: simulasi saja (tombol "Bayar" langsung menandai booking PAID), tanpa payment gateway

Backend
- [x] Migration `V2`: tabel `bookings` (`booking_id`, `user_id`, `schedule_id`, `status` RESERVED/PAID, `created_at`, `expires_at`, `paid_at`) + kolom `tickets.booking_id`; tiket lama dijadikan booking PAID
- [x] `POST /bookings` (menggantikan `POST /tickets`): buat booking RESERVED + tiket per kursi, `expires_at` = sekarang + 5 menit (paling lambat jam mulai film)
- [x] Kursi dari reservasi yang lewat `expires_at` dianggap kosong di `GET /schedules/{id}/seats` dan saat booking, tanpa menunggu job pembersih
- [x] Constraint `unique (schedule_id, seats_code)` tetap jadi penjaga race: hapus tiket reservasi kedaluwarsa untuk kursi yang diminta di transaksi yang sama, sebelum insert
- [x] `GET /bookings/{id}`: detail untuk halaman booking, sertakan `expiresAt` dan waktu server supaya hitung mundur tidak bergantung jam browser
- [x] `POST /bookings/{id}/pay`: hanya pemilik, ditolak 409 "Waktu pembayaran habis" kalau lewat 5 menit, idempoten (sudah PAID → 200 dengan data yang sama)
- [x] `DELETE /bookings/{id}`: batalkan reservasi sebelum bayar, kursi langsung dilepas (aturan 2 jam sebelum tayang tetap berlaku untuk yang sudah PAID)
- [x] `GET /tickets/me` hanya tiket PAID; reservasi yang masih jalan tampil terpisah ("menunggu pembayaran") lewat `GET /bookings/me`
- [x] Job `@Scheduled` tiap menit menghapus reservasi kedaluwarsa (hanya merapikan data; kebenaran tidak bergantung padanya)
- [x] Inject `Clock` (sekarang `LocalDateTime.now(zone)`) supaya test bisa memajukan waktu 5 menit
- [x] Test: reservasi menahan kursi, kedaluwarsa melepas kursi, bayar setelah 5 menit ditolak, bayar dua kali aman, dua user merebut kursi yang sama
- [x] Update README (endpoint) dan ERD (`tiket-bioskop-diagram.html` → tabel `bookings`)

Frontend
- [x] Peta kursi: tombol "Pesan" menjadi "Lanjut ke pembayaran", buat reservasi lalu buka halaman booking
- [x] Halaman `/bookings/{id}`: ringkasan (film, jadwal, studio, kursi, total), hitung mundur dari `expiresAt`, tombol "Bayar Rp X" dan "Batalkan"
- [x] Waktu habis: tombol bayar nonaktif, pesan "Waktu pembayaran habis, kursi dilepas" + link kembali ke peta kursi dengan `?seats=` yang sama
- [x] Halaman booking tetap jalan setelah refresh / dibuka ulang (data dari server); "Tiket saya" menampilkan reservasi aktif dengan link "Lanjutkan pembayaran"

## Teknis
- [x] Ganti HTTP Basic ke JWT: satu token, berlaku 24 jam, tanpa refresh token (lihat komentar `ponytail:` di `AuthUseCase`)
- [x] Pesan 409 yang lebih jelas saat hapus film/user yang masih dipakai schedule/tiket
- [x] Pagination untuk `GET /films`, `/films/search`, `/schedules`, `/users`
- [x] CI: GitHub Actions menjalankan `mvn test` di setiap push / PR
- [ ] Endpoint cari user berdasarkan email (dihapus saat refactor, tambahkan lagi kalau dibutuhkan)

## Frontend (folder `frontend/` di repo ini)

Catatan API: endpoint daftar (`/films`, `/films/search`, `/schedules`, `/users`) mengembalikan
`{"content": [...], "page": {...}}`, endpoint lain berupa objek / array biasa.
Saat development panggil backend lewat `/api/...` (mis. `/api/films`); prefix `/api` dibuang oleh proxy Vite.
Error body berisi `message` (dan `errors` per field untuk 400) yang bisa langsung ditampilkan.

### Setup
- [x] Buat proyek Vite + React + TypeScript di `frontend/`
- [x] Pastikan `frontend/.gitignore` dari template Vite ikut ter-commit (`node_modules`, `dist`, `*.local`)
- [x] Proxy dev server `/api/*` → `http://localhost:8080/*` (tidak perlu CORS saat development)

### Halaman publik
- [x] Daftar film + filter "sedang tayang" (`GET /films?showing=true`) + pencarian (`GET /films/search?name=`) + pagination
- [x] Detail film + daftar jadwalnya (`GET /schedules?filmId=`, bisa difilter `&date=`)
- [x] Register (`POST /users`) dengan pesan error validasi 400 dan duplikat 409
- [x] Login (`POST /auth/login`), simpan token, kirim `Authorization: Bearer <token>`, arahkan ke login saat 401 (token habis setelah 24 jam)

### Halaman user (login)
- [x] Pilih kursi: grid baris A–E × nomor 1–10 dari `GET /schedules/{id}/seats`, kursi terisi dinonaktifkan, pilih maks 10
- [x] Pesan tiket (`POST /tickets` dengan `{"scheduleId", "seatsCodes": [...]}`), tangani 409 kalau kursi keburu dipesan atau jadwal sudah mulai
- [x] Tiket saya (`GET /tickets/me`) + tombol batalkan (`DELETE /tickets/{id}`), nonaktif kalau kurang dari 2 jam sebelum tayang
- [x] Edit profil (`PUT /users/me`), login ulang kalau username berubah

### Halaman admin
- [x] Kelola film: tambah / edit / hapus (hapus ditolak 409 kalau masih punya jadwal → tawarkan set "tidak tayang")
- [x] Kelola jadwal: tambah / edit / hapus (pilih film, studio, tanggal, jam, harga), tampilkan pesan 409 kalau bentrok di studio yang sama
- [x] Daftar user (pagination) & hapus user

### UX (dari `/impeccable critique`, skor awal 23/40)
- [x] P1: kursi yang dipilih tetap ada setelah login/daftar (`?seats=` di URL), konflik hanya melepas kursi yang terambil
- [x] P1: semua pesan error/validasi backend dalam bahasa Indonesia, tanpa ID internal
- [x] P1: checkout merangkum pesanan, tombol menyebut total, "Tiket saya" menandai pesanan baru + total, jadwal mendatang di atas
- [x] P2: kontras mode gelap gagal AA (tombol aksen 3,7:1 → 5,5:1, badge/tab aktif 4,2:1 → 5,7:1, kursi terisi diberi tanda ×)
- [x] P2: border input mode gelap 1,38:1 → 3,3:1 (mode terang 1,3:1 → 3,6:1), token `--control-border`
- [x] P2: peta kursi untuk mobile & keyboard: kursi ~29–34px di ponsel, satu tab stop + tombol panah, bar checkout sticky
- [x] P2: halaman browse generik: kartu film menampilkan jadwal terdekat, detail film pakai strip tanggal + pilihan jam (`GET /schedules?from=`)
- [x] P1 (critique ke-2, 28/40): momen tiket: batas pembatalan di checkout, nomor tiket, "Mulai dalam N menit", notifikasi setelah batal, "Batalkan semua" per jadwal

### Bergantung ke backend
- [x] Auth: JWT dulu sebelum login di frontend dibuat, supaya password tidak disimpan di browser (lihat bagian Teknis)
- [x] CORS di backend untuk domain frontend production (env `CORS_ALLOWED_ORIGINS`)
- [ ] Deploy frontend + backend ke satu project Vercel (Vercel Services, gratis di Hobby)
    - [x] Siapkan repo: `vercel.json` (services), `Dockerfile` backend (port 80, prefix `/api`), build image di CI, langkah deploy di README
    - [ ] Pastikan CI hijau di GitHub (termasuk job `docker`, Dockerfile belum pernah di-build)
    - [x] Import repo di Vercel, isi env `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, samakan region dengan Supabase (`sin1`)
    - [x] Deploy, cek `/api/films` dan halaman depan: https://tiket-bioskop-six.vercel.app
    - [x] Ukur cold start: ~19 detik di `sin1` (batas start container Vercel ~28,6 detik; di `iad1` 29 detik → gagal)
    - [ ] Percepat start (flag JVM, matikan validasi Hibernate di production, AppCDS) supaya cold start lebih singkat dan ada margin
- [x] CI: tambah build frontend ke workflow GitHub Actions
