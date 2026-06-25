package com.example.rentease.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object DashboardUser : Screen("dashboard/user")
    object DashboardPetugas : Screen("dashboard/petugas")
    object DashboardAdmin : Screen("dashboard/admin")
    object BrowseItems : Screen("browse_items")
    object ItemDetail : Screen("item_detail/{itemId}") {
        fun createRoute(itemId: String) = "item_detail/$itemId"
    }
    object Favorites : Screen("favorites")
    object MyTransactions : Screen("my_transactions")
    object MyItems : Screen("my_items")
    object IncomingRentals : Screen("incoming_rentals")
    object History : Screen("history")
    object AddEditItem : Screen("add_edit_item?itemId={itemId}&fromUser={fromUser}") {
        fun createRoute(itemId: String? = null, fromUser: Boolean = false) =
            buildString {
                append("add_edit_item")
                append("?fromUser=$fromUser")
                if (itemId != null) append("&itemId=$itemId")
            }
    }
    object Help : Screen("help")
    object ProfileUser : Screen("profile/user")
    object ProfilePetugas : Screen("profile/petugas")
    object ProfileAdmin : Screen("profile/admin")
    object ManageItems : Screen("manage_items")
    object ManageUsers : Screen("manage_users")
    object ManageReturns : Screen("manage_returns")
    object VerifyRental : Screen("verify_rental")
    object VerifyUserItems : Screen("verify_user_items")
    object ViewReports : Screen("view_reports")
    object UserComplaints : Screen("user_complaints")
    object ChatList : Screen("chat_list")
    object Chat : Screen("chat/{chatId}") {
        fun createRoute(chatId: String) = "chat/$chatId"
    }
    object ReportItem : Screen("report_item/{itemId}?itemName={itemName}") {
        fun createRoute(itemId: String, itemName: String = "") =
            "report_item/$itemId?itemName=${java.net.URLEncoder.encode(itemName, "UTF-8")}"
    }
    object TicketDetail : Screen("ticket_detail/{ticketId}") {
        fun createRoute(ticketId: String) = "ticket_detail/$ticketId"
    }
    object CustomerService : Screen("customer_service")
    object AllItems : Screen("all_items")
}
