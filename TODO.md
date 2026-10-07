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
- [ ] Update `tiket-bioskop-diagram.png`: tambah tabel `tickets`, kolom `users.role`, `schedules.studio_name`

## Fitur
- [x] Batalkan tiket: `DELETE /tickets/{id}`, hanya pemilik tiket (atau admin), paling lambat 2 jam sebelum tayang
- [x] Booking beberapa kursi dalam satu request
- [x] Update / hapus schedule (admin)
- [x] Tolak booking untuk jadwal yang sudah mulai / lewat
- [x] Filter schedule: `GET /schedules?filmId=&date=`
- [x] Cegah dua schedule bentrok di studio yang sama pada jam yang sama
- [x] Dukung pertunjukan lewat tengah malam (jam selesai < jam mulai = selesai besoknya, durasi maks 6 jam)

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
