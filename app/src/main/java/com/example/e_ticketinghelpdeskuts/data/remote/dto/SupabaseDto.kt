package com.example.e_ticketinghelpdeskuts.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String,
    val name: String,
    val username: String,
    val email: String,
    // password perlu dibaca untuk login (dibandingkan di sisi klien, sesuai pola app ini)
    val password: String = "",
    val role: String,
    @SerialName("created_at")
    val createdAt: String
)

@Serializable
data class TicketDto(
    val id: String,
    val title: String,
    val description: String,
    val status: String,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("applicant_id")
    val applicantId: String,
    @SerialName("assigned_to")
    val assignedTo: String? = null,
    @SerialName("attachment_source")
    val attachmentSource: String,
    @SerialName("attachment_name")
    val attachmentName: String? = null
)

@Serializable
data class CommentDto(
    val id: String,
    @SerialName("ticket_id")
    val ticketId: String,
    val sender: String,
    val message: String,
    @SerialName("created_at")
    val createdAt: String
)

@Serializable
data class TicketActivityDto(
    val id: String,
    @SerialName("ticket_id")
    val ticketId: String,
    val title: String,
    val actor: String,
    @SerialName("created_at")
    val createdAt: String
)

@Serializable
data class NotificationDto(
    val id: String,
    val title: String,
    val message: String,
    @SerialName("created_at")
    val createdAt: String = "",
    @SerialName("ticket_id")
    val ticketId: String? = null,
    // Default false agar baris dengan is_read null/absen tetap ter-decode (tidak mematikan seluruh fetch).
    @SerialName("is_read")
    val isRead: Boolean = false
)
