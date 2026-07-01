package com.example.e_ticketinghelpdeskuts.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.e_ticketinghelpdeskuts.domain.model.TicketStatus
import com.example.e_ticketinghelpdeskuts.domain.model.UserRole
import com.example.e_ticketinghelpdeskuts.ui.navigation.Screen
import com.example.e_ticketinghelpdeskuts.ui.screens.ticket.TicketViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController, viewModel: TicketViewModel) {
    val tickets by viewModel.tickets.collectAsState()
    val unreadCount by viewModel.unreadNotificationCount.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    
    // Hitung statistik secara real-time
    val totalCount = tickets.size
    val openCount = tickets.count { it.status == TicketStatus.OPEN }
    val assignedCount = tickets.count { it.status == TicketStatus.ASSIGNED }
    val progressCount = tickets.count { it.status == TicketStatus.IN_PROGRESS }
    val closedCount = tickets.count { it.status == TicketStatus.CLOSED }

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.background
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("E-Helpdesk", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Notifications.route) }) {
                        BadgedBox(badge = { if (unreadCount > 0) Badge { Text(unreadCount.toString()) } }) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                        }
                    }
                    IconButton(onClick = { navController.navigate(Screen.Profile.route) }) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profile")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            if (currentUser != null) {
                FloatingActionButton(
                    onClick = { navController.navigate(Screen.CreateTicket.route) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Ticket")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(backgroundBrush)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Premium Welcome Card with Gradient
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column {
                        Text(
                            text = "Selamat datang,",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                        Text(
                            text = currentUser?.name ?: "Pengguna",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                            shape = CircleShape
                        ) {
                            Text(
                                text = currentUser?.role?.name ?: "USER",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }

            Text(
                text = "Ringkasan Tiket",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            // Re-designed Stat Grid
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                    StatCard("Total Tiket", totalCount.toString(), Icons.AutoMirrored.Filled.List, MaterialTheme.colorScheme.primary) {
                        viewModel.selectStatusFilter(null)
                        navController.navigate(Screen.TicketList.route)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    StatCard("In Progress", progressCount.toString(), Icons.Default.Build, MaterialTheme.colorScheme.secondary) {
                        viewModel.selectStatusFilter(TicketStatus.IN_PROGRESS)
                        navController.navigate(Screen.TicketList.route)
                    }
                }
                Column(modifier = Modifier.weight(1f).padding(start = 6.dp)) {
                    StatCard("Open", openCount.toString(), Icons.Default.Info, MaterialTheme.colorScheme.error) {
                        viewModel.selectStatusFilter(TicketStatus.OPEN)
                        navController.navigate(Screen.TicketList.route)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    StatCard("Selesai", closedCount.toString(), Icons.Default.CheckCircle, MaterialTheme.colorScheme.tertiary) {
                        viewModel.selectStatusFilter(TicketStatus.CLOSED)
                        navController.navigate(Screen.TicketList.route)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(28.dp))
            
            Text(
                text = "Menu Layanan",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            // Re-designed Quick Action Cards (List style for better layout)
            ActionMenuItem(
                title = "Riwayat Pengaduan",
                subtitle = "Lihat status dan detail tiket laporan",
                icon = Icons.AutoMirrored.Filled.List,
                iconColor = MaterialTheme.colorScheme.primary
            ) {
                viewModel.selectStatusFilter(null)
                navController.navigate(Screen.TicketList.route)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            ActionMenuItem(
                title = "Buat Tiket Baru",
                subtitle = "Laporkan kendala IT yang Anda hadapi",
                icon = Icons.Default.AddCircle,
                iconColor = MaterialTheme.colorScheme.secondary
            ) {
                navController.navigate(Screen.CreateTicket.route)
            }

            Spacer(modifier = Modifier.height(12.dp))
            ActionMenuItem(
                title = "Pusat Notifikasi",
                subtitle = "Pantau perkembangan dan respon tiket Anda",
                icon = Icons.Default.Notifications,
                iconColor = MaterialTheme.colorScheme.tertiary
            ) {
                navController.navigate(Screen.Notifications.route)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun StatCard(
    title: String, 
    value: String, 
    icon: ImageVector, 
    color: Color, 
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        border = CardStroke(1.dp, color.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Icon(
                icon, 
                contentDescription = null, 
                modifier = Modifier.size(28.dp), 
                tint = color
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = value, 
                style = MaterialTheme.typography.headlineLarge, 
                fontWeight = FontWeight.Bold, 
                color = color
            )
            Text(
                text = title, 
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ActionMenuItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = CardStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = iconColor.copy(alpha = 0.1f),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun CardStroke(width: androidx.compose.ui.unit.Dp, color: Color): androidx.compose.foundation.BorderStroke {
    return androidx.compose.foundation.BorderStroke(width, color)
}
