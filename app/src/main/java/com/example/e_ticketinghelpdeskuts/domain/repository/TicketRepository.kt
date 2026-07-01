package com.example.e_ticketinghelpdeskuts.domain.repository

import com.example.e_ticketinghelpdeskuts.domain.model.AppNotification
import com.example.e_ticketinghelpdeskuts.domain.model.Comment
import com.example.e_ticketinghelpdeskuts.domain.model.Ticket
import com.example.e_ticketinghelpdeskuts.domain.model.TicketStatus
import kotlinx.coroutines.flow.Flow

interface TicketRepository {
    fun getTickets(): Flow<List<Ticket>>
    fun getTicketById(id: String): Flow<Ticket?>
    fun getNotifications(): Flow<List<AppNotification>>
    suspend fun createTicket(ticket: Ticket)
    suspend fun updateTicketStatus(id: String, status: TicketStatus, actor: String)
    suspend fun assignTicket(id: String, assignee: String, actor: String)
    suspend fun finishTicket(id: String, actor: String)
    suspend fun addComment(ticketId: String, comment: Comment)
    suspend fun markNotificationAsRead(notificationId: String)
    suspend fun markAllNotificationsAsRead()
}
