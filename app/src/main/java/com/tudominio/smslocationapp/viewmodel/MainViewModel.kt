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
import kotlinx.coroutines.delay
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
        // Do not get location automatically
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

        // Do not get location automatically
    }

    fun updatePhoneNumber(number: String) {
        // Clean the number for validation
        val cleanNumber = number.replace(Regex("[^0-9]"), "")

        // Validate Colombian number (10 digits, starts with 3)
        val isValid = cleanNumber.length == 10 && cleanNumber.startsWith("3")

        uiState = uiState.copy(
            phoneNumber = number,
            isPhoneNumberValid = isValid,
            errorMessage = if (!isValid && number.isNotEmpty()) {
                "Must be Colombian mobile"
            } else ""
        )
    }

    fun getCurrentLocation(context: Context) {
        if (!uiState.hasLocationPermission) {
            uiState = uiState.copy(
                showPermissionDialog = true,
                errorMessage = "Location permission required"
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
                        lastMessage = "Location obtained: ${String.format("%.6f", location.latitude)}, ${String.format("%.6f", location.longitude)}"
                    )
                } else {
                    uiState = uiState.copy(
                        isLoadingLocation = false,
                        errorMessage = "Could not get location. Please verify that GPS is enabled."
                    )
                }

            } catch (e: Exception) {
                uiState = uiState.copy(
                    isLoadingLocation = false,
                    errorMessage = "Error getting location: ${e.message}"
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

    /**
     * Función principal para enviar SMS con ubicación
     * Incluye manejo automático de estado y limpieza después de 3 segundos
     */
    fun sendSMSWithLocation(context: Context) {
        // Check permissions first
        if (!uiState.hasSmsPermission || !uiState.hasLocationPermission) {
            uiState = uiState.copy(
                showPermissionDialog = true,
                errorMessage = "SMS and location permissions required"
            )
            return
        }

        if (!uiState.isPhoneNumberValid) {
            uiState = uiState.copy(errorMessage = "Invalid phone number")
            return
        }

        viewModelScope.launch {
            try {
                // Limpiar mensajes anteriores
                clearMessage()
                clearError()

                // Step 1: Indicate SMS sending is in progress
                uiState = uiState.copy(
                    isSendingSMS = true,
                    isLoadingLocation = true
                )

                // Step 2: Get updated GPS location
                val location = getLastKnownLocation()

                if (location == null) {
                    uiState = uiState.copy(
                        isSendingSMS = false,
                        isLoadingLocation = false,
                        errorMessage = "Could not get GPS location. Please verify that GPS is enabled."
                    )
                    return@launch
                }

                // Step 3: Update state with location
                uiState = uiState.copy(
                    currentLocation = location,
                    isLoadingLocation = false
                )

                // Step 4: Create message with updated location
                val message = createLocationMessage(location)
                val fullPhoneNumber = "+57${uiState.phoneNumber.replace(Regex("[^0-9]"), "")}"

                // Step 5: Send SMS
                sendSMS(fullPhoneNumber, message)

                // Step 6: Update success state
                uiState = uiState.copy(
                    isSendingSMS = false,
                    lastMessage = "SMS sent successfully to $fullPhoneNumber"
                )

                // Step 7: Auto-limpiar después de 3 segundos (manejado en la UI)

            } catch (e: Exception) {
                uiState = uiState.copy(
                    isSendingSMS = false,
                    isLoadingLocation = false,
                    errorMessage = "Error sending SMS: ${e.message}"
                )
            }
        }
    }

    /**
     * Limpia la ubicación actual y resetea el estado después de enviar SMS
     * Se llama automáticamente después de 3 segundos desde la UI
     */
    fun clearLocationAndResetButton() {
        uiState = uiState.copy(
            currentLocation = null,
            // El botón se desactivará automáticamente porque currentLocation es null
            // Los permisos y número de teléfono se mantienen
        )
    }

    /**
     * Función auxiliar para validar si se puede enviar SMS
     */
    fun canSendSMS(): Boolean {
        return uiState.isPhoneNumberValid &&
                uiState.hasLocationPermission &&
                uiState.hasSmsPermission &&
                !uiState.isSendingSMS
    }

    private fun createLocationMessage(location: Location): String {
        val currentTimeMillis = System.currentTimeMillis()

        return "GPS Location:\n" +
                "LAT: ${String.format("%.6f", location.latitude)}\n" +
                "LON: ${String.format("%.6f", location.longitude)}\n" +
                "Timestamp: $currentTimeMillis"
    }

    private fun sendSMS(phoneNumber: String, message: String) {
        try {
            val smsManager = SmsManager.getDefault()

            // If message is too long, split it into parts
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
        // Do not get location automatically when permissions are granted
    }
}