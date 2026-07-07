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

    // Daftar pengguna kini bersumber dari repository (Supabase, dengan fallback seed).
    // Eagerly agar registeredUsers.value selalu terisi saat login diproses.
    val registeredUsers: StateFlow<List<AppUser>> = repository.getUsers()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = seedUsers()
        )

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

    // Notifikasi difilter berdasarkan role:
    // - USER hanya melihat notifikasi untuk tiket miliknya (berdasarkan ticketId di allTickets)
    // - ADMIN / HELPDESK melihat semua notifikasi
    val notifications: StateFlow<List<AppNotification>> = combine(
        repository.getNotifications(),
        currentUser,
        allTickets
    ) { allNotifs, activeUser, tickets ->
        when (activeUser?.role) {
            UserRole.USER -> {
                val ownTicketIds = tickets
                    .filter { it.applicantId == activeUser.id }
                    .map { it.id }
                    .toSet()
                allNotifs.filter { notif ->
                    notif.ticketId == null || notif.ticketId in ownTicketIds
                }
            }
            UserRole.HELPDESK, UserRole.ADMIN -> allNotifs
            null -> emptyList()
        }
    }
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

    /** Status tarik-data dari repository — dipakai indikator pull-to-refresh di UI. */
    val isRefreshing: StateFlow<Boolean> = repository.isRefreshing

    init {
        // Auto-refresh berkala: menarik data terbaru dari Supabase tiap 15 detik selama
        // pengguna login. Ini membuat notifikasi & dot merah muncul otomatis (live) tanpa
        // harus keluar-masuk layar dulu.
        viewModelScope.launch {
            while (true) {
                delay(15_000)
                if (_currentUser.value != null) {
                    repository.refresh()
                }
            }
        }
    }

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

    /** Tarik data terbaru dari Supabase. Dipanggil saat layar daftar/dashboard dibuka. */
    fun refresh() {
        repository.refresh()
    }

    fun login(username: String, password: String): Boolean {
        if (username.isBlank() || password.isBlank()) {
            _authMessage.value = AuthMessage.error("Username dan password wajib diisi.")
            return false
        }

        val user = registeredUsers.value.find {
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

        val users = registeredUsers.value
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

        // Persist ke Supabase (async). Validasi sudah lolos, jadi optimistis sukses.
        _authMessage.value = AuthMessage.success("Registrasi berhasil. Silakan login.")
        viewModelScope.launch {
            try {
                repository.createUser(newUser)
            } catch (e: Exception) {
                _authMessage.value = AuthMessage.error("Registrasi gagal disimpan: ${e.message ?: "coba lagi"}")
            }
        }
        return true
    }

    fun resetPassword(email: String): Boolean {
        if (email.isBlank()) {
            _authMessage.value = AuthMessage.error("Email wajib diisi.")
            return false
        }

        val exists = registeredUsers.value.any { it.email.equals(email.trim(), ignoreCase = true) }
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
        attachmentName: String?,
        attachmentUri: String? = null
    ) {
        val user = _currentUser.value
        if (user == null) {
            _authMessage.value = AuthMessage.error("Silakan login terlebih dahulu.")
            return
        }

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
            attachmentUri = attachmentUri,
            activities = listOf(
                TicketActivity(
                    id = UUID.randomUUID().toString(),
                    title = "Tiket dibuat",
                    actor = user.name,
                    timestamp = now
                )
            )
        )
        runTicketAction("membuat tiket") {
            repository.createTicket(newTicket)
        }
    }

    /**
     * Menjalankan aksi tiket (yang menulis ke Supabase) dengan aman.
     * Jika repository melempar exception (mis. constraint DB, jaringan),
     * ditangkap di sini agar aplikasi TIDAK force-close — cukup tampil pesan.
     */
    private fun runTicketAction(errorContext: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                _authMessage.value = AuthMessage.error(
                    "Gagal $errorContext. ${e.message ?: "Periksa koneksi / database."}"
                )
            }
        }
    }

    // Step 3: Menugaskan helpdesk = hak ADMIN saja (SRS FR-007 #4).
    // Saat assign, status otomatis berubah OPEN/ASSIGNED → IN_PROGRESS di repository.
    fun assignTicket(id: String, assignee: String) {
        val actor = _currentUser.value
        if (actor == null) {
            _authMessage.value = AuthMessage.error("Silakan login terlebih dahulu.")
            return
        }

        if (actor.role != UserRole.ADMIN) {
            _authMessage.value = AuthMessage.error("Hanya admin yang dapat menugaskan helpdesk.")
            return
        }

        runTicketAction("menugaskan helpdesk") {
            repository.assignTicket(id, assignee, actor.name)
        }
    }

    fun acceptTicket(id: String) {
        val actor = _currentUser.value
        if (actor == null) {
            _authMessage.value = AuthMessage.error("Silakan login terlebih dahulu.")
            return
        }

        if (actor.role != UserRole.ADMIN) {
            _authMessage.value = AuthMessage.error("Hanya admin yang dapat menerima tiket.")
            return
        }

        runTicketAction("menerima tiket") {
            repository.acceptTicket(id, actor.name)
        }
    }

    // Step 4: Menyelesaikan/menutup tiket = hak HELPDESK saja.
    // Helpdesk yang mengerjakan tiket, jadi hanya helpdesk yang bisa klik Selesai (IN_PROGRESS → CLOSED).
    fun finishTicket(id: String) {
        val actor = _currentUser.value
        if (actor == null) {
            _authMessage.value = AuthMessage.error("Silakan login terlebih dahulu.")
            return
        }

        if (actor.role != UserRole.HELPDESK) {
            _authMessage.value = AuthMessage.error("Hanya helpdesk yang dapat menyelesaikan tiket.")
            return
        }

        runTicketAction("menyelesaikan tiket") {
            repository.finishTicket(id, actor.name)
        }
    }

    fun addComment(ticketId: String, message: String) {
        val actor = _currentUser.value ?: return
        val cleanMessage = message.trim()
        if (cleanMessage.isEmpty()) return

        val comment = Comment(
            // ID ringkas (<=20 char) agar muat di kolom comments.id VARCHAR(20) Supabase.
            id = "CM-" + UUID.randomUUID().toString().replace("-", "").take(14),
            sender = actor.name,
            message = cleanMessage,
            timestamp = currentTimestamp()
        )
        runTicketAction("menambah komentar") {
            repository.addComment(ticketId, comment)
        }
    }

    fun markNotificationAsRead(notificationId: String) {
        runTicketAction("memperbarui notifikasi") {
            repository.markNotificationAsRead(notificationId)
        }
    }

    fun markAllNotificationsAsRead() {
        runTicketAction("memperbarui notifikasi") {
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

    // ---------------------------------------------------------------------
    // Manajemen Pengguna (SRS FR-007 #7) — hanya ADMIN.
    // Perubahan dipersist ke Supabase lewat repository; registeredUsers ikut ter-refresh.
    // ---------------------------------------------------------------------

    /** Prefix id mengikuti role agar konsisten dengan seed (U-/H-/A-). */
    private fun idPrefixFor(role: UserRole): String = when (role) {
        UserRole.USER -> "U"
        UserRole.HELPDESK -> "H"
        UserRole.ADMIN -> "A"
    }

    private fun requireAdmin(action: String): Boolean {
        val actor = _currentUser.value
        if (actor == null) {
            _authMessage.value = AuthMessage.error("Silakan login terlebih dahulu.")
            return false
        }
        if (actor.role != UserRole.ADMIN) {
            _authMessage.value = AuthMessage.error("Hanya admin yang dapat $action.")
            return false
        }
        return true
    }

    /** Admin menambah pengguna baru dengan role pilihan (USER/HELPDESK/ADMIN). */
    fun createUser(
        name: String,
        username: String,
        email: String,
        password: String,
        role: UserRole
    ): Boolean {
        if (!requireAdmin("menambah pengguna")) return false

        if (name.isBlank() || username.isBlank() || email.isBlank() || password.isBlank()) {
            _authMessage.value = AuthMessage.error("Semua field wajib diisi.")
            return false
        }
        if (password.length < 6) {
            _authMessage.value = AuthMessage.error("Password minimal 6 karakter.")
            return false
        }

        val users = registeredUsers.value
        if (users.any { it.username.equals(username.trim(), ignoreCase = true) }) {
            _authMessage.value = AuthMessage.error("Username sudah dipakai.")
            return false
        }
        if (users.any { it.email.equals(email.trim(), ignoreCase = true) }) {
            _authMessage.value = AuthMessage.error("Email sudah terdaftar.")
            return false
        }

        val newUser = AppUser(
            id = "${idPrefixFor(role)}-${Random.nextInt(1000, 9999)}",
            name = name.trim(),
            username = username.trim(),
            email = email.trim(),
            password = password,
            role = role
        )
        _authMessage.value = AuthMessage.success("Pengguna ${newUser.name} (${roleLabel(role)}) ditambahkan.")
        viewModelScope.launch {
            try {
                repository.createUser(newUser)
            } catch (e: Exception) {
                _authMessage.value = AuthMessage.error("Gagal menyimpan pengguna: ${e.message ?: "coba lagi"}")
            }
        }
        return true
    }

    /** Admin mengubah role pengguna. */
    fun updateUserRole(userId: String, newRole: UserRole): Boolean {
        if (!requireAdmin("mengubah role pengguna")) return false

        val users = registeredUsers.value
        val target = users.find { it.id == userId }
        if (target == null) {
            _authMessage.value = AuthMessage.error("Pengguna tidak ditemukan.")
            return false
        }
        _authMessage.value = AuthMessage.success("Role ${target.name} diubah menjadi ${roleLabel(newRole)}.")
        viewModelScope.launch {
            try {
                repository.updateUserRole(userId, newRole)
            } catch (e: Exception) {
                _authMessage.value = AuthMessage.error("Gagal mengubah role: ${e.message ?: "coba lagi"}")
            }
        }
        return true
    }

    /** Admin menghapus pengguna. Tidak boleh menghapus akun sendiri. */
    fun deleteUser(userId: String): Boolean {
        if (!requireAdmin("menghapus pengguna")) return false

        if (_currentUser.value?.id == userId) {
            _authMessage.value = AuthMessage.error("Tidak dapat menghapus akun sendiri.")
            return false
        }
        val users = registeredUsers.value
        val target = users.find { it.id == userId }
        if (target == null) {
            _authMessage.value = AuthMessage.error("Pengguna tidak ditemukan.")
            return false
        }
        _authMessage.value = AuthMessage.success("Pengguna ${target.name} dihapus.")
        viewModelScope.launch {
            try {
                repository.deleteUser(userId)
            } catch (e: Exception) {
                _authMessage.value = AuthMessage.error("Gagal menghapus pengguna: ${e.message ?: "coba lagi"}")
            }
        }
        return true
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
