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

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        viewModel.isLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (viewModel.isLocationGranted) {
            fetchLocation()
        }
        viewModel.isCameraGranted = permissions[Manifest.permission.CAMERA] == true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        checkInitialPermissions()

        setContent {
            AgroScanTheme(darkTheme = viewModel.isDarkTheme) {
                // Navegación principal basada en el estado de Login del ViewModel
                var navigationState by rememberSaveable { mutableStateOf(if (viewModel.isLoggedIn) "dashboard" else "login") }
                
                // Sincronizar estado local con ViewModel (por si expira la sesión)
                LaunchedEffect(viewModel.isLoggedIn) {
                    navigationState = if (viewModel.isLoggedIn) "dashboard" else "login"
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (navigationState == "login") {
                            Box(modifier = Modifier.padding(innerPadding)) {
                                LoginScreen(
                                    viewModel = viewModel,
                                    onLoginSuccess = { navigationState = "dashboard" }
                                )
                            }
                        } else {
                            DashboardScreen(
                                viewModel = viewModel,
                                onLocationRequest = { requestInitialPermissions() },
                                onCameraRequest = { requestInitialPermissions() }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun checkInitialPermissions() {
        viewModel.isCameraGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        viewModel.isLocationGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (viewModel.isLocationGranted) {
            fetchLocation()
        }
    }

    private fun requestInitialPermissions() {
        requestPermissionsLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA)
        )
    }

    private fun fetchLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val geocoder = Geocoder(this, Locale.getDefault())
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val city = addresses[0].locality ?: addresses[0].subAdminArea ?: "Ubicación Desconocida"
                        viewModel.userCity = "$city, ${addresses[0].countryName ?: ""}"
                        viewModel.fetchRealWeather(location.latitude, location.longitude)
                    }
                }
            }
        } catch (e: SecurityException) {
            viewModel.userCity = "Error GPS"
        }
    }
}
