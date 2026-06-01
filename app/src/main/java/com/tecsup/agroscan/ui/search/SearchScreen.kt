package com.tecsup.agroscan.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tecsup.agroscan.viewmodel.MainViewModel
import com.tecsup.agroscan.data.AnalysisResult

/**
 * Pantalla de Historial de Análisis.
 * Muestra la lista de diagnósticos realizados por la IA.
 */
@Composable
fun SearchScreen(viewModel: MainViewModel) {
    var consultaBusqueda by remember { mutableStateOf("") }
    
    // Filtrado dinámico por nombre de planta o enfermedad
    val historialFiltrado = remember(consultaBusqueda, viewModel.analysisHistory.size) {
        if (consultaBusqueda.isEmpty()) {
            viewModel.analysisHistory
        } else {
            viewModel.analysisHistory.filter { 
                it.plantName.contains(consultaBusqueda, ignoreCase = true) || 
                it.description.contains(consultaBusqueda, ignoreCase = true) 
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Historial",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Buscador estilo One UI
        OutlinedTextField(
            value = consultaBusqueda,
            onValueChange = { consultaBusqueda = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Buscar análisis...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (historialFiltrado.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (consultaBusqueda.isEmpty()) "No hay análisis registrados" else "Sin resultados",
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 140.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(
                    items = historialFiltrado,
                    key = { it.date + it.time + it.plantName }
                ) { analisis ->
                    TarjetaHistorialCompleta(analisis)
                }
            }
        }
    }
}

@Composable
fun TarjetaHistorialCompleta(analisis: AnalysisResult) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = analisis.color.copy(alpha = 0.15f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        analisis.icon, 
                        null, 
                        modifier = Modifier.padding(10.dp),
                        tint = analisis.color
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = analisis.plantName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "${analisis.date} • ${analisis.time}", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Información sincronizada (Ubicación, Temp, Humedad)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ItemInfoSincro(Icons.Default.LocationOn, analisis.location)
                ItemInfoSincro(Icons.Default.DeviceThermostat, analisis.temperature)
                ItemInfoSincro(Icons.Default.WaterDrop, analisis.humidity)
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = analisis.description,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = analisis.summary,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun ItemInfoSincro(icono: androidx.compose.ui.graphics.vector.ImageVector, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icono, null, modifier = Modifier.size(14.dp), tint = Color(0xFF007AFF))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = texto, fontSize = 11.sp, color = Color.Gray)
    }
}
