package com.example.e_ticketinghelpdeskuts.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.e_ticketinghelpdeskuts.domain.model.TicketStatus
import com.example.e_ticketinghelpdeskuts.ui.theme.StatusClosedContainer
import com.example.e_ticketinghelpdeskuts.ui.theme.StatusClosedContainerDark
import com.example.e_ticketinghelpdeskuts.ui.theme.StatusClosedOnContainer
import com.example.e_ticketinghelpdeskuts.ui.theme.StatusClosedOnContainerDark
import com.example.e_ticketinghelpdeskuts.ui.theme.StatusOpenContainer
import com.example.e_ticketinghelpdeskuts.ui.theme.StatusOpenContainerDark
import com.example.e_ticketinghelpdeskuts.ui.theme.StatusOpenOnContainer
import com.example.e_ticketinghelpdeskuts.ui.theme.StatusOpenOnContainerDark
import com.example.e_ticketinghelpdeskuts.ui.theme.StatusAssignedContainer
import com.example.e_ticketinghelpdeskuts.ui.theme.StatusAssignedContainerDark
import com.example.e_ticketinghelpdeskuts.ui.theme.StatusAssignedOnContainer
import com.example.e_ticketinghelpdeskuts.ui.theme.StatusAssignedOnContainerDark
import com.example.e_ticketinghelpdeskuts.ui.theme.StatusProgressContainer
import com.example.e_ticketinghelpdeskuts.ui.theme.StatusProgressContainerDark
import com.example.e_ticketinghelpdeskuts.ui.theme.StatusProgressOnContainer
import com.example.e_ticketinghelpdeskuts.ui.theme.StatusProgressOnContainerDark

/** Human-readable label for a ticket status. */
fun TicketStatus.label(): String = when (this) {
    TicketStatus.OPEN -> "Open"
    TicketStatus.ASSIGNED -> "Assigned"
    TicketStatus.IN_PROGRESS -> "In Progress"
    TicketStatus.CLOSED -> "Selesai"
}

private data class StatusColors(val container: Color, val content: Color)

@Composable
private fun colorsFor(status: TicketStatus): StatusColors {
    val dark = isSystemInDarkTheme()
    return when (status) {
        TicketStatus.OPEN -> if (dark) {
            StatusColors(StatusOpenContainerDark, StatusOpenOnContainerDark)
        } else {
            StatusColors(StatusOpenContainer, StatusOpenOnContainer)
        }

        TicketStatus.ASSIGNED -> if (dark) {
            StatusColors(StatusAssignedContainerDark, StatusAssignedOnContainerDark)
        } else {
            StatusColors(StatusAssignedContainer, StatusAssignedOnContainer)
        }

        TicketStatus.IN_PROGRESS -> if (dark) {
            StatusColors(StatusProgressContainerDark, StatusProgressOnContainerDark)
        } else {
            StatusColors(StatusProgressContainer, StatusProgressOnContainer)
        }

        TicketStatus.CLOSED -> if (dark) {
            StatusColors(StatusClosedContainerDark, StatusClosedOnContainerDark)
        } else {
            StatusColors(StatusClosedContainer, StatusClosedOnContainer)
        }
    }
}

/** A single, theme-aware pill that renders a ticket status consistently everywhere. */
@Composable
fun StatusChip(status: TicketStatus, modifier: Modifier = Modifier) {
    val colors = colorsFor(status)
    Surface(
        modifier = modifier,
        color = colors.container,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = status.label().uppercase(),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = colors.content,
            style = MaterialTheme.typography.labelMedium
        )
    }
}
