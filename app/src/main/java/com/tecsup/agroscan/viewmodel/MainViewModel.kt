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
import com.tecsup.agroscan.data.Field as AgroField
import com.tecsup.agroscan.network.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.*

/**
 * Cerebro de la aplicación (ViewModel).
 * Sincroniza datos reales entre la base de datos PostgreSQL y la interfaz de usuario.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val preferencias = application.getSharedPreferences("agroscan_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    // Configuración de APIS
    private val weatherApiKey = "5f0c1c384f0bfb043d85c828865604a7"
    private val roboflowApiKey = "0eETUshIouQdAiHdxL83"
    private val roboflowProject = "plant-diseases-9mchz"
    private val roboflowVersion = "2"

    // --- ESTADO DEL USUARIO ---
    var currentUser by mutableStateOf<User?>(null)
    var isLoggedIn by mutableStateOf(value = false)

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
    private var agroScanApiService: AgroScanApiService? = null

    init {
        cargarSesion()
        cargarHistorialLocal()
    }

    // --- LÓGICA DE USUARIOS ---
    
    fun login(email: String, pass: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val apiPublica = AgroScanApiService.create()
                val respuesta = apiPublica.login(email, pass)
                
                val usuario = User(
                    id = 0,
                    empresaId = respuesta.empresa_id,
                    nombre = respuesta.nombre,
                    email = email,
                    rol = respuesta.rol.uppercase(),
                    activo = true,
                    token = respuesta.access_token
                )
                
                currentUser = usuario
                isLoggedIn = true
                agroScanApiService = AgroScanApiService.create(usuario.token)
                
                guardarSesion(usuario)
                sincronizarDatosDelServidor()
                onResult(true)
            } catch (e: Exception) {
                Log.e("VM", "Error login", e)
                onResult(false)
            }
        }
    }

    private fun guardarSesion(user: User) {
        preferencias.edit().putString("user_session", gson.toJson(user)).apply()
    }

    private fun cargarSesion() {
        val json = preferencias.getString("user_session", null)
        if (json != null) {
            try {
                val usuario = gson.fromJson(json, User::class.java)
                currentUser = usuario
                isLoggedIn = true
                agroScanApiService = AgroScanApiService.create(usuario.token)
                sincronizarDatosDelServidor()
            } catch (e: Exception) { logout() }
        }
    }

    fun logout() {
        currentUser = null
        isLoggedIn = false
        agroScanApiService = null
        preferencias.edit().remove("user_session").apply()
    }

    // --- SINCRONIZACIÓN CON POSTGRESQL ---

    fun sincronizarDatosDelServidor() {
        viewModelScope.launch {
            val api = agroScanApiService ?: return@launch
            try {
                // 1. Obtener campos de la BD real
                val camposBackend = api.listarCampos()
                
                // 2. Mapear para la UI del mapa
                zones.clear()
                camposBackend.forEach { campo ->
                    zones.add(ZoneInfo(
                        name = campo.nombre,
                        crop = "Detectando cultivo...",
                        color = Color(0xFF007AFF),
                        daysToHarvest = 30,
                        location = LatLng(campo.latitud, campo.longitud),
                        hectares = campo.hectares, // Sincronizado con el modelo Field.hectares
                        plantingDate = "--",
                        cropStatus = "Activo"
                    ))
                }
                
                // 3. Cargar cultivos asociados
                val cultivosBackend = api.listarCultivos()
                cultivosBackend.forEach { cultivo ->
                    val campoAsociado = camposBackend.find { it.id == cultivo.campoId }
                    if (campoAsociado != null) {
                        val index = zones.indexOfFirst { it.name == campoAsociado.nombre }
                        if (index != -1) {
                            val zonaExistente = zones[index]
                            zones[index] = zonaExistente.copy(
                                crop = "${cultivo.nombre} (${cultivo.variedad})",
                                plantingDate = cultivo.fechaSiembra,
                                cropStatus = cultivo.estado
                            )
                        }
                    }
                }
                guardarZonasLocales()
            } catch (e: Exception) {
                Log.e("VM", "Error sync", e)
            }
        }
    }

    private fun guardarZonasLocales() {
        preferencias.edit().putString("saved_zones", gson.toJson(zones.toList())).apply()
    }

    private fun cargarHistorialLocal() {
        val jsonHistorial = preferencias.getString("analysis_history", null)
        if (jsonHistorial != null) {
            try {
                val tipo = object : TypeToken<List<AnalysisResult>>() {}.type
                analysisHistory.clear()
                analysisHistory.addAll(gson.fromJson(jsonHistorial, tipo))
            } catch (e: Exception) { Log.e("VM", "History error", e) }
        }
    }

    private fun guardarHistorialLocal() {
        preferencias.edit().putString("analysis_history", gson.toJson(analysisHistory.toList())).apply()
    }

    fun addZone(zone: ZoneInfo) {
        zones.add(zone)
        guardarZonasLocales()
    }

    fun removeZone(zone: ZoneInfo) {
        zones.remove(zone)
        guardarZonasLocales()
    }

    fun updateZone(oldZone: ZoneInfo, newZone: ZoneInfo) {
        val index = zones.indexOf(oldZone)
        if (index != -1) {
            zones[index] = newZone
            guardarZonasLocales()
        }
    }

    fun updateZoneFromAI(zoneName: String, status: String) {
        val index = zones.indexOfFirst { it.name == zoneName }
        if (index != -1) {
            val zona = zones[index]
            val colorNuevo = when {
                status.contains("Saludable", true) -> Color(0xFF2ECC71)
                status.contains("Vigilancia", true) -> Color(0xFFFFB300)
                else -> Color(0xFFE57373)
            }
            zones[index] = zona.copy(cropStatus = status, color = colorNuevo)
            guardarZonasLocales()
        }
    }

    fun fetchRealWeather(lat: Double, lon: Double) {
        viewModelScope.launch {
            try {
                val response = weatherApiService.getCurrentWeather(lat, lon, weatherApiKey)
                realWeatherData = WeatherData(response.name, "${response.main.temp.toInt()}°C", "${response.main.humidity}%", "${response.wind.speed} km/h", "UV 7", "0.0mm")
            } catch (e: Exception) { Log.e("VM", "Error clima", e) }
        }
    }

    private fun translateResult(englishName: String): String {
        val name = englishName.lowercase()
        return when {
            name.contains("healthy") -> "Planta Saludable"
            name.contains("rice leaf smut") -> "Mancha Foliar (Arroz)"
            name.contains("bacterial leaf blight") -> "Tizón Bacteriano"
            name.contains("brown spot") -> "Mancha Parda"
            name.contains("leaf blast") -> "Piricularia (Quemado)"
            name.contains("rust") -> "Roya (Hongo)"
            name.contains("smut") -> "Falso Carbón"
            name.contains("blight") -> "Tizón / Añublo"
            name.contains("spot") -> "Mancha Foliar"
            else -> englishName
        }
    }

    fun analyzePlantImage(base64Image: String, zoneName: String = "Zona A1", onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val requestBody = base64Image.toRequestBody("text/plain".toMediaType())
                val response = roboflowApiService.detectDisease(roboflowProject, roboflowVersion, roboflowApiKey, requestBody)
                
                val rawResult = if (response.predictions.isNotEmpty()) {
                    response.predictions.maxByOrNull { it.confidence }?.className ?: "Healthy"
                } else "Healthy"

                val translatedResult = translateResult(rawResult)
                val confidence = if (response.predictions.isNotEmpty()) {
                    " (${(response.predictions.maxByOrNull { it.confidence }!!.confidence * 100).toInt()}%)"
                } else ""

                val fullResultText = translatedResult + confidence
                val now = Date()
                val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(now)
                val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
                
                val historyItem = AnalysisResult(
                    title = "Análisis IA",
                    plantName = zoneName,
                    description = fullResultText,
                    icon = Icons.Default.AutoAwesome,
                    color = if (translatedResult.contains("Saludable")) Color(0xFF2ECC71) else Color(0xFFE57373),
                    date = dateStr,
                    time = timeStr,
                    location = userCity,
                    temperature = realWeatherData?.temp ?: "26°C",
                    humidity = realWeatherData?.humidity ?: "60%",
                    summary = "Sincronización exitosa: El diagnóstico indica $translatedResult."
                )
                
                analysisHistory.add(0, historyItem)
                guardarHistorialLocal()
                updateZoneFromAI(zoneName, translatedResult)
                onResult(fullResultText)
            } catch (e: Exception) {
                Log.e("VM", "Error IA", e)
                onResult("Error de conexión con la IA")
            }
        }
    }

    fun toggleTheme(isDark: Boolean) { isDarkTheme = isDark }
}
