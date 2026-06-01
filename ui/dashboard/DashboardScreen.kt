package com.tecsup.agroscan.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tecsup.agroscan.ui.settings.SettingsScreen
import com.tecsup.agroscan.ui.search.SearchScreen
import com.tecsup.agroscan.ui.ia.AIScreen
import com.tecsup.agroscan.viewmodel.MainViewModel
import com.tecsup.agroscan.data.ZoneInfo
import com.tecsup.agroscan.data.WeatherData
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.ui.graphics.Brush

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onLocationRequest: () -> Unit = {},
    onCameraRequest: () -> Unit = {}
) {
    var selectedItem by remember { mutableIntStateOf(0) }
    var selectedZone by remember { mutableStateOf<ZoneInfo?>(null) }
    var showDetailSheet by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }
    val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val addSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val bgColor = MaterialTheme.colorScheme.background

    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                when (selectedItem) {
                    0 -> MainDashboardContent(
                        locationEnabled = viewModel.isLocationGranted,
                        cityName = viewModel.userCity,
                        zones = viewModel.zones,
                        realWeather = viewModel.realWeatherData,
                        onZoneClick = {
                            selectedZone = it
                            showDetailSheet = true
                        },
                        onAddClick = { showAddSheet = true },
                        onEditZone = { /* Logica de editar */ },
                        onDeleteZone = { viewModel.removeZone(it) }
                    )
                    1 -> AIScreen(viewModel = viewModel)
                    2 -> SearchScreen(viewModel = viewModel)
                    3 -> SettingsScreen(
                        viewModel = viewModel,
                        onLocationToggle = onLocationRequest,
                        onCameraToggle = onCameraRequest
                    )
                }
            }
        }

        // --- DEGRADADOS ---
        Box(modifier = Modifier.fillMaxWidth().height(80.dp).background(Brush.verticalGradient(colors = listOf(bgColor, Color.Transparent))).align(Alignment.TopCenter))
        Box(modifier = Modifier.fillMaxWidth().height(180.dp).align(Alignment.BottomCenter).background(Brush.verticalGradient(colors = listOf(Color.Transparent, bgColor.copy(alpha = 0.5f), bgColor))))

        // --- BARRA DE NAVEGACIÓN ---
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 12.dp).fillMaxWidth().height(72.dp),
            shape = RoundedCornerShape(36.dp),
            color = Color.White.copy(alpha = 0.98f),
            shadowElevation = 10.dp
        ) {
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                val navItems = listOf("Inicio", "IA", "Buscar", "Ajustes")
                val icons = listOf(Icons.Default.Home, Icons.Default.AutoAwesome, Icons.Default.Search, Icons.Default.Settings)
                navItems.forEachIndexed { index, item ->
                    val isSelected = selectedItem == index
                    Column(modifier = Modifier.weight(1f).clickable { selectedItem = index }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(imageVector = icons[index], contentDescription = item, tint = if (isSelected) Color(0xFF007AFF) else Color(0xFF8E9196), modifier = Modifier.size(if (isSelected) 28.dp else 24.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = item, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = if (isSelected) Color(0xFF007AFF) else Color(0xFF8E9196))
                    }
                }
            }
        }

        // --- BOTTOM SHEETS ---
        if (showDetailSheet && selectedZone != null) {
            ModalBottomSheet(
                onDismissRequest = { showDetailSheet = false },
                sheetState = detailSheetState,
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            ) {
                ZoneDetailContent(selectedZone!!)
            }
        }

        if (showAddSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAddSheet = false },
                sheetState = addSheetState,
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            ) {
                AddZoneForm(onAdd = { newZone ->
                    viewModel.addZone(newZone)
                    showAddSheet = false
                })
            }
        }
    }
}

@Composable
fun MainDashboardContent(
    locationEnabled: Boolean,
    cityName: String,
    zones: List<ZoneInfo>,
    realWeather: WeatherData? = null,
    onZoneClick: (ZoneInfo) -> Unit,
    onAddClick: () -> Unit,
    onEditZone: (ZoneInfo) -> Unit,
    onDeleteZone: (ZoneInfo) -> Unit
) {
    val nextHarvestZone = zones.filter { it.daysToHarvest <= 30 }.minByOrNull { it.daysToHarvest }
    val totalHectares = zones.sumOf { it.hectares }.toInt()
    var isWeatherExpanded by remember { mutableStateOf(false) }

    val weatherData = remember(locationEnabled, cityName, realWeather) {
        realWeather ?: if (locationEnabled) {
            WeatherData(location = cityName, temp = "26°C", humidity = "62%", wind = "14km/h", uv = "UV 7", rain = "0.0mm")
        } else {
            WeatherData(location = cityName, temp = "--°C", humidity = "--%", wind = "--km/h", uv = "UV --", rain = "--mm")
        }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
        Spacer(modifier = Modifier.height(32.dp))
        Text(text = "Monitoreo de Cultivos", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Text(text = "$totalHectares hectáreas en monitoreo activo", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(24.dp))
        
        WeatherCard(
            data = weatherData, 
            expanded = isWeatherExpanded, 
            onToggle = { isWeatherExpanded = !isWeatherExpanded }
        )

        if (!locationEnabled) {
            Spacer(modifier = Modifier.height(12.dp))
            AlertCard(title = "Ubicación requerida", subtitle = "Activa GPS para datos locales", icon = Icons.Default.LocationOff, containerColor = Color(0xFFFDE8E8), contentColor = Color(0xFFC81E1E))
        }

        nextHarvestZone?.let { zone ->
            Spacer(modifier = Modifier.height(16.dp))
            AlertCard(title = "Cosecha Óptima Próxima", subtitle = "${zone.name} lista en ${zone.daysToHarvest} días", icon = Icons.Default.WarningAmber, containerColor = Color(0xFFFFF4E5), contentColor = Color(0xFF855300))
        }

        Spacer(modifier = Modifier.height(32.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Mapa de Zonas", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onAddClick) {
                    Icon(Icons.Default.AddCircle, contentDescription = "Agregar Zona", tint = Color(0xFF007AFF), modifier = Modifier.size(28.dp))
                }
            }
            IconButton(onClick = { /* Lógica de refrescar */ }) {
                Icon(Icons.Default.Sync, contentDescription = "Sincronizar", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        ZoneGrid(zones, onZoneClick, onEditZone, onDeleteZone)
        
        Spacer(modifier = Modifier.height(140.dp))
    }
}

@Composable
fun AddZoneForm(onAdd: (ZoneInfo) -> Unit) {
    var name by remember { mutableStateOf("") }
    var crop by remember { mutableStateOf("") }
    var days by remember { mutableStateOf("") }
    var hectares by remember { mutableStateOf("") }
    var plantingDate by remember { mutableStateOf("") }
    var observations by remember { mutableStateOf("") }
    var capturedPhoto by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxWidth().padding(24.dp).verticalScroll(rememberScrollState())) {
        Text("Nueva Zona de Cultivo", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(20.dp))
        
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre de la zona") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
        Spacer(modifier = Modifier.height(12.dp))
        
        OutlinedTextField(value = crop, onValueChange = { crop = it }, label = { Text("Tipo de Cultivo") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = hectares, onValueChange = { hectares = it }, label = { Text("Hectáreas") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp))
            OutlinedTextField(value = plantingDate, onValueChange = { plantingDate = it }, label = { Text("F. Siembra") }, placeholder = { Text("dd/mm/aaaa") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp))
        }
        Spacer(modifier = Modifier.height(12.dp))
        
        OutlinedTextField(value = days, onValueChange = { days = it }, label = { Text("Días cosecha") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(value = observations, onValueChange = { observations = it }, label = { Text("Observaciones") }, modifier = Modifier.fillMaxWidth().height(100.dp), shape = RoundedCornerShape(16.dp))
        Spacer(modifier = Modifier.height(16.dp))

        Text("Estado del Cultivo (Vía IA)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF007AFF))
        Spacer(modifier = Modifier.height(8.dp))
        
        Surface(
            modifier = Modifier.fillMaxWidth().height(120.dp).clickable { capturedPhoto = "simulated_uri" },
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFF2F4F7),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (capturedPhoto == null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = Color.Gray)
                        Text("Tomar foto para análisis", fontSize = 12.sp, color = Color.Gray)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2ECC71))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Foto capturada y analizada", color = Color(0xFF2ECC71), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = {
                if (name.isNotBlank() && crop.isNotBlank()) {
                    onAdd(ZoneInfo(
                        name = name, 
                        crop = crop, 
                        color = Color(0xFF007AFF), 
                        daysToHarvest = days.toIntOrNull() ?: 30, 
                        location = LatLng(-14.0, -75.7),
                        hectares = hectares.toDoubleOrNull() ?: 0.0,
                        plantingDate = plantingDate,
                        observations = observations,
                        cropStatus = if (capturedPhoto != null) "Analizado (IA)" else "Pendiente",
                        photoUri = capturedPhoto
                    ))
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text("Guardar Zona", fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun ZoneGrid(zones: List<ZoneInfo>, onZoneClick: (ZoneInfo) -> Unit, onEdit: (ZoneInfo) -> Unit, onDelete: (ZoneInfo) -> Unit) {
    Column {
        zones.chunked(2).forEach { pair ->
            Row(modifier = Modifier.fillMaxWidth()) {
                pair.forEach { zone ->
                    Box(modifier = Modifier.weight(1f)) {
                        ZoneCard(zone, onClick = { onZoneClick(zone) }, onEdit = { onEdit(zone) }, onDelete = { onDelete(zone) })
                    }
                }
                if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun ZoneCard(zone: ZoneInfo, onClick: () -> Unit, onEdit: (ZoneInfo) -> Unit, onDelete: (ZoneInfo) -> Unit) {
    Surface(
        shape = RoundedCornerShape(28.dp), 
        color = Color.White, 
        modifier = Modifier
            .padding(6.dp)
            .fillMaxWidth()
            .height(180.dp)
            .clickable { onClick() }, 
        shadowElevation = 2.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // BOTONES SUPERIORES (Editar izquierda, Eliminar derecha)
            IconButton(
                onClick = { onEdit(zone) },
                modifier = Modifier.align(Alignment.TopStart).padding(4.dp).size(36.dp)
            ) {
                Surface(shape = CircleShape, color = Color(0xFFF2F4F7), modifier = Modifier.fillMaxSize()) {
                    Icon(Icons.Default.Edit, null, tint = Color.Gray, modifier = Modifier.padding(8.dp).size(16.dp))
                }
            }

            IconButton(
                onClick = { onDelete(zone) },
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(36.dp)
            ) {
                Surface(shape = CircleShape, color = Color(0xFFFFEBEE), modifier = Modifier.fillMaxSize()) {
                    Icon(Icons.Default.Delete, null, tint = Color(0xFFE57373), modifier = Modifier.padding(8.dp).size(16.dp))
                }
            }

            // CONTENIDO
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(shape = CircleShape, color = zone.color.copy(alpha = 0.15f), modifier = Modifier.size(56.dp)) {
                    Box(contentAlignment = Alignment.Center) { 
                        Icon(Icons.Default.LocationOn, null, tint = zone.color, modifier = Modifier.size(32.dp)) 
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = zone.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                Text(text = zone.crop, fontSize = 13.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun ZoneDetailContent(zone: ZoneInfo) {
    val cameraPositionState = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(zone.location, 16f) }
    Column(modifier = Modifier.fillMaxWidth().padding(24.dp).verticalScroll(rememberScrollState())) {
        Text(text = "Detalles de ${zone.name}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Text(text = "Cultivo: ${zone.crop}", fontSize = 16.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(24.dp))
        
        Surface(modifier = Modifier.fillMaxWidth().height(220.dp), shape = RoundedCornerShape(24.dp), color = Color.LightGray) {
            // Nota: Si no hay API Key válida, esto podría causar problemas visuales.
            GoogleMap(
                modifier = Modifier.fillMaxSize(), 
                cameraPositionState = cameraPositionState, 
                uiSettings = MapUiSettings(zoomControlsEnabled = false)
            ) {
                Marker(state = MarkerState(position = zone.location), title = zone.name)
                Circle(
                    center = zone.location,
                    radius = (zone.hectares * 10.0).coerceAtLeast(50.0),
                    fillColor = zone.color.copy(alpha = 0.3f),
                    strokeColor = zone.color,
                    strokeWidth = 2f
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            InfoBox("Hectáreas", "${zone.hectares} ha", Color(0xFF007AFF))
            InfoBox("F. Siembra", zone.plantingDate.ifEmpty { "--" }, Color(0xFF8E9196))
            InfoBox("Días restantes", "${zone.daysToHarvest}", zone.color)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = Color(0xFFF2F4F7)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Estado del Cultivo (IA)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1C1E))
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = zone.cropStatus, fontSize = 16.sp, color = if (zone.cropStatus.contains("Saludable")) Color(0xFF2ECC71) else Color(0xFF007AFF), fontWeight = FontWeight.SemiBold)
                
                if (zone.observations.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Observaciones", fontSize = 12.sp, color = Color.Gray)
                    Text(text = zone.observations, fontSize = 14.sp, color = Color.Black)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = {}, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(28.dp)) {
            Text("Actualizar Análisis IA", fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun InfoBox(label: String, value: String, accentColor: Color) {
    Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFF2F4F7), modifier = Modifier.width(100.dp)) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, fontSize = 11.sp, color = Color.Gray)
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = accentColor)
        }
    }
}

@Composable
fun WeatherCard(data: WeatherData, expanded: Boolean, onToggle: () -> Unit) {
    val currentDate = remember { SimpleDateFormat("dd MMMM yyyy", Locale.forLanguageTag("es-PE")).format(Date()) }
    Surface(
        shape = RoundedCornerShape(28.dp), 
        color = Color(0xFF007AFF), 
        contentColor = Color.White, 
        modifier = Modifier.fillMaxWidth().clickable { onToggle() }
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(text = "Condiciones Actuales", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                    Text(text = "${data.location} - $currentDate", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, 
                    contentDescription = null, 
                    modifier = Modifier.size(24.dp)
                )
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    WeatherInfoItem(Icons.Default.DeviceThermostat, data.temp)
                    WeatherInfoItem(Icons.Default.WaterDrop, data.humidity)
                    WeatherInfoItem(Icons.Default.Air, data.wind)
                    WeatherInfoItem(Icons.Default.WbSunny, data.uv)
                    WeatherInfoItem(Icons.Default.CloudQueue, data.rain)
                }
            }
        }
    }
}

@Composable
fun WeatherInfoItem(icon: ImageVector, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun AlertCard(title: String, subtitle: String, icon: ImageVector, containerColor: Color, contentColor: Color) {
    Surface(shape = RoundedCornerShape(24.dp), color = containerColor, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, color = contentColor, fontSize = 15.sp)
                Text(text = subtitle, color = contentColor.copy(alpha = 0.8f), fontSize = 13.sp)
            }
        }
    }
}
