package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material3.AlertDialog
import com.example.R
import com.example.model.AdminIdentity
import com.example.model.GenderOption
import com.example.model.RegistrationInput
import com.example.model.formatDateKeyDmy
import com.example.model.utcMillisToDateKey

@Composable
fun LoginScreen(
    isBusy: Boolean,
    hasAdminAccount: Boolean,
    errorMessage: String?,
    onClearError: () -> Unit,
    onLogin: (identifier: String, password: String) -> Unit,
    onClaimAdmin: (email: String, phone: String, password: String) -> Unit,
    onNavigateToSignUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    var userIdInput by rememberSaveable { mutableStateOf("") }
    var passwordInput by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var showAdminClaimDialog by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current

    val displayError = localError ?: errorMessage

    if (showAdminClaimDialog && !hasAdminAccount) {
        AdminFirstSetupDialog(
            initialPassword = passwordInput,
            isBusy = isBusy,
            errorMessage = errorMessage,
            onDismiss = {
                onClearError()
                showAdminClaimDialog = false
            },
            onConfirmClaim = { adminPassword ->
                onClaimAdmin(
                    AdminIdentity.PRIMARY_ADMIN_EMAIL,
                    AdminIdentity.ADMIN_PHONE,
                    adminPassword
                )
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    tonalElevation = 2.dp,
                    modifier = Modifier.size(84.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_medtime_foreground),
                        contentDescription = "MedTym App Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(24.dp))
                    )
                }

                Text(
                    text = "MedTym",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = userIdInput,
                    onValueChange = {
                        userIdInput = it
                        localError = null
                        onClearError()
                    },
                    label = { Text("Email ID or Phone No") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_id_input")
                )

                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = {
                        passwordInput = it
                        localError = null
                        onClearError()
                    },
                    label = { Text("Password") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = { passwordVisible = !passwordVisible },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            if (!hasAdminAccount && AdminIdentity.isDesignatedAdminIdentifier(userIdInput)) {
                                localError = null
                                onClearError()
                                showAdminClaimDialog = true
                            } else if (userIdInput.isBlank() || passwordInput.isBlank()) {
                                localError = "Please enter your ID and password."
                            } else {
                                onLogin(userIdInput, passwordInput)
                            }
                        }
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_password_input")
                )

                if (!displayError.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = displayError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        if (!hasAdminAccount && AdminIdentity.isDesignatedAdminIdentifier(userIdInput)) {
                            localError = null
                            onClearError()
                            showAdminClaimDialog = true
                        } else if (userIdInput.isBlank() || passwordInput.isBlank()) {
                            localError = "Please enter your ID and password."
                        } else {
                            onLogin(userIdInput, passwordInput)
                        }
                    },
                    enabled = !isBusy,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("login_submit_button")
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            text = "Log In",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        localError = null
                        onClearError()
                        onNavigateToSignUp()
                    },
                    enabled = !isBusy,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .testTag("go_to_signup_button")
                ) {
                    Text(
                        text = "Sign Up",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (!hasAdminAccount) {
                    TextButton(
                        onClick = {
                            localError = null
                            onClearError()
                            showAdminClaimDialog = true
                        },
                        enabled = !isBusy,
                        modifier = Modifier.testTag("admin_first_setup_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AdminPanelSettings,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Admin First-Time Setup",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminFirstSetupDialog(
    initialPassword: String,
    isBusy: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirmClaim: (password: String) -> Unit
) {
    var password by remember { mutableStateOf(initialPassword) }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    val displayError = localError ?: errorMessage

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.AdminPanelSettings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Activate Admin Access")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Admin Account",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = AdminIdentity.PRIMARY_ADMIN_EMAIL,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Phone: ${AdminIdentity.ADMIN_PHONE}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = "Set your admin password to activate one-time admin login.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        localError = null
                    },
                    label = { Text("Create Admin Password") },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = "Toggle password visibility"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_claim_password_input")
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        localError = null
                    },
                    label = { Text("Confirm Password") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_claim_confirm_input")
                )
                if (!displayError.isNullOrBlank()) {
                    Text(
                        text = displayError,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        password.length < 6 -> localError = "Password must be at least 6 characters."
                        password != confirmPassword -> localError = "Passwords do not match."
                        else -> onConfirmClaim(password)
                    }
                },
                enabled = !isBusy,
                modifier = Modifier.testTag("admin_claim_submit_button")
            ) {
                Text(if (isBusy) "Activating..." else "Activate Admin")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isBusy) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(
    isBusy: Boolean,
    errorMessage: String?,
    onClearError: () -> Unit,
    onRegister: (RegistrationInput) -> Unit,
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = !isBusy) {
        onBackToLogin()
    }

    var firstName by rememberSaveable { mutableStateOf("") }
    var lastName by rememberSaveable { mutableStateOf("") }
    var gender by rememberSaveable { mutableStateOf(GenderOption.MALE.displayName) }
    var dob by rememberSaveable { mutableStateOf("") }
    var state by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var showDobPicker by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current
    val displayError = localError ?: errorMessage

    if (showDobPicker) {
        DobDatePickerDialog(
            onDismiss = { showDobPicker = false },
            onConfirm = { pickedDmy ->
                dob = pickedDmy
                localError = null
                onClearError()
                showDobPicker = false
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Create Account",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackToLogin,
                        enabled = !isBusy,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("register_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Login"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = {
                            if (it.length <= 40) firstName = it
                            localError = null
                            onClearError()
                        },
                        label = { Text("First Name") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("register_first_name")
                    )
                    OutlinedTextField(
                        value = lastName,
                        onValueChange = {
                            if (it.length <= 40) lastName = it
                            localError = null
                            onClearError()
                        },
                        label = { Text("Last Name") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("register_last_name")
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Gender",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GenderOption.entries.forEach { option ->
                            val selected = gender == option.displayName
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    gender = option.displayName
                                    localError = null
                                    onClearError()
                                },
                                label = { Text(option.displayName) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("register_gender_${option.name.lowercase()}")
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = dob,
                    onValueChange = {
                        if (it.length <= 10) dob = it
                        localError = null
                        onClearError()
                    },
                    label = { Text("DOB (dd-mm-yyyy)") },
                    placeholder = { Text("25-11-1995") },
                    trailingIcon = {
                        IconButton(
                            onClick = { showDobPicker = true },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("register_dob_calendar_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DateRange,
                                contentDescription = "Pick Date of Birth",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("register_dob")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = state,
                        onValueChange = {
                            if (it.length <= 50) state = it
                            localError = null
                            onClearError()
                        },
                        label = { Text("State") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("register_state")
                    )
                    OutlinedTextField(
                        value = city,
                        onValueChange = {
                            if (it.length <= 50) city = it
                            localError = null
                            onClearError()
                        },
                        label = { Text("City") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("register_city")
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        if (it.length <= 100) email = it
                        localError = null
                        onClearError()
                    },
                    label = { Text("Email ID") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Email,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("register_email")
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        if (it.length <= 15) phone = it
                        localError = null
                        onClearError()
                    },
                    label = { Text("Phone No") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Phone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("register_phone")
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        if (it.length <= 64) password = it
                        localError = null
                        onClearError()
                    },
                    label = { Text("Password (min 6 characters)") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = { passwordVisible = !passwordVisible },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("register_password")
                )

                if (!displayError.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = displayError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        onRegister(
                            RegistrationInput(
                                firstName = firstName,
                                lastName = lastName,
                                gender = gender,
                                dob = dob,
                                state = state,
                                city = city,
                                email = email,
                                phone = phone,
                                password = password
                            )
                        )
                    },
                    enabled = !isBusy,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("register_submit_button")
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            text = "Sign Up",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Already have an account?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(
                        onClick = onBackToLogin,
                        enabled = !isBusy,
                        modifier = Modifier.testTag("back_to_login_button")
                    ) {
                        Text("Log In", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DobDatePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (dobDmy: String) -> Unit
) {
    val nowMillis = remember { System.currentTimeMillis() }
    val pickerState = rememberDatePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= nowMillis
        }
    )
    val dmyFormatter = remember {
        DatePickerDefaults.dateFormatter(selectedDateSkeleton = "dd-MM-yyyy")
    }
    val selectedDmy = pickerState.selectedDateMillis?.let { formatDateKeyDmy(utcMillisToDateKey(it)) }

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { if (selectedDmy != null) onConfirm(selectedDmy) },
                enabled = selectedDmy != null
            ) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(
            state = pickerState,
            dateFormatter = dmyFormatter,
            showModeToggle = false,
            title = {
                Text(
                    text = "Select Date of Birth",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)
                )
            },
            headline = {
                Text(
                    text = selectedDmy ?: "dd-mm-yyyy",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, bottom = 12.dp)
                )
            }
        )
    }
}
