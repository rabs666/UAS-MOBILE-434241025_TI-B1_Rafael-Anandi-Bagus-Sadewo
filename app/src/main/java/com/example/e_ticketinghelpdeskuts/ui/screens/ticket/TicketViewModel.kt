package com.example.e_ticketinghelpdeskuts.ui.screens.ticket

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.e_ticketinghelpdeskuts.domain.model.AppNotification
import com.example.e_ticketinghelpdeskuts.domain.model.AppUser
import com.example.e_ticketinghelpdeskuts.domain.model.AttachmentSource
import com.example.e_ticketinghelpdeskuts.domain.model.Comment
import com.example.e_ticketinghelpdeskuts.domain.model.Ticket
import com.example.e_ticketinghelpdeskuts.domain.model.TicketActivity
import com.example.e_ticketinghelpdeskuts.domain.model.TicketStatus
import com.example.e_ticketinghelpdeskuts.domain.model.UserRole
import com.example.e_ticketinghelpdeskuts.domain.repository.TicketRepository
import com.example.e_ticketinghelpdeskuts.domain.usecase.GetTicketDetailUseCase
import com.example.e_ticketinghelpdeskuts.domain.usecase.GetTicketsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.random.Random

/**
 * Typed feedback for auth/permission actions. [isError] drives the banner colour & icon
 * explicitly, so the UI never has to guess intent by matching words in the message.
 */
data class AuthMessage(val text: String, val isError: Boolean) {
    companion object {
        fun error(text: String) = AuthMessage(text, isError = true)
        fun success(text: String) = AuthMessage(text, isError = false)
    }
}

class TicketViewModel(
    private val repository: TicketRepository
) : ViewModel() {

    private val getTicketsUseCase = GetTicketsUseCase(repository)
    private val getTicketDetailUseCase = GetTicketDetailUseCase(repository)

    private val _registeredUsers = MutableStateFlow(seedUsers())
    val registeredUsers: StateFlow<List<AppUser>> = _registeredUsers.asStateFlow()

    private val _currentUser = MutableStateFlow<AppUser?>(null)
    val currentUser: StateFlow<AppUser?> = _currentUser.asStateFlow()

    private val _authMessage = MutableStateFlow<AuthMessage?>(null)
    val authMessage: StateFlow<AuthMessage?> = _authMessage.asStateFlow()

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _selectedStatusFilter = MutableStateFlow<TicketStatus?>(null)
    val selectedStatusFilter: StateFlow<TicketStatus?> = _selectedStatusFilter.asStateFlow()

    fun selectStatusFilter(status: TicketStatus?) {
        _selectedStatusFilter.value = status
    }

    val isLoggedIn: StateFlow<Boolean> = currentUser
        .map { it != null }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val allTickets: StateFlow<List<Ticket>> = getTicketsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val tickets: StateFlow<List<Ticket>> = combine(allTickets, currentUser) { allTickets, activeUser ->
        when (activeUser?.role) {
            UserRole.USER -> allTickets.filter { it.applicantId == activeUser.id }
            UserRole.HELPDESK, UserRole.ADMIN -> allTickets
            null -> emptyList()
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val notifications: StateFlow<List<AppNotification>> = repository.getNotifications()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val unreadNotificationCount: StateFlow<Int> = notifications
        .map { list -> list.count { !it.isRead } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val assignableAgents: StateFlow<List<String>> = registeredUsers
        .map { users ->
            users.filter { it.role == UserRole.HELPDESK || it.role == UserRole.ADMIN }
                .map { it.name }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun clearAuthMessage() {
        _authMessage.value = null
    }

    fun login(username: String, password: String): Boolean {
        if (username.isBlank() || password.isBlank()) {
            _authMessage.value = AuthMessage.error("Username dan password wajib diisi.")
            return false
        }

        val user = _registeredUsers.value.find {
            it.username.equals(username.trim(), ignoreCase = true) && it.password == password
        }

        return if (user == null) {
            _authMessage.value = AuthMessage.error("Login gagal. Cek username atau password.")
            false
        } else {
            _currentUser.value = user
            _authMessage.value = AuthMessage.success("Selamat datang, ${user.name}.")
            true
        }
    }

    fun register(name: String, username: String, email: String, password: String): Boolean {
        if (name.isBlank() || username.isBlank() || email.isBlank() || password.isBlank()) {
            _authMessage.value = AuthMessage.error("Semua field wajib diisi.")
            return false
        }

        if (password.length < 6) {
            _authMessage.value = AuthMessage.error("Password minimal 6 karakter.")
            return false
        }

        val users = _registeredUsers.value
        if (users.any { it.username.equals(username.trim(), ignoreCase = true) }) {
            _authMessage.value = AuthMessage.error("Username sudah dipakai.")
            return false
        }
        if (users.any { it.email.equals(email.trim(), ignoreCase = true) }) {
            _authMessage.value = AuthMessage.error("Email sudah terdaftar.")
            return false
        }

        val newUser = AppUser(
            id = "U-${Random.nextInt(1000, 9999)}",
            name = name.trim(),
            username = username.trim(),
            email = email.trim(),
            password = password,
            role = UserRole.USER
        )

        _registeredUsers.value = users + newUser
        _authMessage.value = AuthMessage.success("Registrasi berhasil. Silakan login.")
        return true
    }

    fun resetPassword(email: String): Boolean {
        if (email.isBlank()) {
            _authMessage.value = AuthMessage.error("Email wajib diisi.")
            return false
        }

        val exists = _registeredUsers.value.any { it.email.equals(email.trim(), ignoreCase = true) }
        return if (exists) {
            _authMessage.value = AuthMessage.success("Instruksi reset password telah dikirim ke ${email.trim()}.")
            true
        } else {
            _authMessage.value = AuthMessage.error("Email tidak ditemukan.")
            false
        }
    }

    fun logout() {
        _currentUser.value = null
        _authMessage.value = AuthMessage.success("Berhasil logout.")
    }

    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
    }

    fun getTicketDetail(id: String): StateFlow<Ticket?> {
        return combine(getTicketDetailUseCase(id), currentUser) { ticket, activeUser ->
            if (ticket == null || activeUser == null) {
                null
            } else if (activeUser.role == UserRole.USER && ticket.applicantId != activeUser.id) {
                null
            } else {
                ticket
            }
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )
    }

    fun createTicket(
        title: String,
        description: String,
        attachmentSource: AttachmentSource,
        attachmentName: String?
    ) {
        val user = _currentUser.value
        if (user == null) {
            _authMessage.value = AuthMessage.error("Silakan login terlebih dahulu.")
            return
        }

        viewModelScope.launch {
            val now = currentTimestamp()
            val newTicket = Ticket(
                id = "T-${Random.nextInt(1000, 9999)}",
                title = title.trim(),
                description = description.trim(),
                status = TicketStatus.OPEN,
                createdAt = now,
                applicantId = user.id,
                applicant = user.name,
                attachmentSource = attachmentSource,
                attachmentName = attachmentName?.trim()?.takeIf { it.isNotEmpty() },
                activities = listOf(
                    TicketActivity(
                        id = UUID.randomUUID().toString(),
                        title = "Tiket dibuat",
                        actor = user.name,
                        timestamp = now
                    )
                )
            )
            repository.createTicket(newTicket)
        }
    }

    fun updateStatus(id: String, status: TicketStatus) {
        val actor = _currentUser.value
        if (actor == null) {
            _authMessage.value = AuthMessage.error("Silakan login terlebih dahulu.")
            return
        }

        if (actor.role == UserRole.USER) {
            _authMessage.value = AuthMessage.error("Hanya helpdesk/admin yang dapat mengubah status tiket.")
            return
        }

        viewModelScope.launch {
            repository.updateTicketStatus(id, status, actor.name)
        }
    }

    fun assignTicket(id: String, assignee: String) {
        val actor = _currentUser.value
        if (actor == null) {
            _authMessage.value = AuthMessage.error("Silakan login terlebih dahulu.")
            return
        }

        if (actor.role == UserRole.USER) {
            _authMessage.value = AuthMessage.error("Hanya helpdesk/admin yang dapat assign tiket.")
            return
        }

        viewModelScope.launch {
            repository.assignTicket(id, assignee, actor.name)
        }
    }

    fun addComment(ticketId: String, message: String) {
        val actor = _currentUser.value ?: return
        val cleanMessage = message.trim()
        if (cleanMessage.isEmpty()) return

        viewModelScope.launch {
            val comment = Comment(
                id = UUID.randomUUID().toString(),
                sender = actor.name,
                message = cleanMessage,
                timestamp = currentTimestamp()
            )
            repository.addComment(ticketId, comment)
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead()
        }
    }

    fun roleLabel(role: UserRole): String {
        return when (role) {
            UserRole.USER -> "User"
            UserRole.HELPDESK -> "Helpdesk"
            UserRole.ADMIN -> "Admin"
        }
    }

    private fun currentTimestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
    }

    private fun seedUsers(): List<AppUser> {
        return listOf(
            AppUser("U-001", "Ahmad Dani", "ahmad", "ahmad@campus.ac.id", "123456", UserRole.USER),
            AppUser("U-002", "Siti Aminah", "siti", "siti@campus.ac.id", "123456", UserRole.USER),
            AppUser("U-003", "Budi Utomo", "budi", "budi@campus.ac.id", "123456", UserRole.USER),
            AppUser("H-001", "Rina Helpdesk", "helpdesk", "helpdesk@campus.ac.id", "123456", UserRole.HELPDESK),
            AppUser("H-002", "Arif Helpdesk", "arif", "arif@campus.ac.id", "123456", UserRole.HELPDESK),
            AppUser("A-001", "Admin UTS", "admin", "admin@campus.ac.id", "123456", UserRole.ADMIN)
        )
    }
}

class TicketViewModelFactory(private val repository: TicketRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TicketViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TicketViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
