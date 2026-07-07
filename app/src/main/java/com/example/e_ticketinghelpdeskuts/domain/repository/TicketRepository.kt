package com.example.e_ticketinghelpdeskuts.domain.repository

import com.example.e_ticketinghelpdeskuts.domain.model.AppNotification
import com.example.e_ticketinghelpdeskuts.domain.model.AppUser
import com.example.e_ticketinghelpdeskuts.domain.model.Comment
import com.example.e_ticketinghelpdeskuts.domain.model.Ticket
import com.example.e_ticketinghelpdeskuts.domain.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface TicketRepository {
    fun getTickets(): Flow<List<Ticket>>
    fun getTicketById(id: String): Flow<Ticket?>
    fun getNotifications(): Flow<List<AppNotification>>
    /** true selama proses tarik data ke Supabase berlangsung. Dipakai indikator pull-to-refresh. */
    val isRefreshing: StateFlow<Boolean>
    /** Ambil ulang data terbaru dari sumber (Supabase). Dipanggil saat layar dibuka / pull-to-refresh. */
    fun refresh()
    suspend fun createTicket(ticket: Ticket)
    suspend fun assignTicket(id: String, assignee: String, actor: String)
    suspend fun acceptTicket(id: String, actor: String)
    suspend fun finishTicket(id: String, actor: String)
    suspend fun addComment(ticketId: String, comment: Comment)
    suspend fun markNotificationAsRead(notificationId: String)
    suspend fun markAllNotificationsAsRead()

    // Manajemen pengguna (persist ke tabel users)
    fun getUsers(): Flow<List<AppUser>>
    suspend fun createUser(user: AppUser)
    suspend fun updateUserRole(id: String, role: UserRole)
    suspend fun deleteUser(id: String)
}
