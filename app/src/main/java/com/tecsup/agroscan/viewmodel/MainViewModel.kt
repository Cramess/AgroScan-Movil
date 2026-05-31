package com.tecsup.agroscan.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tecsup.agroscan.data.*
import com.tecsup.agroscan.network.WeatherApiService
import com.tecsup.agroscan.network.RoboflowApiService
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("agroscan_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    // Configuración APIS
    private val weatherApiKey = "5f0c1c384f0bfb043d85c828865604a7"
    private val roboflowApiKey = "0eETUshIouQdAiHdxL83"
    private val roboflowProject = "planta-enfermedad-ep6jy"
    private val roboflowVersion = 2

    // --- ESTADO DEL USUARIO ---
    var currentUser by mutableStateOf<User?>(null)
    var isLoggedIn by mutableStateOf(false)

    // Estados Globales
    var isDarkTheme by mutableStateOf(value = false)
    var userCity by mutableStateOf("Buscando ubicación...")
    var isCameraGranted by mutableStateOf(false)
    var isLocationGranted by mutableStateOf(false)

    // UI States
    var realWeatherData by mutableStateOf<WeatherData?>(null)
    var analysisHistory = mutableStateListOf<AnalysisResult>()
    var zones = mutableStateListOf<ZoneInfo>() 

    private val weatherApiService = WeatherApiService.create()
    private val roboflowApiService = RoboflowApiService.create()

    init {
        loadSession()
        loadLocalHistory()
    }

    // --- LÓGICA DE USUARIOS ---
    
    fun login(email: String, pass: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            if (email.contains("@") && pass.length >= 4) {
                val mockUser = User(
                    id = 1,
                    empresaId = 101,
                    nombre = "Cristian Alex",
                    email = email,
                    rol = if (email.contains("admin")) "ADMIN" else "OPERADOR",
                    activo = true,
                    token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
                )
                currentUser = mockUser
                isLoggedIn = true
                saveSession(mockUser)
                loadInitialData()
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }

    private fun saveSession(user: User) {
        prefs.edit().putString("user_session", gson.toJson(user)).apply()
    }

    private fun loadSession() {
        val json = prefs.getString("user_session", null)
        if (json != null) {
            currentUser = gson.fromJson(json, User::class.java)
            isLoggedIn = true
            loadInitialData()
        }
    }

    fun logout() {
        currentUser = null
        isLoggedIn = false
        prefs.edit().remove("user_session").apply()
    }

    // --- PERSISTENCIA DE DATOS ---

    private fun loadInitialData() {
        val zonesJson = prefs.getString("saved_zones", null)
        if (zonesJson != null) {
            try {
                val type = object : TypeToken<List<ZoneInfo>>() {}.type
                val savedZones: List<ZoneInfo> = gson.fromJson(zonesJson, type)
                zones.clear()
                zones.addAll(savedZones)
            } catch (e: Exception) {
                Log.e("VM", "Error loading zones", e)
                loadDefaultZones()
            }
        } else {
            loadDefaultZones()
        }
    }

    private fun loadDefaultZones() {
        zones.clear()
        zones.addAll(listOf(
            ZoneInfo("Campo Norte", "Espárrago (UC157)", Color(0xFF2ECC71), 27, LatLng(-14.0678, -75.7286), 250.0, "10/02/2024", "", "Saludable"),
            ZoneInfo("Campo Sur", "Palta (Hass)", Color(0xFFFFB300), 45, LatLng(-14.0700, -75.7300), 180.0, "15/01/2024", "", "Vigilancia")
        ))
        saveLocalZones()
    }

    private fun saveLocalZones() {
        prefs.edit().putString("saved_zones", gson.toJson(zones.toList())).apply()
    }

    private fun loadLocalHistory() {
        val historyJson = prefs.getString("analysis_history", null)
        if (historyJson != null) {
            try {
                val type = object : TypeToken<List<AnalysisResult>>() {}.type
                analysisHistory.clear()
                analysisHistory.addAll(gson.fromJson(historyJson, type))
            } catch (e: Exception) { Log.e("VM", "History error", e) }
        }
    }

    private fun saveLocalHistory() {
        prefs.edit().putString("analysis_history", gson.toJson(analysisHistory.toList())).apply()
    }

    fun addZone(zone: ZoneInfo) {
        zones.add(zone)
        saveLocalZones()
    }

    fun removeZone(zone: ZoneInfo) {
        zones.remove(zone)
        saveLocalZones()
    }

    fun updateZoneFromAI(zoneName: String, status: String) {
        val index = zones.indexOfFirst { it.name == zoneName }
        if (index != -1) {
            val zone = zones[index]
            val newColor = when {
                status.contains("Saludable", true) -> Color(0xFF2ECC71)
                status.contains("Vigilancia", true) -> Color(0xFFFFB300)
                else -> Color(0xFFE57373)
            }
            zones[index] = zone.copy(cropStatus = status, color = newColor)
            saveLocalZones()
        }
    }

    fun fetchRealWeather(lat: Double, lon: Double) {
        viewModelScope.launch {
            try {
                val response = weatherApiService.getCurrentWeather(lat, lon, weatherApiKey)
                realWeatherData = WeatherData(response.name, "${response.main.temp.toInt()}°C", "${response.main.humidity}%", "${response.wind.speed} km/h", "UV 7", "0.0mm")
            } catch (e: Exception) { Log.e("VM", "Weather error", e) }
        }
    }

    fun analyzePlantImage(base64Image: String, zoneName: String = "Zona A1", onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val requestBody = base64Image.toRequestBody("text/plain".toMediaType())
                val response = roboflowApiService.detectDisease(roboflowProject, roboflowVersion, roboflowApiKey, requestBody)
                
                val result = if (response.predictions.isNotEmpty()) {
                    val best = response.predictions.maxByOrNull { it.confidence }
                    "${best?.className} (${(best!!.confidence * 100).toInt()}%)"
                } else "Saludable"

                val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                val historyItem = AnalysisResult(
                    title = "Análisis IA",
                    plantName = zoneName,
                    description = result,
                    icon = Icons.Default.AutoAwesome,
                    color = if (result.contains("Saludable")) Color(0xFF2ECC71) else Color(0xFFE57373),
                    date = date,
                    summary = "Diagnóstico completado."
                )
                
                analysisHistory.add(0, historyItem)
                saveLocalHistory()
                updateZoneFromAI(zoneName, result)
                onResult(result)
            } catch (e: Exception) { onResult("Error IA") }
        }
    }

    fun toggleTheme(isDark: Boolean) { isDarkTheme = isDark }
}
