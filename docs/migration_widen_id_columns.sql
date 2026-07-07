-- ============================================================
--  MIGRASI: Lebarkan kolom id agar muat UUID (36 karakter)
-- ============================================================
--  MASALAH:
--  Kolom id di notifications / comments / ticket_activities semula VARCHAR(20),
--  sedangkan aplikasi membuat id memakai UUID (36 karakter). Akibatnya Postgres
--  menolak INSERT ("value too long for type character varying(20)"), sehingga
--  notifikasi/komentar/aktivitas GAGAL tersimpan diam-diam → notifikasi & dot
--  merah tidak pernah muncul saat status tiket diperbarui.
--
--  SOLUSI:
--  Lebarkan kolom id (dan kolom ticket_id yang mereferensikannya bila perlu)
--  menjadi VARCHAR(64) supaya UUID penuh pun aman.
--
--  CARA PAKAI:
--  Buka Supabase → SQL Editor → tempel skrip ini → Run. Aman dijalankan berulang.
-- ============================================================

ALTER TABLE notifications      ALTER COLUMN id TYPE VARCHAR(64);
ALTER TABLE comments           ALTER COLUMN id TYPE VARCHAR(64);
ALTER TABLE ticket_activities  ALTER COLUMN id TYPE VARCHAR(64);

-- Verifikasi hasil:
-- SELECT table_name, column_name, character_maximum_length
-- FROM information_schema.columns
-- WHERE column_name = 'id'
--   AND table_name IN ('notifications','comments','ticket_activities');
