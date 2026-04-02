package com.attendance.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.attendance.app.ui.screens.attendance.AttendanceScreen
import com.attendance.app.ui.screens.attendance.MonthlyViewScreen
import com.attendance.app.ui.screens.backup.BackupScreen
import com.attendance.app.ui.screens.categories.CategoryScreen
import com.attendance.app.ui.screens.dashboard.QuickPunchScreen
import com.attendance.app.ui.screens.departments.DepartmentScreen
import com.attendance.app.ui.screens.employee.EmployeeFormScreen
import com.attendance.app.ui.screens.employee.EmployeeListScreen
import com.attendance.app.ui.screens.employee.EmployeeMonthDetailScreen
import com.attendance.app.ui.screens.kiosk.KioskScreen
import com.attendance.app.ui.screens.settings.SettingsScreen
import com.attendance.app.ui.screens.units.UnitManagementScreen

sealed class Screen(val route: String, val title: String) {
    data object Dashboard : Screen("dashboard", "Dashboard")
    data object Employees : Screen("employees", "Employees")
    data object EmployeeForm : Screen("employee_form?id={id}", "Employee") {
        fun createRoute(id: Long? = null): String {
            return if (id != null) "employee_form?id=$id" else "employee_form"
        }
    }
    data object Attendance : Screen("attendance", "Attendance")
    data object MonthlyView : Screen("monthly_view", "Monthly View")
    data object Departments : Screen("departments", "Departments")
    data object Categories : Screen("categories", "Categories")
    data object Units : Screen("units", "Units")
    data object Settings : Screen("settings", "Settings")
    data object Backup : Screen("backup", "Backup")
    data object Kiosk : Screen("kiosk", "Kiosk Mode")
    data object EmployeeMonthDetail : Screen(
        "employee_month_detail/{employeeId}/{year}/{month}",
        "Employee Record"
    ) {
        fun createRoute(employeeId: Long, year: Int, month: Int) =
            "employee_month_detail/$employeeId/$year/$month"
    }
}

data class BottomNavItem(
    val screen: Screen,
    val icon: @Composable () -> Unit,
    val label: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem(Screen.Dashboard, { Icon(Icons.Default.Home, null) }, "Home"),
        BottomNavItem(Screen.Employees, { Icon(Icons.Default.People, null) }, "Employees"),
        BottomNavItem(Screen.Attendance, { Icon(Icons.Default.CheckCircle, null) }, "Attendance"),
        BottomNavItem(Screen.Backup, { Icon(Icons.Default.Backup, null) }, "Backup"),
        BottomNavItem(Screen.Settings, { Icon(Icons.Default.Settings, null) }, "Settings")
    )

    val showBottomBar = currentRoute in bottomNavItems.map { it.screen.route }
    // Hide system bars in kiosk mode
    val isKioskMode = currentRoute == Screen.Kiosk.route

    Scaffold(
        bottomBar = {
            if (showBottomBar && !isKioskMode) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = item.icon,
                            label = { Text(item.label) },
                            selected = currentRoute == item.screen.route,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = if (isKioskMode) Modifier else Modifier.padding(paddingValues)
        ) {
            composable(Screen.Dashboard.route) {
                QuickPunchScreen(
                    onNavigateToEmployees = { navController.navigate(Screen.Employees.route) },
                    onNavigateToAttendance = { navController.navigate(Screen.Attendance.route) },
                    onNavigateToMonthlyView = { navController.navigate(Screen.MonthlyView.route) },
                    onNavigateToDepartments = { navController.navigate(Screen.Departments.route) },
                    onNavigateToCategories = { navController.navigate(Screen.Categories.route) },
                    onNavigateToKioskMode = { navController.navigate(Screen.Kiosk.route) },
                    onNavigateToUnits = { navController.navigate(Screen.Units.route) }
                )
            }

            composable(Screen.Employees.route) {
                EmployeeListScreen(
                    onAddEmployee = { navController.navigate(Screen.EmployeeForm.createRoute()) },
                    onEditEmployee = { id -> navController.navigate(Screen.EmployeeForm.createRoute(id)) }
                )
            }

            composable(
                route = Screen.EmployeeForm.route,
                arguments = listOf(navArgument("id") {
                    type = NavType.LongType
                    defaultValue = -1L
                })
            ) { backStackEntry ->
                val employeeId = backStackEntry.arguments?.getLong("id") ?: -1L
                EmployeeFormScreen(
                    employeeId = if (employeeId == -1L) null else employeeId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Attendance.route) {
                AttendanceScreen()
            }

            composable(Screen.MonthlyView.route) {
                MonthlyViewScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEmployeeDetail = { empId, year, month ->
                        navController.navigate(Screen.EmployeeMonthDetail.createRoute(empId, year, month))
                    }
                )
            }

            composable(
                route = Screen.EmployeeMonthDetail.route,
                arguments = listOf(
                    navArgument("employeeId") { type = NavType.LongType },
                    navArgument("year") { type = NavType.IntType },
                    navArgument("month") { type = NavType.IntType }
                )
            ) {
                EmployeeMonthDetailScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Departments.route) {
                DepartmentScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Categories.route) {
                CategoryScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Units.route) {
                UnitManagementScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen()
            }

            composable(Screen.Backup.route) {
                BackupScreen()
            }

            composable(Screen.Kiosk.route) {
                KioskScreen(
                    onExitKiosk = { navController.popBackStack() }
                )
            }
        }
    }
}
