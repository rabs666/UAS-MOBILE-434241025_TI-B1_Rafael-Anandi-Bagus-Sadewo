package com.example.e_ticketinghelpdeskuts.data.repository

import com.example.e_ticketinghelpdeskuts.data.remote.dto.*
import com.example.e_ticketinghelpdeskuts.domain.model.*
import com.example.e_ticketinghelpdeskuts.domain.repository.TicketRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class SupabaseTicketRepository(
    private val supabase: SupabaseClient
) : TicketRepository {

    private val ticketsFlow = MutableStateFlow<List<Ticket>>(emptyList())
    private val notificationsFlow = MutableStateFlow<List<AppNotification>>(emptyList())
    // Di-seed sebagai fallback agar akun demo tetap bisa login walau Supabase belum termuat/offline.
    private val usersFlow = MutableStateFlow<List<AppUser>>(seedUsers())
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _isRefreshing = MutableStateFlow(false)
    override val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        refreshData()
    }

    /** Dipanggil dari UI (saat layar dibuka) untuk menarik data terbaru dari Supabase. */
    override fun refresh() {
        refreshData()
    }

    private fun refreshData() {
        scope.launch {
            _isRefreshing.value = true
            try {
                // Tiap section di-fetch dalam try/catch sendiri. Tujuannya: jika satu tabel
                // (mis. tickets) gagal decode, section lain — terutama NOTIFICATIONS — tetap
                // ter-update. Sebelumnya semuanya satu blok try, jadi 1 baris rusak = notifikasi
                // & dot ikut hilang total.

                // Fetch Users for resolving names
                var userMap: Map<String, UserDto> = emptyMap()
                try {
                    val users = supabase.postgrest["users"].select().decodeList<UserDto>()
                    userMap = users.associateBy { it.id }

                    // Simpan daftar user (untuk login & manajemen pengguna)
                    usersFlow.value = users.map { u ->
                        AppUser(
                            id = u.id,
                            name = u.name,
                            username = u.username,
                            email = u.email,
                            password = u.password,
                            role = try { UserRole.valueOf(u.role) } catch (e: Exception) { UserRole.USER }
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Fetch Tickets (+ Comments + Activities)
                try {
                    val ticketsDto = supabase.postgrest["tickets"].select().decodeList<TicketDto>()

                    val commentsDto = supabase.postgrest["comments"].select().decodeList<CommentDto>()
                    val commentsByTicket = commentsDto.groupBy { it.ticketId }

                    val activitiesDto = supabase.postgrest["ticket_activities"].select().decodeList<TicketActivityDto>()
                    val activitiesByTicket = activitiesDto.groupBy { it.ticketId }

                    val mappedTickets = ticketsDto.map { dto ->
                        val applicant = userMap[dto.applicantId]?.name ?: "Unknown"
                        val assignedToName = dto.assignedTo?.let { userMap[it]?.name }

                        Ticket(
                            id = dto.id,
                            title = dto.title,
                            description = dto.description,
                            status = try { TicketStatus.valueOf(dto.status) } catch(e: Exception) { TicketStatus.OPEN },
                            createdAt = dto.createdAt,
                            applicantId = dto.applicantId,
                            applicant = applicant,
                            assignedTo = assignedToName,
                            attachmentSource = try { AttachmentSource.valueOf(dto.attachmentSource) } catch(e: Exception) { AttachmentSource.NONE },
                            attachmentName = dto.attachmentName,
                            comments = commentsByTicket[dto.id]?.map { c ->
                                Comment(c.id, c.sender, c.message, c.createdAt)
                            } ?: emptyList(),
                            activities = activitiesByTicket[dto.id]?.map { a ->
                                TicketActivity(a.id, a.title, a.actor, a.createdAt)
                            } ?: emptyList()
                        )
                    }.sortedByDescending { it.createdAt }

                    ticketsFlow.value = mappedTickets
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Fetch Notifications — independen agar dot & daftar notifikasi tidak ikut
                // hilang saat tabel lain bermasalah.
                try {
                    val notifsDto = supabase.postgrest["notifications"].select().decodeList<NotificationDto>()
                    val mappedNotifs = notifsDto.map { n: NotificationDto ->
                        AppNotification(
                            id = n.id,
                            title = n.title,
                            message = n.message,
                            timestamp = n.createdAt,
                            ticketId = n.ticketId,
                            isRead = n.isRead
                        )
                    }
                    notificationsFlow.value = mappedNotifs.sortedByDescending { it.timestamp }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    override fun getTickets(): Flow<List<Ticket>> = ticketsFlow

    override fun getTicketById(id: String): Flow<Ticket?> = ticketsFlow.map { list ->
        list.find { it.id == id }
    }

    override fun getNotifications(): Flow<List<AppNotification>> = notificationsFlow

    override fun getUsers(): Flow<List<AppUser>> = usersFlow

    override suspend fun createUser(user: AppUser) {
        @Serializable
        data class UserInsert(
            val id: String,
            val name: String,
            val username: String,
            val email: String,
            val password: String,
            val role: String
        )
        // created_at diisi otomatis oleh DEFAULT NOW() di Supabase
        supabase.postgrest["users"].insert(
            UserInsert(user.id, user.name, user.username, user.email, user.password, user.role.name)
        )
        refreshData()
    }

    override suspend fun updateUserRole(id: String, role: UserRole) {
        @Serializable
        data class RoleUpdate(val role: String)
        supabase.postgrest["users"].update(RoleUpdate(role.name)) {
            filter { eq("id", id) }
        }
        refreshData()
    }

    override suspend fun deleteUser(id: String) {
        supabase.postgrest["users"].delete {
            filter { eq("id", id) }
        }
        refreshData()
    }

    override suspend fun createTicket(ticket: Ticket) {
        val dto = TicketDto(
            id = ticket.id,
            title = ticket.title,
            description = ticket.description,
            status = ticket.status.name,
            createdAt = ticket.createdAt,
            applicantId = ticket.applicantId,
            assignedTo = null,
            attachmentSource = ticket.attachmentSource.name,
            attachmentName = ticket.attachmentName
        )
        supabase.postgrest["tickets"].insert(dto)
        
        val activity = TicketActivityDto(
            id = newId("AC"),
            ticketId = ticket.id,
            title = "Tiket dibuat",
            actor = ticket.applicant,
            createdAt = ticket.createdAt
        )
        supabase.postgrest["ticket_activities"].insert(activity)

        pushNotification("Tiket Baru", "${ticket.id} dibuat oleh ${ticket.applicant}", ticket.id)
        refreshData()
    }

    override suspend fun assignTicket(id: String, assignee: String, actor: String) {
        val timestamp = now()
        
        // Coba mencari User ID berdasarkan nama assignee
        val users = supabase.postgrest["users"].select().decodeList<UserDto>()
        val assigneeUser = users.find { it.name == assignee }
        
        @Serializable
        data class AssignUpdate(@SerialName("assigned_to") val assignedTo: String?, val status: String)
        
        supabase.postgrest["tickets"].update(AssignUpdate(assigneeUser?.id, TicketStatus.IN_PROGRESS.name)) {
            filter { eq("id", id) }
        }

        val activity1 = TicketActivityDto(newId("AC"), id, "Tiket di-assign ke $assignee", actor, timestamp)
        val activity2 = TicketActivityDto(newId("AC"), id, "Status otomatis berubah menjadi In Progress", "System", timestamp)
        supabase.postgrest["ticket_activities"].insert(listOf(activity1, activity2))

        pushNotification("Penugasan Tiket", "$id ditugaskan ke $assignee — status: In Progress", id)
        refreshData()
    }

    override suspend fun acceptTicket(id: String, actor: String) {
        @Serializable
        data class StatusUpdate(val status: String)
        
        supabase.postgrest["tickets"].update(StatusUpdate(TicketStatus.ASSIGNED.name)) {
            filter { eq("id", id) }
        }

        val activity = TicketActivityDto(newId("AC"), id, "Tiket diterima oleh admin — status Assigned", actor, now())
        supabase.postgrest["ticket_activities"].insert(activity)

        pushNotification("Tiket Diterima", "$id telah diterima oleh $actor — status: Assigned", id)
        refreshData()
    }

    override suspend fun finishTicket(id: String, actor: String) {
        @Serializable
        data class StatusUpdate(val status: String)
        
        supabase.postgrest["tickets"].update(StatusUpdate(TicketStatus.CLOSED.name)) {
            filter { eq("id", id) }
        }

        val activity = TicketActivityDto(newId("AC"), id, "Tiket diselesaikan — status Closed", actor, now())
        supabase.postgrest["ticket_activities"].insert(activity)

        pushNotification("Tiket Selesai ✓", "$id telah diselesaikan oleh $actor", id)
        refreshData()
    }

    override suspend fun addComment(ticketId: String, comment: Comment) {
        val dto = CommentDto(
            id = comment.id,
            ticketId = ticketId,
            sender = comment.sender,
            message = comment.message,
            createdAt = comment.timestamp
        )
        supabase.postgrest["comments"].insert(dto)

        val activity = TicketActivityDto(newId("AC"), ticketId, "Komentar baru dari ${comment.sender}", comment.sender, comment.timestamp)
        supabase.postgrest["ticket_activities"].insert(activity)

        pushNotification("Komentar Baru", "${comment.sender} menambahkan komentar pada $ticketId", ticketId)
        refreshData()
    }

    override suspend fun markNotificationAsRead(notificationId: String) {
        @Serializable
        data class ReadUpdate(@SerialName("is_read") val isRead: Boolean)
        
        supabase.postgrest["notifications"].update(ReadUpdate(true)) {
            filter { eq("id", notificationId) }
        }
        refreshData()
    }

    override suspend fun markAllNotificationsAsRead() {
        @Serializable
        data class ReadUpdate(@SerialName("is_read") val isRead: Boolean)
        
        supabase.postgrest["notifications"].update(ReadUpdate(true)) {
            filter { eq("is_read", false) }
        }
        refreshData()
    }

    private suspend fun pushNotification(title: String, message: String, ticketId: String?) {
        val dto = NotificationDto(
            id = newId("NT"),
            title = title,
            message = message,
            createdAt = now(),
            ticketId = ticketId,
            isRead = false
        )
        supabase.postgrest["notifications"].insert(dto)
    }

    private fun now(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    }

    /**
     * ID unik ringkas (<= 20 karakter) agar MUAT di kolom VARCHAR(20) Supabase.
     * UUID penuh berukuran 36 karakter → ditolak Postgres ("value too long"),
     * itulah yang dulu membuat insert notifikasi/aktivitas/komentar gagal diam-diam
     * sehingga notifikasi & dot tidak pernah muncul. Format: "PP-<14 hex>" = 17 char.
     */
    private fun newId(prefix: String): String {
        val rand = UUID.randomUUID().toString().replace("-", "").take(14)
        return "$prefix-$rand"
    }

    // Fallback akun demo (identik dengan seed di tabel users Supabase) agar
    // login tetap berfungsi saat data Supabase belum termuat / offline.
    private fun seedUsers(): List<AppUser> = listOf(
        AppUser("U-001", "Ahmad Dani", "ahmad", "ahmad@campus.ac.id", "123456", UserRole.USER),
        AppUser("U-002", "Siti Aminah", "siti", "siti@campus.ac.id", "123456", UserRole.USER),
        AppUser("U-003", "Budi Utomo", "budi", "budi@campus.ac.id", "123456", UserRole.USER),
        AppUser("H-001", "Rina Helpdesk", "helpdesk", "helpdesk@campus.ac.id", "123456", UserRole.HELPDESK),
        AppUser("H-002", "Arif Helpdesk", "arif", "arif@campus.ac.id", "123456", UserRole.HELPDESK),
        AppUser("A-001", "Admin UTS", "admin", "admin@campus.ac.id", "123456", UserRole.ADMIN)
    )
}
