package com.tecsup.agroscan

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.tecsup.agroscan.ui.dashboard.DashboardScreen
import com.tecsup.agroscan.ui.login.LoginScreen
import com.tecsup.agroscan.ui.theme.AgroScanTheme
import com.tecsup.agroscan.viewmodel.MainViewModel
import java.util.*

/**
 * Actividad Principal.
 * Gestiona los permisos del sistema y el flujo de navegación raíz.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val lanzadorPermisos = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permisos ->
        viewModel.isLocationGranted = permisos[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (viewModel.isLocationGranted) {
            obtenerUbicacion()
        }
        viewModel.isCameraGranted = permisos[Manifest.permission.CAMERA] == true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        verificarPermisosIniciales()

        setContent {
            AgroScanTheme(darkTheme = viewModel.isDarkTheme) {
                // Estado de navegación sincronizado con el login del ViewModel
                var estadoNavegacion by rememberSaveable { mutableStateOf(if (viewModel.isLoggedIn) "dashboard" else "login") }
                
                LaunchedEffect(viewModel.isLoggedIn) {
                    estadoNavegacion = if (viewModel.isLoggedIn) "dashboard" else "login"
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (estadoNavegacion == "login") {
                            Box(modifier = Modifier.padding(innerPadding)) {
                                LoginScreen(
                                    viewModel = viewModel,
                                    alTenerExito = { estadoNavegacion = "dashboard" }
                                )
                            }
                        } else {
                            DashboardScreen(
                                viewModel = viewModel,
                                onLocationRequest = { solicitarPermisos() },
                                onCameraRequest = { solicitarPermisos() }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun verificarPermisosIniciales() {
        viewModel.isCameraGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        viewModel.isLocationGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (viewModel.isLocationGranted) {
            obtenerUbicacion()
        }
    }

    private fun solicitarPermisos() {
        lanzadorPermisos.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA)
        )
    }

    private fun obtenerUbicacion() {
        val clienteUbicacion = LocationServices.getFusedLocationProviderClient(this)
        try {
            clienteUbicacion.lastLocation.addOnSuccessListener { ubicacion ->
                if (ubicacion != null) {
                    val geocodificador = Geocoder(this, Locale.getDefault())
                    @Suppress("DEPRECATION")
                    val direcciones = geocodificador.getFromLocation(ubicacion.latitude, ubicacion.longitude, 1)
                    if (!direcciones.isNullOrEmpty()) {
                        val ciudad = direcciones[0].locality ?: direcciones[0].subAdminArea ?: "Ubicación Desconocida"
                        viewModel.userCity = "$ciudad, ${direcciones[0].countryName ?: ""}"
                        viewModel.fetchRealWeather(ubicacion.latitude, ubicacion.longitude)
                    }
                }
            }
        } catch (_: SecurityException) {
            viewModel.userCity = "Error GPS"
        }
    }
}
