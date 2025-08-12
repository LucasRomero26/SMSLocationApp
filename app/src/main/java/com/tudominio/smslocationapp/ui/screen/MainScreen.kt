package com.tudominio.smslocation.ui.screen

import android.Manifest
import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.tudominio.smslocation.R
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.tudominio.smslocation.viewmodel.MainViewModel

// Custom color palette
private val PrimaryBlue = Color(0xFF93949c)
private val SecondaryBlue = Color(0xFF93949c)
private val DarkBlue = Color(0xFF93949c)
private val MediumBlue = Color(0xFF93949c)
private val LightBlue = Color(0xFF93949c)
private val AccentBlue = Color(0xFF93949c)
private val BrightBlue = Color(0xFF93949c)
private val DeepBlue = Color(0xFF93949c)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState = viewModel.uiState
    val focusManager = LocalFocusManager.current

    // Permission management
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.SEND_SMS
        )
    )

    // Initialize location client
    LaunchedEffect(Unit) {
        viewModel.initializeLocationClient(context)
    }

    // Handle permission changes
    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted) {
            viewModel.onPermissionsGranted(context)
        }
    }

    // Auto-hide location after SMS is sent
    LaunchedEffect(uiState.lastMessage) {
        if (uiState.lastMessage.isNotEmpty() && uiState.currentLocation != null) {
            kotlinx.coroutines.delay(5000) // Wait 5 seconds
            viewModel.clearLocationAndResetButton()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Background gradient circles for visual appeal
        BackgroundDecorations()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Modern header
            ModernHeaderSection()

            Spacer(modifier = Modifier.height(32.dp))

            // Main interaction card
            ModernMainCard(
                uiState = uiState,
                viewModel = viewModel,
                permissionsState = permissionsState
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BackgroundDecorations() {
    Box(modifier = Modifier.fillMaxSize()) {
        // Top right gradient circle
        Box(
            modifier = Modifier
                .size(200.dp)
                .offset(x = 150.dp, y = (-50).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            BrightBlue.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Bottom left gradient circle
        Box(
            modifier = Modifier
                .size(150.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-30).dp, y = 50.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            PrimaryBlue.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
    }
}

@Composable
private fun ModernHeaderSection() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Image(
            painter = painterResource(id = R.drawable.location_icon),
            contentDescription = null,
            modifier = Modifier.size(120.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Welcome to SMS Juls",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp
            ),
            color = DarkBlue
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Share your location instantly via SMS",
            style = MaterialTheme.typography.bodyLarge,
            color = DarkBlue.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun ModernMainCard(
    uiState: com.tudominio.smslocation.viewmodel.UiState,
    viewModel: MainViewModel,
    permissionsState: MultiplePermissionsState
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(28.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Modern phone number input
            ModernPhoneNumberInput(
                phoneNumber = uiState.phoneNumber,
                isValid = uiState.isPhoneNumberValid,
                onPhoneNumberChange = viewModel::updatePhoneNumber,
                enabled = !uiState.isSendingSMS,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Modern permissions status
            ModernPermissionsStatus(
                hasLocationPermission = uiState.hasLocationPermission,
                hasSmsPermission = uiState.hasSmsPermission,
                onRequestPermissions = {
                    permissionsState.launchMultiplePermissionRequest()
                }
            )

            // SMS process animation
            AnimatedVisibility(
                visible = uiState.isSendingSMS,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(400, easing = EaseOutCubic)
                ) + fadeIn(animationSpec = tween(400)),
                exit = slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            ) {
                ModernSMSProcessCard(
                    isLoadingLocation = uiState.isLoadingLocation,
                    isSendingSMS = uiState.isSendingSMS,
                    modifier = Modifier.padding(top = 24.dp)
                )
            }

            // Location display
            AnimatedVisibility(
                visible = uiState.currentLocation != null && !uiState.isSendingSMS,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                uiState.currentLocation?.let { location ->
                    ModernLocationDisplay(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        modifier = Modifier.padding(top = 24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Modern send button
            ModernSendButton(
                enabled = uiState.isPhoneNumberValid &&
                        uiState.hasLocationPermission &&
                        uiState.hasSmsPermission &&
                        !uiState.isSendingSMS,
                isLoading = uiState.isSendingSMS,
                onSendSMS = { viewModel.sendSMSWithLocation(context) }
            )
        }
    }
}

@Composable
private fun ModernPhoneNumberInput(
    phoneNumber: String,
    isValid: Boolean,
    onPhoneNumberChange: (String) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Column(modifier = modifier) {
        Text(
            text = "Phone Number",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = DarkBlue,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = onPhoneNumberChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            placeholder = {
                Text(
                    "3012345678",
                    color = MediumBlue.copy(alpha = 0.5f)
                )
            },
            leadingIcon = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 16.dp)
                ) {
                    Text(
                        text = "+57",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MediumBlue
                    )
                    Divider(
                        modifier = Modifier
                            .height(24.dp)
                            .width(1.dp)
                            .padding(horizontal = 12.dp),
                        color = MediumBlue.copy(alpha = 0.3f)
                    )
                }
            },
            trailingIcon = {
                if (phoneNumber.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = if (isValid) LightBlue.copy(alpha = 0.2f)
                                else Color.Red.copy(alpha = 0.1f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isValid) Icons.Default.Check else Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (isValid) LightBlue else Color.Red
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,  // Cambio: Number en lugar de Phone
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                }
            ),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (isValid && phoneNumber.isNotEmpty()) LightBlue else PrimaryBlue,
                unfocusedBorderColor = MediumBlue.copy(alpha = 0.3f),
                focusedTextColor = DarkBlue,
                unfocusedTextColor = DarkBlue,
                disabledBorderColor = MediumBlue.copy(alpha = 0.2f),
                disabledTextColor = DarkBlue.copy(alpha = 0.6f)
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = when {
                phoneNumber.isEmpty() -> "Enter Colombian mobile number"
                isValid -> "Valid number"
                else -> "Must be Colombian mobile\n(10 digits, starts with 3)"
            },
            style = MaterialTheme.typography.bodySmall,
            color = when {
                phoneNumber.isEmpty() -> MediumBlue.copy(alpha = 0.7f)
                isValid -> LightBlue
                else -> Color.Red
            },
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

@Composable
private fun ModernPermissionsStatus(
    hasLocationPermission: Boolean,
    hasSmsPermission: Boolean,
    onRequestPermissions: () -> Unit
) {
    val allPermissionsGranted = hasLocationPermission && hasSmsPermission

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (allPermissionsGranted)
                LightBlue.copy(alpha = 0.1f)
            else
                Color.Red.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = if (allPermissionsGranted) LightBlue else Color.Red,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (allPermissionsGranted) Icons.Default.Shield else Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (allPermissionsGranted) "All Set!" else "Permissions Needed",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = if (allPermissionsGranted) LightBlue else Color.Red
                )

                Text(
                    text = if (allPermissionsGranted)
                        "GPS and SMS permissions granted"
                    else
                        "Location and SMS access required",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (allPermissionsGranted)
                        MediumBlue.copy(alpha = 0.8f)
                    else
                        Color.Red.copy(alpha = 0.8f)
                )
            }

            if (!allPermissionsGranted) {
                Button(
                    onClick = onRequestPermissions,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(40.dp)
                ) {
                    Text("Grant", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun ModernSMSProcessCard(
    isLoadingLocation: Boolean,
    isSendingSMS: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = PrimaryBlue.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Animated loading indicator
            val infiniteTransition = rememberInfiniteTransition(label = "loading")
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rotation"
            )

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(PrimaryBlue, BrightBlue)
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isLoadingLocation -> Icons.Default.MyLocation
                        isSendingSMS -> Icons.Default.Send
                        else -> Icons.Default.Refresh
                    },
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = when {
                    isLoadingLocation -> "Getting Location"
                    isSendingSMS -> "Sending Message"
                    else -> "Processing"
                },
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = DarkBlue,
                textAlign = TextAlign.Left
            )

            Text(
                text = when {
                    isLoadingLocation -> "Acquiring GPS coordinates..."
                    isSendingSMS -> "Sending SMS with location data"
                    else -> "Preparing to send..."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MediumBlue.copy(alpha = 0.8f),
                textAlign = TextAlign.Left
            )
        }
    }
}

@Composable
private fun ModernLocationDisplay(
    latitude: Double,
    longitude: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = LightBlue.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(LightBlue, BrightBlue)
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Current Location",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = DarkBlue
                    )
                    Text(
                        text = "GPS coordinates acquired",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MediumBlue.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Location coordinates
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Latitude",
                        style = MaterialTheme.typography.bodySmall,
                        color = MediumBlue.copy(alpha = 0.7f)
                    )
                    Text(
                        text = String.format("%.6f", latitude),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = DarkBlue
                    )
                }
                Column {
                    Text(
                        text = "Longitude",
                        style = MaterialTheme.typography.bodySmall,
                        color = MediumBlue.copy(alpha = 0.7f)
                    )
                    Text(
                        text = String.format("%.6f", longitude),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = DarkBlue
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernSendButton(
    enabled: Boolean,
    isLoading: Boolean,
    onSendSMS: () -> Unit
) {
    val focusManager = LocalFocusManager.current  // Agregar para ocultar teclado

    Button(
        onClick = {
            focusManager.clearFocus()  // Ocultar teclado cuando se presiona enviar
            onSendSMS()
        },
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (enabled) PrimaryBlue else MediumBlue.copy(alpha = 0.3f),
            contentColor = Color.White,
            disabledContainerColor = MediumBlue.copy(alpha = 0.3f),
            disabledContentColor = Color.White.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
        } else {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }

        Text(
            text = if (isLoading) "Sending..." else "Send Location",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

@Composable
private fun ModernStatusSection(
    uiState: com.tudominio.smslocation.viewmodel.UiState,
    viewModel: MainViewModel
) {
    // Success message
    AnimatedVisibility(
        visible = uiState.lastMessage.isNotEmpty(),
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.Green.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color.Green,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = uiState.lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Green.copy(alpha = 0.8f)
                )
            }
        }
    }

    // Error message
    AnimatedVisibility(
        visible = uiState.errorMessage.isNotEmpty(),
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.Red.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = Color.Red,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = uiState.errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Red.copy(alpha = 0.8f)
                )
            }
        }
    }
}