
# E-Ticketing Helpdesk UTS

Aplikasi **e-ticketing helpdesk** berbasis **Native Android (Jetpack Compose + Material 3)**.
Pengguna dapat membuat tiket bantuan, petugas helpdesk/admin menindaklanjuti, mengubah
status, meng-assign, dan berdiskusi lewat komentar — lengkap dengan notifikasi in-app,
mode gelap, dan kontrol akses berbasis role.

> Proyek tugas **UAS Mobile** (Teori + Praktikum). Repository memuat **source code**,
> **APK rilis tersigner**, dan **dokumentasi lengkap** (SRS, flow diagram, UI/UX, API, database, naskah video).

---

## 1. Fitur Utama

- 🔐 **Autentikasi** — login, registrasi, reset password (dengan validasi).
- 🎫 **Manajemen tiket** — buat, lihat daftar, lihat detail, lampiran (kamera/file).
- 🔁 **Siklus status** — `OPEN → IN_PROGRESS → CLOSED` dengan audit trail.
- 👥 **Role-based access** — `USER`, `HELPDESK`, `ADMIN` dengan hak akses berbeda.
- 💬 **Komentar & diskusi** per tiket.
- 🔔 **Notifikasi in-app** dengan badge belum-dibaca.
- 🌙 **Mode gelap** dapat di-toggle dari Profil.
- 🎨 **Desain konsisten** — palet brand, komponen reusable, logo & splash kustom.

---

## 2. Tech Stack

| Komponen | Teknologi |
|----------|-----------|
| Bahasa | Kotlin 2.0.21 |
| UI | Jetpack Compose (BOM 2024.09.00) + Material 3 |
| Arsitektur | MVVM (UI → ViewModel → UseCase → Repository) |
| Navigasi | Navigation Compose 2.7.7 |
| State | Kotlin Coroutines + StateFlow |
| Build | Android Gradle Plugin 8.13.0 |
| Min / Target / Compile SDK | 24 / 35 / 35 |
| Data layer | `FakeTicketRepository` (in-memory dummy, siap diganti REST API) |

---

## 3. Struktur Proyek

```
app/src/main/java/com/example/e_ticketinghelpdeskuts/
├── MainActivity.kt              # Entry point + NavHost
├── data/repository/             # FakeTicketRepository (dummy data)
├── domain/
│   ├── model/                   # Ticket, AppUser, AppNotification, dll
│   ├── repository/              # Kontrak TicketRepository
│   └── usecase/                 # GetTickets, GetTicketDetail
├── ui/
│   ├── components/              # BrandLogo, Buttons, StatusChip, MessageBanner, ...
│   ├── navigation/              # Screen (sealed route)
│   ├── screens/                 # auth, dashboard, ticket, profile, notification, splash
│   └── theme/                   # Color, Type, Spacing, Theme (light/dark)
└── utils/                       # AssetLoader, DebugUtils, InputValidation
```

---

## 4. Cara Build & Menjalankan

**Prasyarat:** Android Studio (Ladybug+), JDK 17, Android SDK 35.

```bash
# Debug — jalankan di emulator/perangkat
./gradlew installDebug

# Release — APK tersigner (output di app/build/outputs/apk/release/)
./gradlew assembleRelease
```

APK rilis siap-pakai juga tersedia di repo:

```
apk file eticketing.apk
```

> Signing menggunakan `keys.jks` via `keystore.properties`. Build release mengaktifkan
> R8 (minify), shrink resources, dan ProGuard.

---

## 5. Akun Demo

Password semua akun: **`123456`**

| Username | Role | Keterangan |
|----------|------|------------|
| `ahmad` | USER | Pelapor (lihat & buat tiket sendiri) |
| `siti` | USER | Pelapor |
| `budi` | USER | Pelapor |
| `helpdesk` | HELPDESK | Tangani semua tiket |
| `arif` | HELPDESK | Tangani semua tiket |
| `admin` | ADMIN | Akses penuh |

---

## 6. Dokumentasi

| Dokumen | Isi |
|---------|-----|
| [docs/FLOW_AND_UIUX.md](docs/FLOW_AND_UIUX.md) | Flow diagram (navigasi, auth, lifecycle, role) + deskripsi UI/UX tiap layar |
| [API_DOCUMENTATION.md](API_DOCUMENTATION.md) | Endpoint REST API & skema data model |
| [docs/openapi.yaml](docs/openapi.yaml) | Spesifikasi OpenAPI (impor ke Swagger / Postman) |
| [docs/DATABASE_DOCUMENTATION.md](docs/DATABASE_DOCUMENTATION.md) | Skema relasional, DDL, ERD, seed data |

---

## 7. Catatan

Lapisan data saat ini memakai **`FakeTicketRepository`** (data dummy in-memory) sehingga
aplikasi dapat dijalankan tanpa backend. Skema database dan spesifikasi API pada folder
`docs/` adalah representasi backend setara yang siap diintegrasikan (mis. Retrofit + REST)
tanpa mengubah lapisan domain/UI.

---

**Application ID:** `com.example.e_ticketinghelpdeskuts` · **Versi:** 1.0 (versionCode 1)
