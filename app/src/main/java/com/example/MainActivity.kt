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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.data.auth.AuthSessionManager
import com.example.model.FamilyMember
import com.example.model.Medicine
import com.example.model.TimeSlot
import com.example.notifications.MedicationNotificationHelper
import com.example.notifications.MedicationReminderScheduler
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MedicineEditorSheet
import com.example.ui.screens.MedicinesScreen
import com.example.ui.screens.RegistrationScreen
import com.example.ui.screens.SettingsDialog
import com.example.ui.screens.TodayScreen
import com.example.ui.theme.MedTimeTheme
import com.example.ui.viewmodel.AuthUiState
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
                    sessionManager = AuthSessionManager(app),
                    onAlarmsSync = { meds, memberNames ->
                        MedicationReminderScheduler.syncAllAlarms(app, meds, memberNames)
                    }
                )
            }
        }
    )

    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()
    val isAuthBusy by viewModel.isAuthBusy.collectAsStateWithLifecycle()
    val hasAdminAccount by viewModel.hasAdminAccount.collectAsStateWithLifecycle()
    val adminUsers by viewModel.adminUsers.collectAsStateWithLifecycle()
    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    var showRegisterScreen by remember { mutableStateOf(false) }

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
    var profileMenuExpanded by remember { mutableStateOf(false) }
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var editingMember by remember { mutableStateOf<FamilyMember?>(null) }
    var deletingMember by remember { mutableStateOf<FamilyMember?>(null) }

    val navigateToTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    when (authState) {
        is AuthUiState.Checking -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return
        }

        is AuthUiState.Unauthenticated -> {
            if (showRegisterScreen) {
                RegistrationScreen(
                    isBusy = isAuthBusy,
                    errorMessage = authError,
                    onClearError = viewModel::clearAuthError,
                    onRegister = viewModel::register,
                    onBackToLogin = {
                        viewModel.clearAuthError()
                        showRegisterScreen = false
                    }
                )
            } else {
                LoginScreen(
                    isBusy = isAuthBusy,
                    hasAdminAccount = hasAdminAccount,
                    errorMessage = authError,
                    onClearError = viewModel::clearAuthError,
                    onLogin = viewModel::login,
                    onClaimAdmin = viewModel::claimAdminAccount,
                    onNavigateToSignUp = {
                        viewModel.clearAuthError()
                        showRegisterScreen = true
                    }
                )
            }
            return
        }

        is AuthUiState.Authenticated -> {
            LaunchedEffect(Unit) {
                showRegisterScreen = false
            }
        }
    }

    val isCurrentUserAdmin = (authState as? AuthUiState.Authenticated)?.user?.isAdmin == true

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
                            if (isCurrentUserAdmin) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("topbar_admin_badge")
                                ) {
                                    Text(
                                        text = "ADMIN",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        val successData = (dashboardState as? UiState.Success)?.data
                        if (successData != null) {
                            val activeLabel = if (successData.selectedMemberId == FamilyMember.ALL_ID) {
                                "All Family"
                            } else {
                                successData.selectedMember?.name ?: "Self"
                            }
                            Box(modifier = Modifier.padding(end = 4.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .clickable { profileMenuExpanded = true }
                                        .minimumInteractiveComponentSize()
                                        .testTag("profile_switcher_dropdown")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (successData.selectedMemberId == FamilyMember.ALL_ID) {
                                                Icons.Filled.Groups
                                            } else {
                                                Icons.Filled.Person
                                            },
                                            contentDescription = "Switch patient profile",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = activeLabel,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.widthIn(max = 108.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Icon(
                                            imageVector = Icons.Filled.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = profileMenuExpanded,
                                    onDismissRequest = { profileMenuExpanded = false }
                                ) {
                                    successData.familyMembers.forEach { member ->
                                        val isSelected = successData.selectedMemberId == member.id
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = member.name,
                                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Filled.Person,
                                                    contentDescription = null,
                                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            },
                                            trailingIcon = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    if (isSelected) {
                                                        Icon(
                                                            imageVector = Icons.Filled.Check,
                                                            contentDescription = "Selected",
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            profileMenuExpanded = false
                                                            editingMember = member
                                                        },
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .testTag("edit_member_${member.id}")
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Filled.Edit,
                                                            contentDescription = "Edit ${member.name}",
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                    if (successData.familyMembers.size > 1) {
                                                        IconButton(
                                                            onClick = {
                                                                profileMenuExpanded = false
                                                                deletingMember = member
                                                            },
                                                            modifier = Modifier
                                                                .size(32.dp)
                                                                .testTag("delete_member_${member.id}")
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Filled.DeleteOutline,
                                                                contentDescription = "Remove ${member.name}",
                                                                tint = MaterialTheme.colorScheme.error,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            },
                                            onClick = {
                                                viewModel.selectMember(member.id)
                                                profileMenuExpanded = false
                                            },
                                            modifier = Modifier.testTag("profile_option_${member.id}")
                                        )
                                    }
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                    )
                                    val isAllSelected = successData.selectedMemberId == FamilyMember.ALL_ID
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "All Family",
                                                fontWeight = if (isAllSelected) FontWeight.SemiBold else FontWeight.Normal
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Filled.Groups,
                                                contentDescription = null,
                                                tint = if (isAllSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        trailingIcon = if (isAllSelected) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Filled.Check,
                                                    contentDescription = "Selected",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        } else null,
                                        onClick = {
                                            viewModel.selectMember(FamilyMember.ALL_ID)
                                            profileMenuExpanded = false
                                        },
                                        modifier = Modifier.testTag("profile_option_all_family")
                                    )
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "Add Family Member",
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Medium
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Filled.PersonAdd,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            profileMenuExpanded = false
                                            showAddMemberDialog = true
                                        },
                                        modifier = Modifier.testTag("profile_option_add_member")
                                    )
                                }
                            }
                        }

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
                                            onSelectFilter = viewModel::setDoseFilter,
                                            onToggleDoseTaken = viewModel::toggleDoseTaken,
                                            onMarkDoseSkipped = viewModel::markDoseSkipped,
                                            onEditMedicine = { med ->
                                                editingMedicine = med
                                                showEditorSheet = true
                                            }
                                        )
                                    }
                                    composable(ROUTE_TIMETABLE) {
                                        val isAllMembers = data.selectedMemberId == FamilyMember.ALL_ID
                                        MedicinesScreen(
                                            medicines = data.medicines,
                                            memberNamesById = data.memberNamesById,
                                            showMemberBadge = isAllMembers,
                                            activeMemberLabel = if (isAllMembers) "All Family" else (data.selectedMember?.name ?: "Self"),
                                            isSaving = isSaving,
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
                                            onUpdateRecurrence = { med, rule ->
                                                viewModel.updateMedicineRecurrence(med.id, rule)
                                            },
                                            onTestNotification = { med ->
                                                if (!MedicationNotificationHelper.hasNotificationPermission(context)) {
                                                    requestNotificationPermission()
                                                } else {
                                                    val previewSlot = med.enabledSlots().firstOrNull() ?: TimeSlot.MORNING
                                                    val memberName = data.memberNamesById[med.memberId] ?: "Self"
                                                    val shown = MedicationNotificationHelper.showReminderNotification(
                                                        context = context,
                                                        medicineId = med.id,
                                                        medicineName = med.name,
                                                        dosage = med.dosage,
                                                        mealRelation = med.mealRelationFor(previewSlot),
                                                        slot = previewSlot,
                                                        hour = med.hourFor(previewSlot),
                                                        minute = med.minuteFor(previewSlot),
                                                        isTestPreview = true,
                                                        memberName = memberName
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
                                            doseLogs = data.doseLogs,
                                            showMemberBadge = data.selectedMemberId == FamilyMember.ALL_ID
                                        )
                                    }
                                }

                                if (showEditorSheet) {
                                    val defaultEditorMemberId = if (data.selectedMemberId == FamilyMember.ALL_ID) {
                                        FamilyMember.SELF_ID
                                    } else {
                                        data.selectedMemberId
                                    }
                                    MedicineEditorSheet(
                                        existingMedicine = editingMedicine,
                                        userProfile = data.userProfile,
                                        familyMembers = data.familyMembers,
                                        defaultMemberId = defaultEditorMemberId,
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

                                if (showAddMemberDialog) {
                                    AddFamilyMemberDialog(
                                        isSaving = isSaving,
                                        onDismiss = { showAddMemberDialog = false },
                                        onAdd = { memberName, relation ->
                                            viewModel.addFamilyMember(memberName, relation) {
                                                showAddMemberDialog = false
                                            }
                                        }
                                    )
                                }

                                editingMember?.let { memberToEdit ->
                                    EditFamilyMemberDialog(
                                        member = memberToEdit,
                                        isSaving = isSaving,
                                        onDismiss = { editingMember = null },
                                        onSave = { updatedName ->
                                            viewModel.renameFamilyMember(memberToEdit.id, updatedName) {
                                                editingMember = null
                                            }
                                        }
                                    )
                                }

                                deletingMember?.let { memberToDelete ->
                                    AlertDialog(
                                        onDismissRequest = { deletingMember = null },
                                        title = { Text("Remove ${memberToDelete.name}?") },
                                        text = {
                                            Text(
                                                "All medicines, alert times, and dose history saved under ${memberToDelete.name} will also be permanently deleted."
                                            )
                                        },
                                        confirmButton = {
                                            TextButton(
                                                onClick = {
                                                    viewModel.deleteFamilyMember(memberToDelete.id) {
                                                        deletingMember = null
                                                    }
                                                },
                                                modifier = Modifier.testTag("confirm_delete_member_button")
                                            ) {
                                                Text("Remove", color = MaterialTheme.colorScheme.error)
                                            }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { deletingMember = null }) {
                                                Text("Cancel")
                                            }
                                        }
                                    )
                                }

                                if (showSettingsDialog) {
                                    SettingsDialog(
                                        userProfile = data.userProfile,
                                        loggedInUser = data.loggedInUser,
                                        adminUsers = adminUsers,
                                        sessionExpiresAtMillis = data.sessionExpiresAtMillis,
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
                                        onRequestExactAlarmPermission = requestExactAlarmPermission,
                                        onUpdateUserRole = viewModel::updateUserRole,
                                        onDeleteUser = viewModel::deleteUserAccount,
                                        onLogout = {
                                            showSettingsDialog = false
                                            showRegisterScreen = false
                                            viewModel.logout()
                                        }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddFamilyMemberDialog(
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onAdd: (name: String, relation: String) -> Unit
) {
    var memberName by remember { mutableStateOf("") }
    var selectedRelation by remember { mutableStateOf("Family") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val quickPresets = listOf("Child", "Spouse", "Grandma", "Grandpa", "Brother", "Sister")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Family Member") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Track and schedule medicines separately for another family member.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = memberName,
                    onValueChange = {
                        if (it.length <= 24) memberName = it
                        errorMessage = null
                    },
                    label = { Text("Member Name") },
                    placeholder = { Text("e.g., Aarav, Grandma, Papa") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_member_name_input")
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickPresets.forEach { preset ->
                        FilterChip(
                            selected = memberName.equals(preset, ignoreCase = true),
                            onClick = {
                                memberName = preset
                                selectedRelation = preset
                                errorMessage = null
                            },
                            label = { Text(preset) }
                        )
                    }
                }
                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = memberName.trim()
                    if (trimmed.isEmpty()) {
                        errorMessage = "Please enter a name."
                    } else {
                        onAdd(trimmed, selectedRelation)
                    }
                },
                enabled = !isSaving,
                modifier = Modifier.testTag("confirm_add_member_button")
            ) {
                Text(if (isSaving) "Adding..." else "Add Member")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EditFamilyMemberDialog(
    member: FamilyMember,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val initialInput = if (member.id == FamilyMember.SELF_ID && member.name.equals("Self", ignoreCase = true)) {
        ""
    } else {
        member.name
    }
    var memberName by remember(member) { mutableStateOf(initialInput) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (member.id == FamilyMember.SELF_ID) "Edit Your Name"
                else "Edit ${member.name}"
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (member.id == FamilyMember.SELF_ID) {
                        "Set your name so your daily greeting feels personal (e.g., Good Evening, Khushi)."
                    } else {
                        "Update this family member's display name across schedules, alerts, and history."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = memberName,
                    onValueChange = {
                        if (it.length <= 24) memberName = it
                        errorMessage = null
                    },
                    label = { Text("Display Name") },
                    placeholder = {
                        Text(
                            if (member.id == FamilyMember.SELF_ID) "e.g., Ashish, Khushi"
                            else "e.g., Mummy, Papa, Aarav"
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_member_name_input")
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = memberName.trim()
                    if (trimmed.isEmpty()) {
                        errorMessage = "Please enter a name."
                    } else {
                        onSave(trimmed)
                    }
                },
                enabled = !isSaving,
                modifier = Modifier.testTag("confirm_edit_member_button")
            ) {
                Text(if (isSaving) "Saving..." else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


