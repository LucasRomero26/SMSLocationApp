package com.tudominio.smslocation.ui.screen

import android.Manifest
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.tudominio.smslocation.viewmodel.MainViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState = viewModel.uiState

    // Gestión de permisos
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.SEND_SMS
        )
    )

    // Inicializar cliente de ubicación
    LaunchedEffect(Unit) {
        viewModel.initializeLocationClient(context)
    }

    // Manejar cambios en permisos
    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted) {
            viewModel.onPermissionsGranted(context)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header con título
        HeaderSection()

        Spacer(modifier = Modifier.height(32.dp))

        // Tarjeta principal
        MainCard(
            uiState = uiState,
            viewModel = viewModel,
            permissionsState = permissionsState
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Información de estado
        StatusSection(uiState = uiState, viewModel = viewModel)
    }
}

@Composable
private fun HeaderSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "SMS Ubicación",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Envía tu ubicación GPS vía SMS",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun MainCard(
    uiState: com.tudominio.smslocation.viewmodel.UiState,
    viewModel: MainViewModel,
    permissionsState: MultiplePermissionsState
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Campo de número telefónico
            PhoneNumberInput(
                phoneNumber = uiState.phoneNumber,
                isValid = uiState.isPhoneNumberValid,
                onPhoneNumberChange = viewModel::updatePhoneNumber,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Estado de permisos (sin obtener GPS automáticamente)
            PermissionsStatus(
                hasLocationPermission = uiState.hasLocationPermission,
                hasSmsPermission = uiState.hasSmsPermission,
                onRequestPermissions = {
                    permissionsState.launchMultiplePermissionRequest()
                }
            )

            // Mostrar proceso de envío SMS
            AnimatedVisibility(
                visible = uiState.isSendingSMS,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                SMSProcessCard(
                    isLoadingLocation = uiState.isLoadingLocation,
                    isSendingSMS = uiState.isSendingSMS,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            // Mostrar coordenadas si están disponibles y se acaba de enviar
            AnimatedVisibility(
                visible = uiState.currentLocation != null && !uiState.isSendingSMS,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                uiState.currentLocation?.let { location ->
                    LocationDisplay(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botón de enviar SMS
            SendSMSButton(
                enabled = uiState.isPhoneNumberValid && uiState.hasLocationPermission && uiState.hasSmsPermission,
                isLoading = uiState.isSendingSMS,
                onSendSMS = { viewModel.sendSMSWithLocation(context) }
            )
        }
    }
}

@Composable
private fun PhoneNumberInput(
    phoneNumber: String,
    isValid: Boolean,
    onPhoneNumberChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Número de destino",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = onPhoneNumberChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("3012345678") },
            leadingIcon = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 12.dp)
                ) {
                    Text(
                        text = "+57",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Divider(
                        modifier = Modifier
                            .height(24.dp)
                            .width(1.dp)
                            .padding(horizontal = 8.dp),
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            },
            trailingIcon = {
                if (phoneNumber.isNotEmpty()) {
                    Icon(
                        imageVector = if (isValid) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (isValid) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (isValid && phoneNumber.isNotEmpty())
                    MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
            )
        )

        Text(
            text = if (phoneNumber.isEmpty()) "Ingrese número móvil colombiano"
            else if (isValid) "✓ Número válido"
            else "Debe ser móvil colombiano (10 dígitos, inicia con 3)",
            style = MaterialTheme.typography.bodySmall,
            color = if (phoneNumber.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant
            else if (isValid) MaterialTheme.colorScheme.tertiary
            else MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 4.dp, start = 16.dp)
        )
    }
}

@Composable
private fun PermissionsStatus(
    hasLocationPermission: Boolean,
    hasSmsPermission: Boolean,
    onRequestPermissions: () -> Unit
) {
    val allPermissionsGranted = hasLocationPermission && hasSmsPermission

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (allPermissionsGranted)
                MaterialTheme.colorScheme.tertiaryContainer
            else
                MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (allPermissionsGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (allPermissionsGranted)
                    MaterialTheme.colorScheme.onTertiaryContainer
                else
                    MaterialTheme.colorScheme.onErrorContainer
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (allPermissionsGranted)
                        "✓ Permisos concedidos"
                    else
                        "Permisos requeridos",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (allPermissionsGranted)
                        MaterialTheme.colorScheme.onTertiaryContainer
                    else
                        MaterialTheme.colorScheme.onErrorContainer
                )

                Text(
                    text = if (allPermissionsGranted)
                        "GPS y SMS disponibles"
                    else
                        "Necesarios para obtener GPS y enviar SMS",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (allPermissionsGranted)
                        MaterialTheme.colorScheme.onTertiaryContainer
                    else
                        MaterialTheme.colorScheme.onErrorContainer
                )
            }

            if (!allPermissionsGranted) {
                IconButton(onClick = onRequestPermissions) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Solicitar permisos",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun SMSProcessCard(
    isLoadingLocation: Boolean,
    isSendingSMS: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                strokeWidth = 3.dp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = when {
                    isLoadingLocation -> "📡 Obteniendo ubicación GPS..."
                    isSendingSMS -> "📱 Enviando SMS..."
                    else -> "⏳ Procesando..."
                },
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )

            Text(
                text = when {
                    isLoadingLocation -> "Por favor espera mientras obtenemos tu ubicación actual"
                    isSendingSMS -> "Enviando mensaje con coordenadas GPS"
                    else -> "Preparando envío..."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LocationDisplay(
    latitude: Double,
    longitude: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Coordenadas GPS",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "LAT: ${String.format("%.6f", latitude)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )

            Text(
                text = "LON: ${String.format("%.6f", longitude)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
private fun SendSMSButton(
    enabled: Boolean,
    isLoading: Boolean,
    onSendSMS: () -> Unit
) {
    Button(
        onClick = onSendSMS,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.tertiary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onTertiary,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Enviando SMS...")
        } else {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Enviar SMS con ubicación",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun StatusSection(
    uiState: com.tudominio.smslocation.viewmodel.UiState,
    viewModel: MainViewModel
) {
    // Mensajes de éxito
    AnimatedVisibility(
        visible = uiState.lastMessage.isNotEmpty(),
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = uiState.lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = viewModel::clearMessage) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }

    // Mensajes de error
    AnimatedVisibility(
        visible = uiState.errorMessage.isNotEmpty(),
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = uiState.errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = viewModel::clearError) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}