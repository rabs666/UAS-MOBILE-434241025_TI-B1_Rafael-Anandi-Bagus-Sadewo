# -*- coding: utf-8 -*-
"""Render database tables (phpMyAdmin/Workbench-style grids) as PNGs from the
real seed data in docs/schema.sql, for embedding into the SRS docx."""
import os
from PIL import Image, ImageDraw, ImageFont

OUT = os.path.join(os.path.dirname(__file__), "..", "docs", "img")
os.makedirs(OUT, exist_ok=True)

# Palette (match brand + a phpMyAdmin-ish neutral grid)
PRIMARY = (0, 100, 147)        # header band #006493
HEADER_TXT = (255, 255, 255)
GRID = (208, 216, 224)
ROW_A = (255, 255, 255)
ROW_B = (244, 248, 251)
INK = (33, 41, 49)
CAPTION = (90, 105, 120)
NULLC = (150, 158, 166)
SCALE = 2
PAD = 14            # cell horizontal padding
ROW_H = 30         # logical row height
HEAD_H = 34


def font(size, bold=False):
    names = (["arialbd.ttf", "segoeuib.ttf"] if bold else ["arial.ttf", "segoeui.ttf"])
    for n in names:
        for base in ("C:/Windows/Fonts/", "/usr/share/fonts/truetype/dejavu/"):
            try:
                return ImageFont.truetype(base + n, size)
            except Exception:
                pass
    try:
        return ImageFont.truetype("DejaVuSans.ttf", size)
    except Exception:
        return ImageFont.load_default()


F = font(15 * SCALE)
FB = font(15 * SCALE, bold=True)
FCAP = font(14 * SCALE, bold=True)
FMONO = font(14 * SCALE)


def text_w(d, s, fnt):
    bb = d.textbbox((0, 0), s, font=fnt)
    return bb[2] - bb[0]


def render_table(name, columns, rows, fname, note=None):
    # Measure column widths with a probe image
    probe = Image.new("RGB", (10, 10))
    pd = ImageDraw.Draw(probe)
    col_w = []
    for ci, col in enumerate(columns):
        w = text_w(pd, col, FB) / SCALE
        for r in rows:
            cell = "NULL" if r[ci] is None else str(r[ci])
            w = max(w, text_w(pd, cell, F) / SCALE)
        col_w.append(int(w) + PAD * 2)

    title = f"Tabel: {name}   ({len(rows)} baris)"
    cap_h = 30
    note_h = 24 if note else 0
    total_w = sum(col_w)
    total_h = cap_h + HEAD_H + ROW_H * len(rows) + note_h

    img = Image.new("RGB", ((total_w + 2) * SCALE, (total_h + 2) * SCALE), (255, 255, 255))
    d = ImageDraw.Draw(img)

    def S(v):
        return int(v * SCALE)

    # Caption
    d.text((S(2), S(4)), title, font=FCAP, fill=PRIMARY)

    y = cap_h
    # Header band
    d.rectangle([S(0), S(y), S(total_w), S(y + HEAD_H)], fill=PRIMARY)
    x = 0
    for ci, col in enumerate(columns):
        d.text((S(x + PAD), S(y + (HEAD_H - 16) / 2)), col, font=FB, fill=HEADER_TXT)
        x += col_w[ci]
    y += HEAD_H

    # Rows (zebra)
    for ri, r in enumerate(rows):
        fill = ROW_A if ri % 2 == 0 else ROW_B
        d.rectangle([S(0), S(y), S(total_w), S(y + ROW_H)], fill=fill)
        x = 0
        for ci in range(len(columns)):
            val = r[ci]
            cell = "NULL" if val is None else str(val)
            color = NULLC if val is None else INK
            d.text((S(x + PAD), S(y + (ROW_H - 15) / 2)), cell, font=F, fill=color)
            x += col_w[ci]
        y += ROW_H

    # Grid lines
    d.rectangle([S(0), S(cap_h), S(total_w), S(cap_h + HEAD_H + ROW_H * len(rows))],
                outline=GRID, width=S(1))
    yy = cap_h + HEAD_H
    for _ in range(len(rows) + 1):
        d.line([S(0), S(yy), S(total_w), S(yy)], fill=GRID, width=S(1))
        yy += ROW_H if _ > 0 else 0
    # vertical lines
    xx = 0
    for ci in range(len(columns)):
        d.line([S(xx), S(cap_h), S(xx), S(cap_h + HEAD_H + ROW_H * len(rows))], fill=GRID, width=S(1))
        xx += col_w[ci]
    d.line([S(total_w), S(cap_h), S(total_w), S(cap_h + HEAD_H + ROW_H * len(rows))], fill=GRID, width=S(1))

    if note:
        d.text((S(2), S(total_h - note_h + 4)), note, font=FMONO, fill=CAPTION)

    img = img.resize((img.width // SCALE, img.height // SCALE), Image.LANCZOS)
    p = os.path.join(OUT, fname)
    img.save(p)
    print("wrote", os.path.normpath(p))


# ---- Seed data identical to docs/schema.sql ----
users_cols = ["id", "name", "username", "email", "role"]
users = [
    ("U-001", "Ahmad Dani", "ahmad", "ahmad@campus.ac.id", "USER"),
    ("U-002", "Siti Aminah", "siti", "siti@campus.ac.id", "USER"),
    ("U-003", "Budi Utomo", "budi", "budi@campus.ac.id", "USER"),
    ("H-001", "Rina Helpdesk", "helpdesk", "helpdesk@campus.ac.id", "HELPDESK"),
    ("H-002", "Arif Helpdesk", "arif", "arif@campus.ac.id", "HELPDESK"),
    ("A-001", "Admin UTS", "admin", "admin@campus.ac.id", "ADMIN"),
]

tickets_cols = ["id", "title", "status", "created_at", "applicant_id", "assigned_to", "attachment_source", "attachment_name"]
tickets = [
    ("T-001", "Koneksi Internet Putus", "OPEN", "2026-04-08 09:00", "U-001", None, "FILE", "log-internet.png"),
    ("T-002", "Layar Monitor Berkedip", "IN_PROGRESS", "2026-04-07 14:20", "U-002", "H-001", "NONE", None),
    ("T-003", "Install Software Design", "CLOSED", "2026-04-06 10:00", "U-003", "H-002", "NONE", None),
]

comments_cols = ["id", "ticket_id", "sender", "message", "created_at"]
comments = [
    ("C-001", "T-001", "Ahmad Dani", "Internet mati sejak pagi.", "2026-04-08 09:02"),
    ("C-002", "T-002", "Rina Helpdesk", "Sudah saya jadwalkan pengecekan onsite.", "2026-04-07 15:00"),
]

acts_cols = ["id", "ticket_id", "title", "actor", "created_at"]
acts = [
    ("A-001", "T-001", "Tiket dibuat", "Ahmad Dani", "2026-04-08 09:00"),
    ("A-002", "T-002", "Tiket dibuat", "Siti Aminah", "2026-04-07 14:20"),
    ("A-003", "T-002", "Status diubah menjadi IN_PROGRESS", "Rina Helpdesk", "2026-04-07 14:45"),
    ("A-004", "T-002", "Tiket di-assign ke Rina Helpdesk", "Admin UTS", "2026-04-07 14:46"),
    ("A-005", "T-003", "Tiket dibuat", "Budi Utomo", "2026-04-06 10:00"),
    ("A-006", "T-003", "Status diubah menjadi IN_PROGRESS", "Arif Helpdesk", "2026-04-06 10:30"),
    ("A-007", "T-003", "Status diubah menjadi CLOSED", "Arif Helpdesk", "2026-04-06 11:10"),
]

notif_cols = ["id", "title", "message", "created_at", "ticket_id", "is_read"]
notifs = [
    ("N-001", "Update Status", "T-002 sedang ditangani helpdesk", "2026-04-07 14:50", "T-002", "0"),
    ("N-002", "Tiket Selesai", "T-003 sudah diselesaikan", "2026-04-06 11:11", "T-003", "1"),
]

NOTE = "Sumber: docs/schema.sql (seed identik dengan data dummy aplikasi)."
render_table("users", users_cols, users, "db_users.png", NOTE)
render_table("tickets", tickets_cols, tickets, "db_tickets.png", NOTE)
render_table("comments", comments_cols, comments, "db_comments.png", NOTE)
render_table("ticket_activities", acts_cols, acts, "db_activities.png", NOTE)
render_table("notifications", notif_cols, notifs, "db_notifications.png", NOTE)
print("done")
