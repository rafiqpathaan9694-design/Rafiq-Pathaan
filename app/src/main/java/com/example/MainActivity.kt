package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.i18n.LocalAppStrings
import com.example.i18n.stringsFor
import com.example.ui.MainViewModel
import com.example.ui.StudentFilter
import com.example.ui.screens.BackupExportScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FormReviewScreen
import com.example.ui.screens.PinLockScreen
import com.example.ui.screens.PrintPreviewScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StudentFormScreen
import com.example.ui.screens.StudentListScreen
import com.example.ui.theme.MobileSolutionTheme

object AppRoutes {
    const val DASHBOARD = "dashboard"
    const val STUDENTS = "students"
    const val BACKUP = "backup"
    const val SETTINGS = "settings"
    const val STUDENT_FORM = "student_form"
    const val FORM_REVIEW = "form_review"
    const val PRINT_PREVIEW = "print_preview/{studentId}"

    fun printPreview(studentId: Long): String = "print_preview/$studentId"
}

private data class TopLevelNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MobileSolutionTheme {
                MobileSolutionApp()
            }
        }
    }
}

@Composable
fun MobileSolutionApp(
    viewModel: MainViewModel = viewModel()
) {
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val isUnlocked by viewModel.isUnlocked.collectAsStateWithLifecycle()
    val strings = stringsFor(settings.language)

    CompositionLocalProvider(LocalAppStrings provides strings) {
        if (settings.pinEnabled && !isUnlocked) {
            PinLockScreen(
                onVerifyPin = { pin -> viewModel.unlockWithPin(pin) }
            )
            return@CompositionLocalProvider
        }

        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route

        val stats by viewModel.dashboardStats.collectAsStateWithLifecycle()
        val allStudents by viewModel.allStudents.collectAsStateWithLifecycle()
        val filteredStudents by viewModel.filteredStudents.collectAsStateWithLifecycle()
        val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
        val statusFilter by viewModel.statusFilter.collectAsStateWithLifecycle()
        val formState by viewModel.formState.collectAsStateWithLifecycle()
        val backupBusy by viewModel.backupBusy.collectAsStateWithLifecycle()
        val context = LocalContext.current

        val topLevelItems = listOf(
            TopLevelNavItem(
                route = AppRoutes.DASHBOARD,
                label = strings.navDashboard,
                selectedIcon = Icons.Filled.Dashboard,
                unselectedIcon = Icons.Outlined.Dashboard,
                testTag = "nav_dashboard"
            ),
            TopLevelNavItem(
                route = AppRoutes.STUDENTS,
                label = strings.navStudents,
                selectedIcon = Icons.Filled.Group,
                unselectedIcon = Icons.Outlined.Group,
                testTag = "nav_students"
            ),
            TopLevelNavItem(
                route = AppRoutes.BACKUP,
                label = strings.navBackup,
                selectedIcon = Icons.Filled.Backup,
                unselectedIcon = Icons.Outlined.Backup,
                testTag = "nav_backup"
            ),
            TopLevelNavItem(
                route = AppRoutes.SETTINGS,
                label = strings.navSettings,
                selectedIcon = Icons.Filled.Settings,
                unselectedIcon = Icons.Outlined.Settings,
                testTag = "nav_settings"
            )
        )

        val showTopLevelNav = currentRoute in topLevelItems.map { it.route }

        fun navigateTopLevel(route: String) {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavRail = maxWidth >= 600.dp

            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    if (showTopLevelNav && !useNavRail) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            topLevelItems.forEach { item ->
                                val selected = currentRoute == item.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTopLevel(item.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label
                                        )
                                    },
                                    label = { Text(item.label) },
                                    modifier = Modifier.testTag(item.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (showTopLevelNav && useNavRail) {
                        NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                            topLevelItems.forEach { item ->
                                val selected = currentRoute == item.route
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTopLevel(item.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label
                                        )
                                    },
                                    label = { Text(item.label) },
                                    modifier = Modifier.testTag(item.testTag)
                                )
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = AppRoutes.DASHBOARD,
                        modifier = Modifier.weight(1f)
                    ) {
                        composable(AppRoutes.DASHBOARD) {
                            DashboardScreen(
                                stats = stats,
                                recentStudents = allStudents,
                                onNewStudentClick = {
                                    viewModel.startNewForm()
                                    navController.navigate(AppRoutes.STUDENT_FORM)
                                },
                                onOpenStudentsFilter = { filter ->
                                    viewModel.setStatusFilter(filter)
                                    navigateTopLevel(AppRoutes.STUDENTS)
                                },
                                onOpenBackupExport = {
                                    navigateTopLevel(AppRoutes.BACKUP)
                                },
                                onEditStudent = { id ->
                                    viewModel.loadStudentForEdit(id)
                                    navController.navigate(AppRoutes.STUDENT_FORM)
                                },
                                onPreviewStudent = { id ->
                                    navController.navigate(AppRoutes.printPreview(id))
                                }
                            )
                        }

                        composable(AppRoutes.STUDENTS) {
                            StudentListScreen(
                                students = filteredStudents,
                                searchQuery = searchQuery,
                                activeFilter = statusFilter,
                                onSearchChange = viewModel::setSearchQuery,
                                onFilterChange = viewModel::setStatusFilter,
                                onNewStudentClick = {
                                    viewModel.startNewForm()
                                    navController.navigate(AppRoutes.STUDENT_FORM)
                                },
                                onEditStudent = { id ->
                                    viewModel.loadStudentForEdit(id)
                                    navController.navigate(AppRoutes.STUDENT_FORM)
                                },
                                onPreviewPrintStudent = { id ->
                                    navController.navigate(AppRoutes.printPreview(id))
                                },
                                onDeleteStudent = viewModel::deleteStudent
                            )
                        }

                        composable(AppRoutes.BACKUP) {
                            BackupExportScreen(
                                students = allStudents,
                                isBusy = backupBusy,
                                onExportCsvUri = viewModel::exportCsvToUri,
                                onExportBackupZipUri = viewModel::exportBackupZipToUri,
                                onInspectBackupZip = viewModel::inspectBackupFile,
                                onRestoreBackupZip = viewModel::restoreBackupFile
                            )
                        }

                        composable(AppRoutes.SETTINGS) {
                            SettingsScreen(
                                settings = settings,
                                onSelectLanguage = viewModel::setLanguage,
                                onSetPin = viewModel::setPin,
                                onDisablePin = viewModel::disablePin
                            )
                        }

                        composable(AppRoutes.STUDENT_FORM) {
                            StudentFormScreen(
                                formState = formState,
                                documentStorage = viewModel.documentStorage,
                                onUpdateRecord = viewModel::updateDraft,
                                onSelectSection = viewModel::setActiveSection,
                                onAttachUri = { slot, uri ->
                                    viewModel.attachDocumentFromUri(slot, uri) { ok ->
                                        if (!ok) {
                                            Toast.makeText(context, "Could not load selected image", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onAttachCameraFile = { slot, file ->
                                    viewModel.attachDocumentFromCameraFile(slot, file)
                                },
                                onRemoveDoc = viewModel::removeDocument,
                                onSaveDraft = {
                                    viewModel.saveAsPendingDraft(
                                        onSuccess = {
                                            Toast.makeText(context, "Pending draft saved offline", Toast.LENGTH_SHORT).show()
                                            navController.popBackStack()
                                        },
                                        onValidationError = {
                                            Toast.makeText(context, strings.validationBannerTitle, Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                },
                                onReviewAndFinalize = {
                                    if (viewModel.validateAndProceedToReview()) {
                                        navController.navigate(AppRoutes.FORM_REVIEW)
                                    } else {
                                        Toast.makeText(context, strings.validationBannerTitle, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(AppRoutes.FORM_REVIEW) {
                            FormReviewScreen(
                                record = formState.record,
                                isSaving = formState.isSaving,
                                onBackToEdit = { navController.popBackStack() },
                                onConfirmSave = { onSavedId ->
                                    viewModel.confirmAndSaveCompleted(onSavedId)
                                },
                                onOpenPrintPreview = { savedId ->
                                    navController.navigate(AppRoutes.printPreview(savedId)) {
                                        popUpTo(AppRoutes.DASHBOARD)
                                    }
                                },
                                onGoDashboard = {
                                    navController.popBackStack(AppRoutes.DASHBOARD, inclusive = false)
                                }
                            )
                        }

                        composable(
                            route = AppRoutes.PRINT_PREVIEW,
                            arguments = listOf(navArgument("studentId") { type = NavType.LongType })
                        ) { entry ->
                            val studentId = entry.arguments?.getLong("studentId") ?: 0L
                            PrintPreviewScreen(
                                studentId = studentId,
                                loadStudent = viewModel::getStudentForPreview,
                                onEditStudent = { id ->
                                    viewModel.loadStudentForEdit(id)
                                    navController.navigate(AppRoutes.STUDENT_FORM)
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
