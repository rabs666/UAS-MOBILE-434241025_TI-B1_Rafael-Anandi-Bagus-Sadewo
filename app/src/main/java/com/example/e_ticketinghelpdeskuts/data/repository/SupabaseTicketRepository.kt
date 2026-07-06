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
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        refreshData()
    }

    private fun refreshData() {
        scope.launch {
            try {
                // Fetch Users for resolving names
                val users = supabase.postgrest["users"].select().decodeList<UserDto>()
                val userMap = users.associateBy { it.id }

                // Fetch Tickets
                val ticketsDto = supabase.postgrest["tickets"].select().decodeList<TicketDto>()
                
                // Fetch Comments
                val commentsDto = supabase.postgrest["comments"].select().decodeList<CommentDto>()
                val commentsByTicket = commentsDto.groupBy { it.ticketId }
                
                // Fetch Activities
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

                // Fetch Notifications
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
        }
    }

    override fun getTickets(): Flow<List<Ticket>> = ticketsFlow

    override fun getTicketById(id: String): Flow<Ticket?> = ticketsFlow.map { list ->
        list.find { it.id == id }
    }

    override fun getNotifications(): Flow<List<AppNotification>> = notificationsFlow

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
            id = UUID.randomUUID().toString(),
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

        val activity1 = TicketActivityDto(UUID.randomUUID().toString(), id, "Tiket di-assign ke $assignee", actor, timestamp)
        val activity2 = TicketActivityDto(UUID.randomUUID().toString(), id, "Status otomatis berubah menjadi In Progress", "System", timestamp)
        supabase.postgrest["ticket_activities"].insert(listOf(activity1, activity2))

        pushNotification("Penugasan Tiket", "$id ditugaskan ke $assignee — status: In Progress", id)
        refreshData()
    }

    override suspend fun acceptTicket(id: String, actor: String) {
        @Serializable
        data class StatusUpdate(val status: String)
        
        supabase.postgrest["tickets"].update(StatusUpdate(TicketStatus.IN_PROGRESS.name)) {
            filter { eq("id", id) }
        }

        val activity = TicketActivityDto(UUID.randomUUID().toString(), id, "Tiket diterima oleh admin — status In Progress", actor, now())
        supabase.postgrest["ticket_activities"].insert(activity)

        pushNotification("Tiket Diterima", "$id telah diterima oleh $actor — status: In Progress", id)
        refreshData()
    }

    override suspend fun finishTicket(id: String, actor: String) {
        @Serializable
        data class StatusUpdate(val status: String)
        
        supabase.postgrest["tickets"].update(StatusUpdate(TicketStatus.CLOSED.name)) {
            filter { eq("id", id) }
        }

        val activity = TicketActivityDto(UUID.randomUUID().toString(), id, "Tiket diselesaikan — status Closed", actor, now())
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

        val activity = TicketActivityDto(UUID.randomUUID().toString(), ticketId, "Komentar baru dari ${comment.sender}", comment.sender, comment.timestamp)
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
            id = UUID.randomUUID().toString(),
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
}
