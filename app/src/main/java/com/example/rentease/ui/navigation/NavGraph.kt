package com.example.rentease.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.rentease.ui.screens.AddEditItemScreen
import com.example.rentease.ui.screens.BrowseItemsScreen
import com.example.rentease.ui.screens.ChatListScreen
import com.example.rentease.ui.screens.ChatScreen
import com.example.rentease.ui.screens.CustomerServiceScreen
import com.example.rentease.ui.screens.DashboardAdminScreen
import com.example.rentease.ui.screens.DashboardPetugasScreen
import com.example.rentease.ui.screens.DashboardUserScreen
import com.example.rentease.ui.screens.FavoritesScreen
import com.example.rentease.ui.screens.HelpScreen
import com.example.rentease.ui.screens.HistoryScreen
import com.example.rentease.ui.screens.IncomingRentalsScreen
import com.example.rentease.ui.screens.ItemDetailScreen
import com.example.rentease.ui.screens.LoginScreen
import com.example.rentease.ui.screens.ManageItemsScreen
import com.example.rentease.ui.screens.ManageReturnsScreen
import com.example.rentease.ui.screens.ManageUsersScreen
import com.example.rentease.ui.screens.MyItemsScreen
import com.example.rentease.ui.screens.MyTransactionsScreen
import com.example.rentease.ui.screens.ProfileAdminScreen
import com.example.rentease.ui.screens.ProfilePetugasScreen
import com.example.rentease.ui.screens.ProfileUserScreen
import com.example.rentease.ui.screens.RegisterScreen
import com.example.rentease.ui.screens.ReportItemScreen
import com.example.rentease.ui.screens.SplashScreen
import com.example.rentease.ui.screens.TicketDetailScreen
import com.example.rentease.ui.screens.UserChatScreen
import com.example.rentease.ui.screens.UserComplaintsScreen
import com.example.rentease.ui.screens.AllItemsScreen
import com.example.rentease.ui.screens.VerifyRentalScreen
import com.example.rentease.ui.screens.VerifyUserItemsScreen
import com.example.rentease.ui.screens.ViewReportsScreen

@Composable
fun RentEaseNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToDashboard = { role ->
                    val dest = when (role) {
                        "admin" -> Screen.DashboardAdmin.route
                        "petugas" -> Screen.DashboardPetugas.route
                        else -> Screen.DashboardUser.route
                    }
                    navController.navigate(dest) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onLoginSuccess = { role ->
                    val dest = when (role) {
                        "admin" -> Screen.DashboardAdmin.route
                        "petugas" -> Screen.DashboardPetugas.route
                        else -> Screen.DashboardUser.route
                    }
                    navController.navigate(dest) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.DashboardUser.route) {
            DashboardUserScreen(
                navController = navController,
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.DashboardPetugas.route) {
            DashboardPetugasScreen(
                navController = navController,
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.DashboardAdmin.route) {
            DashboardAdminScreen(
                navController = navController,
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.BrowseItems.route) {
            BrowseItemsScreen(
                navController = navController,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ItemDetail.route,
            arguments = listOf(navArgument("itemId") { type = NavType.StringType })
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
            ItemDetailScreen(
                itemId = itemId,
                navController = navController,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Favorites.route) {
            FavoritesScreen(
                navController = navController,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.MyTransactions.route) {
            MyTransactionsScreen(
                navController = navController,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.MyItems.route) {
            MyItemsScreen(
                navController = navController,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.IncomingRentals.route) {
            IncomingRentalsScreen(
                navController = navController,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.History.route) {
            HistoryScreen(
                navController = navController,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.AddEditItem.route,
            arguments = listOf(
                navArgument("itemId") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("fromUser") { type = NavType.BoolType; defaultValue = false }
            )
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId")
            val fromUser = backStackEntry.arguments?.getBoolean("fromUser") ?: false
            AddEditItemScreen(
                itemId = itemId,
                isUserMode = fromUser,
                navController = navController,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Help.route) {
            HelpScreen(
                navController = navController,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ProfileUser.route) {
            ProfileUserScreen(
                navController = navController,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ProfilePetugas.route) {
            ProfilePetugasScreen(
                navController = navController,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ProfileAdmin.route) {
            ProfileAdminScreen(
                navController = navController,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ManageItems.route) {
            ManageItemsScreen(
                navController = navController,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ManageUsers.route) {
            ManageUsersScreen(
                navController = navController,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ManageReturns.route) {
            ManageReturnsScreen(
                navController = navController,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.VerifyRental.route) {
            VerifyRentalScreen(
                navController = navController,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.VerifyUserItems.route) {
            VerifyUserItemsScreen(
                navController = navController,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ViewReports.route) {
            ViewReportsScreen(
                navController = navController,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.UserComplaints.route) {
            UserComplaintsScreen(
                navController = navController,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.UserChat.route) {
            UserChatScreen(
                navController = navController,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ChatList.route) {
            ChatListScreen(
                navController = navController,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            ChatScreen(
                chatId = chatId,
                navController = navController,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ReportItem.route,
            arguments = listOf(
                navArgument("itemId") { type = NavType.StringType },
                navArgument("itemName") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
            val itemName = backStackEntry.arguments?.getString("itemName") ?: ""
            ReportItemScreen(
                itemId = itemId,
                itemName = java.net.URLDecoder.decode(itemName, "UTF-8"),
                navController = navController,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.TicketDetail.route,
            arguments = listOf(navArgument("ticketId") { type = NavType.StringType })
        ) { backStackEntry ->
            val ticketId = backStackEntry.arguments?.getString("ticketId") ?: ""
            TicketDetailScreen(
                ticketId = ticketId,
                navController = navController,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AllItems.route) {
            AllItemsScreen(
                navController = navController,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.CustomerService.route) {
            CustomerServiceScreen(
                navController = navController,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
