package com.example.e_ticketinghelpdeskuts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.e_ticketinghelpdeskuts.data.remote.supabase
import com.example.e_ticketinghelpdeskuts.data.repository.FakeTicketRepository
import com.example.e_ticketinghelpdeskuts.data.repository.SupabaseTicketRepository
import com.example.e_ticketinghelpdeskuts.ui.navigation.Screen
import com.example.e_ticketinghelpdeskuts.ui.screens.auth.LoginScreen
import com.example.e_ticketinghelpdeskuts.ui.screens.auth.RegisterScreen
import com.example.e_ticketinghelpdeskuts.ui.screens.auth.ResetPasswordScreen
import com.example.e_ticketinghelpdeskuts.ui.screens.dashboard.DashboardScreen
import com.example.e_ticketinghelpdeskuts.ui.screens.notification.NotificationScreen
import com.example.e_ticketinghelpdeskuts.ui.screens.profile.ProfileScreen
import com.example.e_ticketinghelpdeskuts.ui.screens.splash.SplashScreen
import com.example.e_ticketinghelpdeskuts.ui.screens.ticket.CreateTicketScreen
import com.example.e_ticketinghelpdeskuts.ui.screens.ticket.TicketDetailScreen
import com.example.e_ticketinghelpdeskuts.ui.screens.ticket.TicketListScreen
import com.example.e_ticketinghelpdeskuts.ui.screens.ticket.TicketViewModel
import com.example.e_ticketinghelpdeskuts.ui.screens.ticket.TicketViewModelFactory
import com.example.e_ticketinghelpdeskuts.ui.theme.ETicketingHelpdeskUTSTheme
import com.example.e_ticketinghelpdeskuts.utils.DebugUtils

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        DebugUtils.logDebug("MainActivity.onCreate() started")
        
        setContent {
            val repository = remember { 
                DebugUtils.logDebug("Creating SupabaseTicketRepository")
                SupabaseTicketRepository(supabase)
            }
            val ticketViewModel: TicketViewModel = viewModel(
                factory = TicketViewModelFactory(repository)
            )
            DebugUtils.logDebug("TicketViewModel created successfully")
            val isDarkMode by ticketViewModel.isDarkMode.collectAsState()

            ETicketingHelpdeskUTSTheme(
                darkTheme = isDarkMode,
                dynamicColor = false
            ) {
                AppNavigation(ticketViewModel)
            }
        }
        DebugUtils.logDebug("MainActivity.onCreate() completed")
    }
}

@Composable
fun AppNavigation(ticketViewModel: TicketViewModel) {
    val navController = rememberNavController()
    val isLoggedIn by ticketViewModel.isLoggedIn.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(navController, isLoggedIn)
        }
        composable(Screen.Login.route) {
            LoginScreen(navController, ticketViewModel)
        }
        composable(Screen.Register.route) {
            RegisterScreen(navController, ticketViewModel)
        }
        composable(Screen.ResetPassword.route) {
            ResetPasswordScreen(navController, ticketViewModel)
        }
        composable(Screen.Dashboard.route) {
            DashboardScreen(navController, ticketViewModel)
        }
        composable(Screen.TicketList.route) {
            TicketListScreen(navController, ticketViewModel)
        }
        composable(
            route = Screen.TicketDetail.route,
            arguments = listOf(navArgument("ticketId") { type = NavType.StringType })
        ) { backStackEntry ->
            val ticketId = backStackEntry.arguments?.getString("ticketId")
            TicketDetailScreen(navController, ticketViewModel, ticketId)
        }
        composable(Screen.CreateTicket.route) {
            CreateTicketScreen(navController, ticketViewModel)
        }
        composable(Screen.Profile.route) {
            ProfileScreen(navController, ticketViewModel)
        }
        composable(Screen.Notifications.route) {
            NotificationScreen(navController, ticketViewModel)
        }
    }
}
