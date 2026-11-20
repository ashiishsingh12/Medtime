package com.example

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.MedicationRepository
import com.example.model.Medicine
import com.example.model.TimeSlot
import com.example.notifications.MedicationNotificationHelper
import com.example.notifications.MedicationReminderScheduler
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.MedicineEditorSheet
import com.example.ui.screens.MedicinesScreen
import com.example.ui.screens.SettingsDialog
import com.example.ui.screens.TodayScreen
import com.example.ui.theme.MedTimeTheme
import com.example.ui.viewmodel.MedTimeViewModel
import com.example.ui.viewmodel.UiState
import kotlinx.coroutines.launch

const val ROUTE_TODAY = "today"
const val ROUTE_TIMETABLE = "timetable"
const val ROUTE_HISTORY = "history"

private data class NavDestinationItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val TopDestinations = listOf(
    NavDestinationItem(
        route = ROUTE_TODAY,
        labelRes = R.string.nav_today,
        selectedIcon = Icons.Filled.Today,
        unselectedIcon = Icons.Outlined.Today,
        testTag = "nav_tab_today"
    ),
    NavDestinationItem(
        route = ROUTE_TIMETABLE,
        labelRes = R.string.nav_medicines,
        selectedIcon = Icons.Filled.CalendarMonth,
        unselectedIcon = Icons.Outlined.CalendarMonth,
        testTag = "nav_tab_timetable"
    ),
    NavDestinationItem(
        route = ROUTE_HISTORY,
        labelRes = R.string.nav_history,
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History,
        testTag = "nav_tab_history"
    )
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        MedicationNotificationHelper.ensureChannel(this)
        setContent {
            MedTimeTheme {
                MedTimeMainApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedTimeMainApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val viewModel: MedTimeViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                MedTimeViewModel(
                    repository = MedicationRepository(app),
                    onAlarmsSync = { meds ->
                        MedicationReminderScheduler.syncAllAlarms(app, meds)
                    }
                )
            }
        }
    )

    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    var hasNotificationPermission by remember {
        mutableStateOf(MedicationNotificationHelper.hasNotificationPermission(context))
    }
    var canScheduleExactAlarms by remember {
        mutableStateOf(MedicationReminderScheduler.canScheduleExactAlarms(context))
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotificationPermission = MedicationNotificationHelper.hasNotificationPermission(context)
                canScheduleExactAlarms = MedicationReminderScheduler.canScheduleExactAlarms(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
        scope.launch {
            snackbarHostState.showSnackbar(
                if (granted) "Medicine alert notifications enabled!"
                else "Notifications are off. Enable them in Settings to receive dose alerts."
            )
        }
    }

    val requestNotificationPermission: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            hasNotificationPermission = true
        }
    }

    val requestExactAlarmPermission: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            try {
                context.startActivity(intent)
            } catch (_: Exception) {
                scope.launch {
                    snackbarHostState.showSnackbar("Open device settings to allow exact alarms.")
                }
            }
        }
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_TODAY

    var showEditorSheet by remember { mutableStateOf(false) }
    var editingMedicine by remember { mutableStateOf<Medicine?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val navigateToTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Medication,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showSettingsDialog = true },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("open_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Tune,
                                contentDescription = "Profile and default reminder times",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        modifier = Modifier.navigationBarsPadding()
                    ) {
                        TopDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (currentRoute != ROUTE_HISTORY) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            editingMedicine = null
                            showEditorSheet = true
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null
                            )
                        },
                        text = { Text("Add Medicine") },
                        shape = RoundedCornerShape(16.dp),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("fab_add_medicine")
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        Spacer(modifier = Modifier.size(12.dp))
                        TopDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(modifier = Modifier.widthIn(max = 760.dp).fillMaxSize()) {
                        when (val state = dashboardState) {
                            is UiState.Loading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator()
                                        Spacer(modifier = Modifier.size(12.dp))
                                        Text(
                                            text = "Loading your medicine timetable...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            is UiState.Error -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = "Unable to load timetable",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            text = state.message,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            is UiState.Success -> {
                                val data = state.data
                                NavHost(
                                    navController = navController,
                                    startDestination = ROUTE_TODAY,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    composable(ROUTE_TODAY) {
                                        TodayScreen(
                                            state = data,
                                            hasNotificationPermission = hasNotificationPermission,
                                            canScheduleExactAlarms = canScheduleExactAlarms,
                                            onRequestNotificationPermission = requestNotificationPermission,
                                            onRequestExactAlarmPermission = requestExactAlarmPermission,
                                            onSelectFilter = viewModel::setSlotFilter,
                                            onToggleDoseTaken = viewModel::toggleDoseTaken,
                                            onMarkDoseSkipped = viewModel::markDoseSkipped,
                                            onEditMedicine = { med ->
                                                editingMedicine = med
                                                showEditorSheet = true
                                            }
                                        )
                                    }
                                    composable(ROUTE_TIMETABLE) {
                                        MedicinesScreen(
                                            medicines = data.medicines,
                                            onAddMedicine = {
                                                editingMedicine = null
                                                showEditorSheet = true
                                            },
                                            onEditMedicine = { med ->
                                                editingMedicine = med
                                                showEditorSheet = true
                                            },
                                            onToggleActive = viewModel::toggleMedicineActive,
                                            onDeleteMedicine = viewModel::deleteMedicine,
                                            onTestNotification = { med ->
                                                if (!MedicationNotificationHelper.hasNotificationPermission(context)) {
                                                    requestNotificationPermission()
                                                } else {
                                                    val previewSlot = med.enabledSlots().firstOrNull() ?: TimeSlot.MORNING
                                                    val shown = MedicationNotificationHelper.showReminderNotification(
                                                        context = context,
                                                        medicineId = med.id,
                                                        medicineName = med.name,
                                                        dosage = med.dosage,
                                                        mealRelation = med.mealRelationFor(previewSlot),
                                                        slot = previewSlot,
                                                        hour = med.hourFor(previewSlot),
                                                        minute = med.minuteFor(previewSlot),
                                                        isTestPreview = true
                                                    )
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar(
                                                            if (shown) "Sent test reminder alert for ${med.name}"
                                                            else "Enable notification permission to preview alerts"
                                                        )
                                                    }
                                                }
                                            }
                                        )
                                    }
                                    composable(ROUTE_HISTORY) {
                                        HistoryScreen(
                                            weeklySummary = data.weeklySummary,
                                            doseLogs = data.doseLogs
                                        )
                                    }
                                }

                                if (showEditorSheet) {
                                    MedicineEditorSheet(
                                        existingMedicine = editingMedicine,
                                        userProfile = data.userProfile,
                                        isSaving = isSaving,
                                        onDismiss = {
                                            showEditorSheet = false
                                            editingMedicine = null
                                        },
                                        onSave = { id, input ->
                                            viewModel.saveMedicine(id, input) {
                                                showEditorSheet = false
                                                editingMedicine = null
                                                if (!hasNotificationPermission) {
                                                    requestNotificationPermission()
                                                }
                                            }
                                        }
                                    )
                                }

                                if (showSettingsDialog) {
                                    SettingsDialog(
                                        userProfile = data.userProfile,
                                        hasNotificationPermission = hasNotificationPermission,
                                        canScheduleExactAlarms = canScheduleExactAlarms,
                                        isSaving = isSaving,
                                        onDismiss = { showSettingsDialog = false },
                                        onSaveSettings = { name, mH, mM, aH, aM, eH, eM, nH, nM ->
                                            viewModel.updateProfileAndDefaults(
                                                displayName = name,
                                                morningHour = mH,
                                                morningMinute = mM,
                                                afternoonHour = aH,
                                                afternoonMinute = aM,
                                                eveningHour = eH,
                                                eveningMinute = eM,
                                                nightHour = nH,
                                                nightMinute = nM
                                            ) {
                                                showSettingsDialog = false
                                            }
                                        },
                                        onRequestNotificationPermission = requestNotificationPermission,
                                        onRequestExactAlarmPermission = requestExactAlarmPermission
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
