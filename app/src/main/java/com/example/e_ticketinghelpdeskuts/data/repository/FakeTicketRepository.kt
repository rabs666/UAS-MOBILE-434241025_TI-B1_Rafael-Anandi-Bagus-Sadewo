package com.example.e_ticketinghelpdeskuts.data.repository

import com.example.e_ticketinghelpdeskuts.domain.model.AppNotification
import com.example.e_ticketinghelpdeskuts.domain.model.AttachmentSource
import com.example.e_ticketinghelpdeskuts.domain.model.Comment
import com.example.e_ticketinghelpdeskuts.domain.model.Ticket
import com.example.e_ticketinghelpdeskuts.domain.model.TicketActivity
import com.example.e_ticketinghelpdeskuts.domain.model.TicketStatus
import com.example.e_ticketinghelpdeskuts.domain.repository.TicketRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class FakeTicketRepository : TicketRepository {
    private val ticketsFlow = MutableStateFlow(seedTickets())
    private val notificationsFlow = MutableStateFlow(seedNotifications())

    override fun getTickets(): Flow<List<Ticket>> = ticketsFlow

    override fun getTicketById(id: String): Flow<Ticket?> = ticketsFlow.map { list ->
        list.find { it.id == id }
    }

    override fun getNotifications(): Flow<List<AppNotification>> = notificationsFlow

    override suspend fun createTicket(ticket: Ticket) {
        val currentList = ticketsFlow.value.toMutableList()
        currentList.add(0, ticket)
        ticketsFlow.emit(currentList)

        pushNotification(
            title = "Tiket Baru",
            message = "${ticket.id} dibuat oleh ${ticket.applicant}",
            ticketId = ticket.id
        )
    }

    override suspend fun updateTicketStatus(id: String, status: TicketStatus, actor: String) {
        var updatedTicket: Ticket? = null

        val currentList = ticketsFlow.value.map { ticket ->
            if (ticket.id == id) {
                val latest = ticket.copy(
                    status = status,
                    activities = ticket.activities + TicketActivity(
                        id = UUID.randomUUID().toString(),
                        title = "Status diubah menjadi ${status.name}",
                        actor = actor,
                        timestamp = now()
                    )
                )
                updatedTicket = latest
                latest
            } else {
                ticket
            }
        }

        ticketsFlow.emit(currentList)

        updatedTicket?.let {
            pushNotification(
                title = "Status Tiket Diperbarui",
                message = "${it.id} sekarang berstatus ${it.status.name}",
                ticketId = it.id
            )
        }
    }

    override suspend fun assignTicket(id: String, assignee: String, actor: String) {
        var updatedTicket: Ticket? = null

        val currentList = ticketsFlow.value.map { ticket ->
            if (ticket.id == id) {
                val timestamp = now()
                val latest = ticket.copy(
                    assignedTo = assignee,
                    status = TicketStatus.IN_PROGRESS,
                    activities = ticket.activities + listOf(
                        TicketActivity(
                            id = UUID.randomUUID().toString(),
                            title = "Tiket di-assign ke $assignee",
                            actor = actor,
                            timestamp = timestamp
                        ),
                        TicketActivity(
                            id = UUID.randomUUID().toString(),
                            title = "Status otomatis berubah menjadi IN_PROGRESS",
                            actor = "System",
                            timestamp = timestamp
                        )
                    )
                )
                updatedTicket = latest
                latest
            } else {
                ticket
            }
        }

        ticketsFlow.emit(currentList)

        updatedTicket?.let {
            pushNotification(
                title = "Penugasan Tiket",
                message = "${it.id} ditugaskan ke $assignee — status: IN_PROGRESS",
                ticketId = it.id
            )
        }
    }

    override suspend fun finishTicket(id: String, actor: String) {
        var updatedTicket: Ticket? = null

        val currentList = ticketsFlow.value.map { ticket ->
            if (ticket.id == id) {
                val latest = ticket.copy(
                    status = TicketStatus.CLOSED,
                    activities = ticket.activities + TicketActivity(
                        id = UUID.randomUUID().toString(),
                        title = "Tiket diselesaikan — status CLOSED",
                        actor = actor,
                        timestamp = now()
                    )
                )
                updatedTicket = latest
                latest
            } else {
                ticket
            }
        }

        ticketsFlow.emit(currentList)

        updatedTicket?.let {
            pushNotification(
                title = "Tiket Selesai ✓",
                message = "${it.id} telah diselesaikan oleh $actor",
                ticketId = it.id
            )
        }
    }

    override suspend fun addComment(ticketId: String, comment: Comment) {
        var updatedTicket: Ticket? = null

        val currentList = ticketsFlow.value.map { ticket ->
            if (ticket.id == ticketId) {
                val latest = ticket.copy(
                    comments = ticket.comments + comment,
                    activities = ticket.activities + TicketActivity(
                        id = UUID.randomUUID().toString(),
                        title = "Komentar baru dari ${comment.sender}",
                        actor = comment.sender,
                        timestamp = comment.timestamp
                    )
                )
                updatedTicket = latest
                latest
            } else {
                ticket
            }
        }

        ticketsFlow.emit(currentList)

        updatedTicket?.let {
            pushNotification(
                title = "Komentar Baru",
                message = "${comment.sender} menambahkan komentar pada ${it.id}",
                ticketId = it.id
            )
        }
    }

    override suspend fun markNotificationAsRead(notificationId: String) {
        val updated = notificationsFlow.value.map { notif ->
            if (notif.id == notificationId) notif.copy(isRead = true) else notif
        }
        notificationsFlow.emit(updated)
    }

    override suspend fun markAllNotificationsAsRead() {
        val updated = notificationsFlow.value.map { it.copy(isRead = true) }
        notificationsFlow.emit(updated)
    }

    private suspend fun pushNotification(title: String, message: String, ticketId: String?) {
        val newNotification = AppNotification(
            id = UUID.randomUUID().toString(),
            title = title,
            message = message,
            ticketId = ticketId,
            timestamp = now(),
            isRead = false
        )

        notificationsFlow.emit(listOf(newNotification) + notificationsFlow.value)
    }

    private fun now(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
    }

    private fun seedTickets(): List<Ticket> {
        return listOf(
            Ticket(
                id = "T-001",
                title = "Koneksi Internet Putus",
                description = "Internet di lantai 2 mati total.",
                status = TicketStatus.OPEN,
                createdAt = "2026-04-08 09:00",
                applicantId = "U-001",
                applicant = "Ahmad Dani",
                attachmentSource = AttachmentSource.FILE,
                attachmentName = "log-internet.png",
                comments = listOf(
                    Comment("C-001", "Ahmad Dani", "Internet mati sejak pagi.", "2026-04-08 09:02")
                ),
                activities = listOf(
                    TicketActivity("A-001", "Tiket dibuat", "Ahmad Dani", "2026-04-08 09:00")
                )
            ),
            Ticket(
                id = "T-002",
                title = "Layar Monitor Berkedip",
                description = "Monitor sering mati sendiri saat dipakai.",
                status = TicketStatus.IN_PROGRESS,
                createdAt = "2026-04-07 14:20",
                applicantId = "U-002",
                applicant = "Siti Aminah",
                assignedTo = "Rina Helpdesk",
                comments = listOf(
                    Comment("C-002", "Rina Helpdesk", "Sudah saya jadwalkan pengecekan onsite.", "2026-04-07 15:00")
                ),
                activities = listOf(
                    TicketActivity("A-002", "Tiket dibuat", "Siti Aminah", "2026-04-07 14:20"),
                    TicketActivity("A-003", "Tiket di-assign ke Rina Helpdesk", "Admin UTS", "2026-04-07 14:45"),
                    TicketActivity("A-004", "Status otomatis berubah menjadi IN_PROGRESS", "System", "2026-04-07 14:45")
                )
            ),
            Ticket(
                id = "T-003",
                title = "Install Software Design",
                description = "Butuh Adobe Suite untuk keperluan desain.",
                status = TicketStatus.CLOSED,
                createdAt = "2026-04-06 10:00",
                applicantId = "U-003",
                applicant = "Budi Utomo",
                assignedTo = "Arif Helpdesk",
                activities = listOf(
                    TicketActivity("A-005", "Tiket dibuat", "Budi Utomo", "2026-04-06 10:00"),
                    TicketActivity("A-006", "Status diubah menjadi IN_PROGRESS", "Arif Helpdesk", "2026-04-06 10:30"),
                    TicketActivity("A-007", "Status diubah menjadi CLOSED", "Arif Helpdesk", "2026-04-06 11:10")
                )
            )
        )
    }

    private fun seedNotifications(): List<AppNotification> {
        return listOf(
            // ── Notifikasi untuk Ahmad Dani (U-001 / T-001) ──────────────────
            AppNotification(
                id = "N-001",
                title = "Tiket Kamu Sedang Diproses",
                message = "Tiket \"Koneksi Internet Putus\" (T-001) sedang ditangani oleh tim helpdesk.",
                ticketId = "T-001",
                timestamp = "2026-04-08 10:00",
                isRead = false
            ),
            AppNotification(
                id = "N-002",
                title = "Petugas Sudah Ditugaskan",
                message = "Tiket T-001 kamu telah di-assign ke Rina Helpdesk. Tunggu tindak lanjut.",
                ticketId = "T-001",
                timestamp = "2026-04-08 10:15",
                isRead = false
            ),

            // ── Notifikasi untuk Siti Aminah (U-002 / T-002) ─────────────────
            AppNotification(
                id = "N-003",
                title = "Tiket Kamu Sedang Dikerjakan",
                message = "Tiket \"Layar Monitor Berkedip\" (T-002) kini berstatus IN_PROGRESS. Rina Helpdesk sedang menangani.",
                ticketId = "T-002",
                timestamp = "2026-04-07 14:50",
                isRead = false
            ),

            // ── Notifikasi untuk Budi Utomo (U-003 / T-003) ──────────────────
            AppNotification(
                id = "N-004",
                title = "Tiket Kamu Sudah Selesai ✓",
                message = "Tiket \"Install Software Design\" (T-003) telah CLOSED. Masalah telah diselesaikan.",
                ticketId = "T-003",
                timestamp = "2026-04-06 11:11",
                isRead = true
            ),

            // ── Notifikasi global (admin/helpdesk saja yang relevan) ──────────
            AppNotification(
                id = "N-005",
                title = "Tiket Baru Masuk",
                message = "Ahmad Dani membuat tiket baru: \"Koneksi Internet Putus\" (T-001). Segera ditindaklanjuti.",
                ticketId = "T-001",
                timestamp = "2026-04-08 09:00",
                isRead = true
            )
        )
    }
}
