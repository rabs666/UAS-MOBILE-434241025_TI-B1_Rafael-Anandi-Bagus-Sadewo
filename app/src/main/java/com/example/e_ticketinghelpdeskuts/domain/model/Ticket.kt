package com.example.e_ticketinghelpdeskuts.domain.model

import java.util.UUID

data class Ticket(
    val id: String,
    val title: String,
    val description: String,
    val status: TicketStatus,
    val createdAt: String,
    val applicantId: String,
    val applicant: String,
    val assignedTo: String? = null,
    val attachmentSource: AttachmentSource = AttachmentSource.NONE,
    val attachmentName: String? = null,
    val attachmentUri: String? = null,
    val comments: List<Comment> = emptyList(),
    val activities: List<TicketActivity> = emptyList()
)

data class Comment(
    val id: String,
    val sender: String,
    val message: String,
    val timestamp: String
)

data class TicketActivity(
    val id: String,
    val title: String,
    val actor: String,
    val timestamp: String
)

data class AppUser(
    val id: String,
    val name: String,
    val username: String,
    val email: String,
    val password: String,
    val role: UserRole
)

data class AppNotification(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: String,
    val ticketId: String? = null,
    val isRead: Boolean = false
)

enum class TicketStatus {
    OPEN, ASSIGNED, IN_PROGRESS, CLOSED
}

enum class UserRole {
    USER, HELPDESK, ADMIN
}

enum class AttachmentSource {
    NONE, CAMERA, FILE
}
