package com.mart.villagemart.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.delay

object Routes {
    const val DASHBOARD = "dashboard"
    const val PRODUCTS = "products"
    const val PRODUCT_EDIT = "products/edit?productId={productId}"
    const val NEW_SALE = "sales/new"
    const val SALES_HISTORY = "sales/history"
    const val PURCHASES = "purchases?add={add}"
    const val INVENTORY = "inventory"
    const val LOW_STOCK = "inventory/low-stock"
    const val OUT_OF_STOCK = "inventory/out-of-stock"
    const val STOCK_MOVEMENTS = "inventory/movements"
    const val STOCK_ADJUSTMENT = "inventory/adjust"
    const val CUSTOMERS = "customers"
    const val VILLAGES = "villages"
    const val SUPPLIERS = "suppliers"
    const val EXPENSES = "expenses"
    const val CATEGORIES = "categories"
    const val NOTIFICATIONS = "notifications"
    const val REPORTS = "reports?type={type}"
    const val SETTINGS = "settings"
    const val MORE = "more"

    fun productEdit(productId: Long? = null): String =
        if (productId == null) "products/edit" else "products/edit?productId=$productId"

    fun purchases(add: Boolean = false): String = "purchases?add=$add"

    fun reports(type: String = DEFAULT_REPORT): String = "reports?type=$type"

    /** Routes rendered by the shared list/form screen, mapped to the feature key it understands. */
    val managementFeatures = mapOf(
        SALES_HISTORY to "sales",
        INVENTORY to "inventory",
        LOW_STOCK to "low-stock",
        OUT_OF_STOCK to "out-of-stock",
        STOCK_MOVEMENTS to "stock-movements",
        STOCK_ADJUSTMENT to "stock-adjustment",
        CUSTOMERS to "customers",
        VILLAGES to "villages",
        SUPPLIERS to "suppliers",
        EXPENSES to "expenses",
        CATEGORIES to "categories",
        NOTIFICATIONS to "notifications"
    )
}

private const val SPLASH_MIN_MILLIS = 1200L

@Composable
fun VillageMartRoot(viewModel: AppViewModel = hiltViewModel()) {
    val session by viewModel.state.collectAsStateWithLifecycle()
    var splashShown by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!splashShown) {
            delay(SPLASH_MIN_MILLIS)
            splashShown = true
        }
    }
    when {
        session.checking || !splashShown -> SplashScreen()
        !session.signedIn -> LoginScreen(
            busy = session.busy,
            error = session.error,
            onLogin = viewModel::signIn,
            onClearError = viewModel::clearError
        )
        else -> MainNavigation(
            ownerName = session.ownerName,
            signingOut = session.busy,
            onLogout = viewModel::signOut
        )
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.DASHBOARD, "Home", Icons.Filled.Home),
    Tab(Routes.PRODUCTS, "Products", Icons.Filled.Storefront),
    Tab(Routes.NEW_SALE, "Sell", Icons.Filled.PointOfSale),
    Tab(Routes.INVENTORY, "Stock", Icons.Filled.Inventory2),
    Tab(Routes.MORE, "More", Icons.Filled.MoreHoriz)
)

private fun tabFor(route: String?): String? = when {
    route == null -> null
    route == Routes.DASHBOARD -> Routes.DASHBOARD
    route.startsWith("products") -> Routes.PRODUCTS
    route == Routes.NEW_SALE -> Routes.NEW_SALE
    route.startsWith("inventory") -> Routes.INVENTORY
    else -> Routes.MORE
}

@Composable
private fun MainNavigation(ownerName: String, signingOut: Boolean, onLogout: () -> Unit) {
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val selectedTab = tabFor(entry?.destination?.route)
    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab.route,
                        onClick = { navController.navigateToTab(tab.route) },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        val back: () -> Unit = { navController.popBackStack() }
        val open: (String) -> Unit = { route -> navController.navigate(route) { launchSingleTop = true } }
        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            composable(Routes.DASHBOARD) {
                DashboardScreen(ownerName = ownerName, onNavigate = { route ->
                    if (route == Routes.NEW_SALE || route == Routes.PRODUCTS || route == Routes.INVENTORY) {
                        navController.navigateToTab(route)
                    } else {
                        open(route)
                    }
                })
            }
            composable(Routes.PRODUCTS) {
                ProductsScreen(
                    onAddProduct = { open(Routes.productEdit()) },
                    onEditProduct = { id -> open(Routes.productEdit(id)) }
                )
            }
            composable(
                Routes.PRODUCT_EDIT,
                arguments = listOf(navArgument("productId") { type = NavType.LongType; defaultValue = -1L })
            ) {
                ProductEditScreen(onBack = back, onSaved = back)
            }
            composable(Routes.NEW_SALE) {
                SalesScreen(onOpenHistory = { open(Routes.SALES_HISTORY) })
            }
            composable(
                Routes.PURCHASES,
                arguments = listOf(navArgument("add") { type = NavType.BoolType; defaultValue = false })
            ) { backStackEntry ->
                ManagementScreen(
                    feature = "purchases",
                    openAddOnStart = backStackEntry.arguments?.getBoolean("add") == true,
                    onBack = back
                )
            }
            composable(Routes.INVENTORY) {
                ManagementScreen(feature = "inventory", onOpen = open)
            }
            Routes.managementFeatures.filterKeys { it != Routes.INVENTORY }.forEach { (route, feature) ->
                composable(route) { ManagementScreen(feature = feature, onBack = back, onOpen = open) }
            }
            composable(
                Routes.REPORTS,
                arguments = listOf(navArgument("type") { type = NavType.StringType; defaultValue = DEFAULT_REPORT })
            ) {
                ReportsScreen(onBack = back)
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(ownerName = ownerName, signingOut = signingOut, onLogout = onLogout, onBack = back)
            }
            composable(Routes.MORE) {
                MoreScreen(onOpen = open)
            }
        }
    }
}

private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
