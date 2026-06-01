package com.tecsup.agroscan.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.google.android.gms.maps.model.LatLng

/**
 * Modelo de Usuario (Para Autenticación JWT y Roles).
 */
data class User(
    val id: Int,
    val empresaId: Int,
    val nombre: String,
    val email: String,
    val rol: String, // ej. "ADMIN", "OPERADOR"
    val activo: Boolean,
    val token: String? = null
)

/**
 * Modelo de Campos Agrícolas.
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
 * Modelo de Cultivos.
 */
data class Crop(
    val id: Int,
    val campoId: Int,
    val nombre: String,
    val variedad: String,
    val fechaSiembra: String,
    val estado: String,
    val color: Color = Color(0xFF2ECC71)
)

/**
 * Registro histórico de Clima.
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

/**
 * Resultado de un Análisis realizado por la IA.
 */
data class AnalysisResult(
    val title: String,
    val plantName: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val date: String = "",
    val time: String = "",
    val location: String = "",
    val temperature: String = "",
    val humidity: String = "",
    val summary: String = ""
)

/**
 * Información de una Zona para visualización en el mapa y UI.
 */
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

/**
 * Datos de Clima para la tarjeta principal.
 */
data class WeatherData(
    val location: String, 
    val temp: String, 
    val humidity: String, 
    val wind: String, 
    val uv: String, 
    val rain: String
)
