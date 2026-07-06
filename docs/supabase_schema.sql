-- ============================================================
--  E-Ticketing Helpdesk UTS - Skema Supabase (PostgreSQL)
--
--  Cara pakai:
--  1. Buka dashboard proyek Supabase Anda.
--  2. Masuk ke menu "SQL Editor".
--  3. Salin seluruh isi file ini (Copy) dan Tempel (Paste) di sana.
--  4. Tekan "Run" untuk mengeksekusi semua query.
-- ============================================================

-- Hapus tabel jika sudah ada (sesuai urutan dependensi foreign key)
DROP TABLE IF EXISTS notifications CASCADE;
DROP TABLE IF EXISTS ticket_activities CASCADE;
DROP TABLE IF EXISTS comments CASCADE;
DROP TABLE IF EXISTS tickets CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- ----------------------------------------------------------------
-- Tabel: users
-- ----------------------------------------------------------------
CREATE TABLE users (
    id          VARCHAR(20)  PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    email       VARCHAR(100) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,                 
    role        VARCHAR(20)  NOT NULL DEFAULT 'USER' CHECK (role IN ('USER', 'HELPDESK', 'ADMIN')),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ----------------------------------------------------------------
-- Tabel: tickets
-- ----------------------------------------------------------------
CREATE TABLE tickets (
    id                VARCHAR(20)   PRIMARY KEY,
    title             VARCHAR(100)  NOT NULL,
    description       TEXT          NOT NULL,
    status            VARCHAR(20)   NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'ASSIGNED', 'IN_PROGRESS', 'CLOSED')),
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    applicant_id      VARCHAR(20)   NOT NULL REFERENCES users(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    assigned_to       VARCHAR(20)   REFERENCES users(id) ON UPDATE CASCADE ON DELETE SET NULL,
    attachment_source VARCHAR(20)   NOT NULL DEFAULT 'NONE' CHECK (attachment_source IN ('NONE', 'CAMERA', 'FILE')),
    attachment_name   VARCHAR(255)
);

-- ----------------------------------------------------------------
-- Tabel: comments
-- ----------------------------------------------------------------
CREATE TABLE comments (
    id         VARCHAR(20)  PRIMARY KEY,
    ticket_id  VARCHAR(20)  NOT NULL REFERENCES tickets(id) ON UPDATE CASCADE ON DELETE CASCADE,
    sender     VARCHAR(100) NOT NULL,
    message    TEXT         NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ----------------------------------------------------------------
-- Tabel: ticket_activities (audit trail)
-- ----------------------------------------------------------------
CREATE TABLE ticket_activities (
    id         VARCHAR(20)  PRIMARY KEY,
    ticket_id  VARCHAR(20)  NOT NULL REFERENCES tickets(id) ON UPDATE CASCADE ON DELETE CASCADE,
    title      VARCHAR(255) NOT NULL,
    actor      VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ----------------------------------------------------------------
-- Tabel: notifications
-- ----------------------------------------------------------------
CREATE TABLE notifications (
    id         VARCHAR(20)  PRIMARY KEY,
    title      VARCHAR(150) NOT NULL,
    message    TEXT         NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    ticket_id  VARCHAR(20)  REFERENCES tickets(id) ON UPDATE CASCADE ON DELETE CASCADE,
    is_read    BOOLEAN      NOT NULL DEFAULT FALSE
);

-- ============================================================
--  SEED DATA (identik dengan data dummy aplikasi)
-- ============================================================

-- Users (password '123456' — di produksi gunakan hash)
INSERT INTO users (id, name, username, email, password, role) VALUES
('U-001','Ahmad Dani','ahmad','ahmad@campus.ac.id','123456','USER'),
('U-002','Siti Aminah','siti','siti@campus.ac.id','123456','USER'),
('U-003','Budi Utomo','budi','budi@campus.ac.id','123456','USER'),
('H-001','Rina Helpdesk','helpdesk','helpdesk@campus.ac.id','123456','HELPDESK'),
('H-002','Arif Helpdesk','arif','arif@campus.ac.id','123456','HELPDESK'),
('A-001','Admin UTS','admin','admin@campus.ac.id','123456','ADMIN');

-- Tickets
INSERT INTO tickets (id,title,description,status,created_at,applicant_id,assigned_to,attachment_source,attachment_name) VALUES
('T-001','Koneksi Internet Putus','Internet di lantai 2 mati total.','OPEN','2026-04-08 09:00','U-001',NULL,'FILE','log-internet.png'),
('T-002','Layar Monitor Berkedip','Monitor sering mati sendiri saat dipakai.','IN_PROGRESS','2026-04-07 14:20','U-002','H-001','NONE',NULL),
('T-003','Install Software Design','Butuh Adobe Suite untuk keperluan desain.','CLOSED','2026-04-06 10:00','U-003','H-002','NONE',NULL);

-- Comments
INSERT INTO comments (id,ticket_id,sender,message,created_at) VALUES
('C-001','T-001','Ahmad Dani','Internet mati sejak pagi.','2026-04-08 09:02'),
('C-002','T-002','Rina Helpdesk','Sudah saya jadwalkan pengecekan onsite.','2026-04-07 15:00');

-- Ticket Activities
INSERT INTO ticket_activities (id,ticket_id,title,actor,created_at) VALUES
('A-001','T-001','Tiket dibuat','Ahmad Dani','2026-04-08 09:00'),
('A-002','T-002','Tiket dibuat','Siti Aminah','2026-04-07 14:20'),
('A-003','T-002','Status diubah menjadi IN_PROGRESS','Rina Helpdesk','2026-04-07 14:45'),
('A-004','T-002','Tiket di-assign ke Rina Helpdesk','Admin UTS','2026-04-07 14:46'),
('A-005','T-003','Tiket dibuat','Budi Utomo','2026-04-06 10:00'),
('A-006','T-003','Status diubah menjadi IN_PROGRESS','Arif Helpdesk','2026-04-06 10:30'),
('A-007','T-003','Status diubah menjadi CLOSED','Arif Helpdesk','2026-04-06 11:10');

-- Notifications
INSERT INTO notifications (id,title,message,created_at,ticket_id,is_read) VALUES
('N-001','Update Status','T-002 sedang ditangani helpdesk','2026-04-07 14:50','T-002',FALSE),
('N-002','Tiket Selesai','T-003 sudah diselesaikan','2026-04-06 11:11','T-003',TRUE);
