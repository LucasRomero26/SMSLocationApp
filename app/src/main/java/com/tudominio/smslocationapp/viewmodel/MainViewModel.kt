package com.tudominio.smslocation.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.telephony.SmsManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

data class UiState(
    val phoneNumber: String = "",
    val isPhoneNumberValid: Boolean = false,
    val isLoadingLocation: Boolean = false,
    val isSendingSMS: Boolean = false,
    val currentLocation: Location? = null,
    val lastMessage: String = "",
    val errorMessage: String = "",
    val hasLocationPermission: Boolean = false,
    val hasSmsPermission: Boolean = false,
    val showPermissionDialog: Boolean = false
)

class MainViewModel : ViewModel() {

    private var fusedLocationClient: FusedLocationProviderClient? = null

    var uiState by mutableStateOf(UiState())
        private set

    fun initializeLocationClient(context: Context) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        checkPermissions(context)
        // NO obtener ubicación automáticamente
    }

    private fun checkPermissions(context: Context) {
        val hasLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasSmsPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        uiState = uiState.copy(
            hasLocationPermission = hasLocationPermission,
            hasSmsPermission = hasSmsPermission
        )

        // NO obtener ubicación automáticamente
    }

    fun updatePhoneNumber(number: String) {
        // Limpiar el número para validación
        val cleanNumber = number.replace(Regex("[^0-9]"), "")

        // Validar número colombiano (10 dígitos, empieza con 3)
        val isValid = cleanNumber.length == 10 && cleanNumber.startsWith("3")

        uiState = uiState.copy(
            phoneNumber = number,
            isPhoneNumberValid = isValid,
            errorMessage = if (!isValid && number.isNotEmpty()) {
                "Número inválido. Debe ser un móvil colombiano (10 dígitos, inicia con 3)"
            } else ""
        )
    }

    fun getCurrentLocation(context: Context) {
        if (!uiState.hasLocationPermission) {
            uiState = uiState.copy(
                showPermissionDialog = true,
                errorMessage = "Se requiere permiso de ubicación"
            )
            return
        }

        viewModelScope.launch {
            try {
                uiState = uiState.copy(
                    isLoadingLocation = true,
                    errorMessage = ""
                )

                val location = getLastKnownLocation()

                if (location != null) {
                    uiState = uiState.copy(
                        currentLocation = location,
                        isLoadingLocation = false,
                        lastMessage = "Ubicación obtenida: ${String.format("%.6f", location.latitude)}, ${String.format("%.6f", location.longitude)}"
                    )
                } else {
                    uiState = uiState.copy(
                        isLoadingLocation = false,
                        errorMessage = "No se pudo obtener la ubicación. Verifique que el GPS esté activado."
                    )
                }

            } catch (e: Exception) {
                uiState = uiState.copy(
                    isLoadingLocation = false,
                    errorMessage = "Error al obtener ubicación: ${e.message}"
                )
            }
        }
    }

    private suspend fun getLastKnownLocation(): Location? {
        return try {
            val cancellationTokenSource = CancellationTokenSource()

            fusedLocationClient?.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            )?.await()
        } catch (e: SecurityException) {
            null
        }
    }

    fun sendSMSWithLocation(context: Context) {
        // Verificar permisos primero
        if (!uiState.hasSmsPermission || !uiState.hasLocationPermission) {
            uiState = uiState.copy(
                showPermissionDialog = true,
                errorMessage = "Se requieren permisos de SMS y ubicación"
            )
            return
        }

        if (!uiState.isPhoneNumberValid) {
            uiState = uiState.copy(errorMessage = "Número de teléfono inválido")
            return
        }

        viewModelScope.launch {
            try {
                // Paso 1: Indicar que se está enviando SMS
                uiState = uiState.copy(
                    isSendingSMS = true,
                    isLoadingLocation = true,
                    errorMessage = "",
                    lastMessage = ""
                )

                // Paso 2: Obtener ubicación GPS actualizada
                val location = getLastKnownLocation()

                if (location == null) {
                    uiState = uiState.copy(
                        isSendingSMS = false,
                        isLoadingLocation = false,
                        errorMessage = "No se pudo obtener la ubicación GPS. Verifique que el GPS esté activado."
                    )
                    return@launch
                }

                // Paso 3: Crear mensaje con ubicación actualizada
                val message = createLocationMessage(location)
                val fullPhoneNumber = "+57${uiState.phoneNumber.replace(Regex("[^0-9]"), "")}"

                // Paso 4: Enviar SMS
                sendSMS(fullPhoneNumber, message)

                // Paso 5: Actualizar estado de éxito
                uiState = uiState.copy(
                    isSendingSMS = false,
                    isLoadingLocation = false,
                    currentLocation = location, // Guardar ubicación obtenida
                    lastMessage = "SMS enviado exitosamente a $fullPhoneNumber\nLAT: ${String.format("%.6f", location.latitude)}, LON: ${String.format("%.6f", location.longitude)}",
                    phoneNumber = "" // Limpiar campo después del envío
                )

            } catch (e: Exception) {
                uiState = uiState.copy(
                    isSendingSMS = false,
                    isLoadingLocation = false,
                    errorMessage = "Error al enviar SMS: ${e.message}"
                )
            }
        }
    }

    private fun createLocationMessage(location: Location): String {
        val currentTimeMillis = System.currentTimeMillis()

        return "Ubicación GPS:\nLAT: ${String.format("%.6f", location.latitude)}\n" +
                "LON: ${String.format("%.6f", location.longitude)}\n" +
                "Timestamp: $currentTimeMillis"
    }

    private fun sendSMS(phoneNumber: String, message: String) {
        try {
            val smsManager = SmsManager.getDefault()

            // Si el mensaje es muy largo, dividirlo en partes
            val parts = smsManager.divideMessage(message)

            if (parts.size == 1) {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            } else {
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
            }
        } catch (e: Exception) {
            throw e
        }
    }

    fun clearError() {
        uiState = uiState.copy(errorMessage = "")
    }

    fun clearMessage() {
        uiState = uiState.copy(lastMessage = "")
    }

    fun dismissPermissionDialog() {
        uiState = uiState.copy(showPermissionDialog = false)
    }

    fun onPermissionsGranted(context: Context) {
        checkPermissions(context)
        uiState = uiState.copy(showPermissionDialog = false)
        // NO obtener ubicación automáticamente cuando se otorgan permisos
    }
}