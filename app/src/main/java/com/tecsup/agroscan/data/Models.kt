package com.tecsup.agroscan.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.google.android.gms.maps.model.LatLng

/**
 * 1) Modelo de Usuario (Para Login JWT y Roles)
 */
data class User(
    val id: Int,
    val empresaId: Int,
    val nombre: String,
    val email: String,
    val rol: String, // ej. "ADMIN", "OPERADOR"
    val activo: Boolean,
    val token: String? = null // JWT Token
)

/**
 * 2) Modelo de Campos (Ubicación y Hectáreas)
 */
data class Field(
    val id: Int,
    val empresaId: Int,
    val nombre: String,
    val ubicacion: String,
    val latitud: Double,
    val longitud: Double,
    val hectares: Double
)

/**
 * 3) Modelo de Cultivos (Asociados a un Campo)
 */
data class Crop(
    val id: Int,
    val campoId: Int,
    val nombre: String,
    val variedad: String,
    val fechaSiembra: String,
    val estado: String, // ej. "Saludable", "Enfermo"
    val color: Color = Color(0xFF2ECC71)
)

/**
 * 4) Modelo de Registros de Clima (Historial y Alertas)
 */
data class WeatherRecord(
    val id: Int,
    val campoId: Int,
    val temperatura: Double,
    val temperaturaMin: Double,
    val temperaturaMax: Double,
    val humedad: Int,
    val precipitacion: Double,
    val viento: Double,
    val indiceUv: Int,
    val descripcion: String,
    val fecha: String
)

// Modelos auxiliares para la UI
data class AnalysisResult(
    val title: String,
    val plantName: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val date: String = "",
    val summary: String = ""
)

// Mantengo ZoneInfo por compatibilidad con el código actual, pero mapeado a Crop/Field
data class ZoneInfo(
    val name: String, 
    val crop: String, 
    var color: Color, 
    val daysToHarvest: Int, 
    val location: LatLng,
    val hectares: Double = 0.0,
    val plantingDate: String = "",
    val observations: String = "",
    var cropStatus: String = "Pendiente de Análisis",
    val photoUri: String? = null
)

data class WeatherData(
    val location: String, 
    val temp: String, 
    val humidity: String, 
    val wind: String, 
    val uv: String, 
    val rain: String
)
