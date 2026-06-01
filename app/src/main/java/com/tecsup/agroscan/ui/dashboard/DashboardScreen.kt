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
import com.tecsup.agroscan.ui.profile.ProfileScreen
import com.tecsup.agroscan.viewmodel.MainViewModel
import com.tecsup.agroscan.data.ZoneInfo
import com.tecsup.agroscan.data.WeatherData
import com.google.android.gms.maps.model.LatLng
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.ui.graphics.Brush

/**
 * Pantalla Principal del Panel de Control (Dashboard).
 * Gestiona la navegación y muestra el contenido principal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onLocationRequest: () -> Unit = {},
    onCameraRequest: () -> Unit = {}
) {
    var itemSeleccionado by remember { mutableIntStateOf(0) }
    var zonaSeleccionada by remember { mutableStateOf<ZoneInfo?>(null) }
    var mostrarHojaDetalle by remember { mutableStateOf(false) }
    var mostrarHojaAgregar by remember { mutableStateOf(false) }
    var mostrarHojaEditar by remember { mutableStateOf(false) }
    val estadoHojaDetalle = rememberModalBottomSheetState()
    val estadoHojaAgregar = rememberModalBottomSheetState()
    val estadoHojaEditar = rememberModalBottomSheetState()

    val colorFondo = MaterialTheme.colorScheme.background

    Box(modifier = Modifier.fillMaxSize().background(colorFondo)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                when (itemSeleccionado) {
                    0 -> ContenidoPrincipalDashboard(
                        locationEnabled = viewModel.isLocationGranted,
                        cityName = viewModel.userCity,
                        zones = viewModel.zones,
                        realWeather = viewModel.realWeatherData,
                        onZoneClick = {
                            zonaSeleccionada = it
                            mostrarHojaDetalle = true
                        },
                        onAddClick = { mostrarHojaAgregar = true },
                        onEditZone = {
                            zonaSeleccionada = it
                            mostrarHojaEditar = true
                        },
                        onDeleteZone = { viewModel.removeZone(it) }
                    )
                    1 -> AIScreen(viewModel = viewModel)
                    2 -> SearchScreen(viewModel = viewModel)
                    3 -> ProfileScreen(viewModel = viewModel)
                }
            }
        }

        // --- DEGRADADOS ---
        Box(modifier = Modifier.fillMaxWidth().height(80.dp).background(Brush.verticalGradient(colors = listOf(colorFondo, Color.Transparent))).align(Alignment.TopCenter))
        Box(modifier = Modifier.fillMaxWidth().height(180.dp).align(Alignment.BottomCenter).background(Brush.verticalGradient(colors = listOf(Color.Transparent, colorFondo.copy(alpha = 0.5f), colorFondo))))

        // --- BARRA DE NAVEGACIÓN ---
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 12.dp).fillMaxWidth().height(72.dp),
            shape = RoundedCornerShape(36.dp),
            color = Color.White.copy(alpha = 0.98f),
            shadowElevation = 10.dp
        ) {
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                val itemsNav = listOf("Inicio", "IA", "Buscar", "Perfil")
                val iconos = listOf(Icons.Default.Home, Icons.Default.AutoAwesome, Icons.Default.Search, Icons.Default.Person)
                itemsNav.forEachIndexed { index, item ->
                    val estaSeleccionado = itemSeleccionado == index
                    Column(modifier = Modifier.weight(1f).clickable { itemSeleccionado = index }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(imageVector = iconos[index], contentDescription = item, tint = if (estaSeleccionado) Color(0xFF007AFF) else Color(0xFF8E9196), modifier = Modifier.size(if (estaSeleccionado) 28.dp else 24.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = item, fontSize = 11.sp, fontWeight = if (estaSeleccionado) FontWeight.Bold else FontWeight.Medium, color = if (estaSeleccionado) Color(0xFF007AFF) else Color(0xFF8E9196))
                    }
                }
            }
        }

        // --- HOJAS INFERIORES (BOTTOM SHEETS) ---
        if (mostrarHojaDetalle && zonaSeleccionada != null) {
            ModalBottomSheet(
                onDismissRequest = { mostrarHojaDetalle = false },
                sheetState = estadoHojaDetalle,
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            ) {
                ContenidoDetalleZona(zonaSeleccionada!!)
            }
        }

        if (mostrarHojaAgregar) {
            ModalBottomSheet(
                onDismissRequest = { mostrarHojaAgregar = false },
                sheetState = estadoHojaAgregar,
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            ) {
                FormularioAgregarZona(onAdd = { nuevaZona ->
                    viewModel.addZone(nuevaZona)
                    mostrarHojaAgregar = false
                })
            }
        }

        if (mostrarHojaEditar && zonaSeleccionada != null) {
            ModalBottomSheet(
                onDismissRequest = { mostrarHojaEditar = false },
                sheetState = estadoHojaEditar,
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            ) {
                FormularioEditarZona(
                    zona = zonaSeleccionada!!,
                    onUpdate = { zonaActualizada ->
                        viewModel.updateZone(zonaSeleccionada!!, zonaActualizada)
                        mostrarHojaEditar = false
                    }
                )
            }
        }
    }
}

@Composable
fun ContenidoPrincipalDashboard(
    locationEnabled: Boolean,
    cityName: String,
    zones: List<ZoneInfo>,
    realWeather: WeatherData? = null,
    onZoneClick: (ZoneInfo) -> Unit,
    onAddClick: () -> Unit,
    onEditZone: (ZoneInfo) -> Unit,
    onDeleteZone: (ZoneInfo) -> Unit
) {
    val proximaZonaCosecha = zones.filter { it.daysToHarvest <= 30 }.minByOrNull { it.daysToHarvest }
    val totalHectareas = zones.sumOf { it.hectares }.toInt()

    val datosClima = remember(locationEnabled, cityName, realWeather) {
        realWeather ?: if (locationEnabled) {
            WeatherData(location = cityName, temp = "26°C", humidity = "62%", wind = "14km/h", uv = "UV 7", rain = "0.0mm")
        } else {
            WeatherData(location = cityName, temp = "--°C", humidity = "--%", wind = "--km/h", uv = "UV --", rain = "--mm")
        }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
        Spacer(modifier = Modifier.height(32.dp))
        Text(text = "Monitoreo de Cultivos", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Text(text = "$totalHectareas hectáreas en monitoreo activo", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(24.dp))
        TarjetaClima(datosClima)

        if (!locationEnabled) {
            Spacer(modifier = Modifier.height(12.dp))
            TarjetaAlerta(title = "Ubicación requerida", subtitle = "Activa GPS para datos locales", icon = Icons.Default.LocationOff, containerColor = Color(0xFFFDE8E8), contentColor = Color(0xFFC81E1E))
        }

        proximaZonaCosecha?.let { zona ->
            Spacer(modifier = Modifier.height(16.dp))
            TarjetaAlerta(title = "Cosecha Óptima Próxima", subtitle = "${zona.name} lista en ${zona.daysToHarvest} días", icon = Icons.Default.WarningAmber, containerColor = Color(0xFFFFF4E5), contentColor = Color(0xFF855300))
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
            Text(text = "Actualizado ahora", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(16.dp))
        CuadriculaZonas(zones, onZoneClick, onEditZone, onDeleteZone)
        
        // --- ESPACIO EXTRA PARA EL SCROLL ---
        Spacer(modifier = Modifier.height(140.dp))
    }
}

@Composable
fun FormularioAgregarZona(onAdd: (ZoneInfo) -> Unit) {
    var nombre by remember { mutableStateOf("") }
    var cultivo by remember { mutableStateOf("") }
    var dias by remember { mutableStateOf("") }
    var hectareas by remember { mutableStateOf("") }
    var fechaSiembra by remember { mutableStateOf("") }
    var observaciones by remember { mutableStateOf("") }
    var fotoCapturada by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxWidth().padding(24.dp).verticalScroll(rememberScrollState())) {
        Text("Nueva Zona de Cultivo", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(20.dp))
        
        OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre de la zona") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
        Spacer(modifier = Modifier.height(12.dp))
        
        OutlinedTextField(value = cultivo, onValueChange = { cultivo = it }, label = { Text("Tipo de Cultivo") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = hectareas, onValueChange = { hectareas = it }, label = { Text("Hectáreas") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp))
            OutlinedTextField(value = fechaSiembra, onValueChange = { fechaSiembra = it }, label = { Text("F. Siembra") }, placeholder = { Text("dd/mm/aaaa") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp))
        }
        Spacer(modifier = Modifier.height(12.dp))
        
        OutlinedTextField(value = dias, onValueChange = { dias = it }, label = { Text("Días cosecha") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(value = observaciones, onValueChange = { observaciones = it }, label = { Text("Observaciones") }, modifier = Modifier.fillMaxWidth().height(100.dp), shape = RoundedCornerShape(16.dp))
        Spacer(modifier = Modifier.height(16.dp))

        // Sección de Fotografía e IA
        Text("Estado del Cultivo (Vía IA)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF007AFF))
        Spacer(modifier = Modifier.height(8.dp))
        
        Surface(
            modifier = Modifier.fillMaxWidth().height(120.dp).clickable { fotoCapturada = "simulated_uri" },
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFF2F4F7),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (fotoCapturada == null) {
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
                if (nombre.isNotBlank() && cultivo.isNotBlank()) {
                    onAdd(ZoneInfo(
                        name = nombre, 
                        crop = cultivo, 
                        color = Color(0xFF007AFF), 
                        daysToHarvest = dias.toIntOrNull() ?: 30, 
                        location = LatLng(-14.0, -75.7),
                        hectares = hectareas.toDoubleOrNull() ?: 0.0,
                        plantingDate = fechaSiembra,
                        observations = observaciones,
                        cropStatus = if (fotoCapturada != null) "Analizado (IA)" else "Pendiente",
                        photoUri = fotoCapturada
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
fun CuadriculaZonas(zones: List<ZoneInfo>, onZoneClick: (ZoneInfo) -> Unit, onEdit: (ZoneInfo) -> Unit, onDelete: (ZoneInfo) -> Unit) {
    Column {
        zones.chunked(2).forEach { pair ->
            Row(modifier = Modifier.fillMaxWidth()) {
                pair.forEach { zona ->
                    Box(modifier = Modifier.weight(1f)) {
                        TarjetaZona(zona, onClick = { onZoneClick(zona) }, onEdit = { onEdit(zona) }, onDelete = { onDelete(zona) })
                    }
                }
                if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun TarjetaZona(zona: ZoneInfo, onClick: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(28.dp), 
        color = Color.White, 
        modifier = Modifier
            .padding(6.dp)
            .fillMaxWidth()
            .height(200.dp) // Aumentado para evitar colisiones
            .clickable { onClick() }, 
        shadowElevation = 2.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // --- BOTONES DE ACCIÓN (Corregidos y Separados) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFF2F4F7),
                    modifier = Modifier.size(36.dp).clickable { onEdit() }
                ) {
                    Icon(
                        Icons.Default.Edit, 
                        contentDescription = "Editar", 
                        tint = Color.Gray, 
                        modifier = Modifier.padding(8.dp).size(20.dp)
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFFEBEE),
                    modifier = Modifier.size(36.dp).clickable { onDelete() }
                ) {
                    Icon(
                        Icons.Default.Delete, 
                        contentDescription = "Eliminar", 
                        tint = Color(0xFFE57373), 
                        modifier = Modifier.padding(8.dp).size(20.dp)
                    )
                }
            }

            // --- CONTENIDO CENTRAL (Bajado para evitar overlap) ---
            Column(
                modifier = Modifier.fillMaxSize().padding(top = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = CircleShape, 
                    color = zona.color.copy(alpha = 0.15f), 
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) { 
                        Icon(
                            Icons.Default.LocationOn, 
                            contentDescription = null, 
                            tint = zona.color,
                            modifier = Modifier.size(28.dp)
                        ) 
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = zona.name, 
                    fontWeight = FontWeight.ExtraBold, 
                    fontSize = 16.sp, 
                    color = Color(0xFF1A1C1E)
                )
                Text(
                    text = zona.crop, 
                    fontSize = 13.sp, 
                    color = Color(0xFF8E9196)
                )
            }
        }
    }
}

@Composable
fun FormularioEditarZona(zona: ZoneInfo, onUpdate: (ZoneInfo) -> Unit) {
    var nombre by remember { mutableStateOf(zona.name) }
    var cultivo by remember { mutableStateOf(zona.crop) }
    var dias by remember { mutableStateOf(zona.daysToHarvest.toString()) }
    var hectareas by remember { mutableStateOf(zona.hectares.toString()) }
    var fechaSiembra by remember { mutableStateOf(zona.plantingDate) }
    var observaciones by remember { mutableStateOf(zona.observations) }

    Column(modifier = Modifier.fillMaxWidth().padding(24.dp).verticalScroll(rememberScrollState())) {
        Text("Editar Zona", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(20.dp))
        
        OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre de la zona") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
        Spacer(modifier = Modifier.height(12.dp))
        
        OutlinedTextField(value = cultivo, onValueChange = { cultivo = it }, label = { Text("Tipo de Cultivo") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = hectareas, onValueChange = { hectareas = it }, label = { Text("Hectáreas") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp))
            OutlinedTextField(value = fechaSiembra, onValueChange = { fechaSiembra = it }, label = { Text("F. Siembra") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp))
        }
        Spacer(modifier = Modifier.height(12.dp))
        
        OutlinedTextField(value = dias, onValueChange = { dias = it }, label = { Text("Días cosecha") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(value = observaciones, onValueChange = { observaciones = it }, label = { Text("Observaciones") }, modifier = Modifier.fillMaxWidth().height(100.dp), shape = RoundedCornerShape(16.dp))
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = {
                onUpdate(zona.copy(
                    name = nombre,
                    crop = cultivo,
                    daysToHarvest = dias.toIntOrNull() ?: zona.daysToHarvest,
                    hectares = hectareas.toDoubleOrNull() ?: zona.hectares,
                    plantingDate = fechaSiembra,
                    observations = observaciones
                ))
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text("Actualizar Zona", fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun ContenidoDetalleZona(zona: ZoneInfo) {
    val contexto = LocalContext.current
    
    // Configuración obligatoria para OSMDroid
    Configuration.getInstance().userAgentValue = contexto.packageName

    Column(modifier = Modifier.fillMaxWidth().padding(24.dp).verticalScroll(rememberScrollState())) {
        Text(text = "Detalles de ${zona.name}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Text(text = "Cultivo: ${zona.crop}", fontSize = 16.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(24.dp))
        
        Surface(modifier = Modifier.fillMaxWidth().height(220.dp), shape = RoundedCornerShape(24.dp), color = Color.LightGray) {
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(16.0)
                        val puntoInicio = GeoPoint(zona.location.latitude, zona.location.longitude)
                        controller.setCenter(puntoInicio)
                        
                        // Marcador
                        val marcador = Marker(this)
                        marcador.position = puntoInicio
                        marcador.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        marcador.title = zona.name
                        overlays.add(marcador)

                        // Simular círculo (zona de cultivo)
                        val puntosCirculo = ArrayList<GeoPoint>()
                        val radio = (zona.hectares * 10.0).coerceAtLeast(50.0)
                        for (i in 0 until 360) {
                            puntosCirculo.add(GeoPoint(puntoInicio).destinationPoint(radio, i.toDouble()))
                        }
                        val circulo = Polygon(this)
                        circulo.points = puntosCirculo
                        circulo.fillPaint.color = android.graphics.Color.argb(77, 
                            (zona.color.red * 255).toInt(), 
                            (zona.color.green * 255).toInt(), 
                            (zona.color.blue * 255).toInt())
                        circulo.outlinePaint.color = android.graphics.Color.rgb(
                            (zona.color.red * 255).toInt(), 
                            (zona.color.green * 255).toInt(), 
                            (zona.color.blue * 255).toInt())
                        circulo.outlinePaint.strokeWidth = 2f
                        overlays.add(circulo)
                        
                        invalidate()
                    }
                },
                update = { mapView ->
                    val puntoInicio = GeoPoint(zona.location.latitude, zona.location.longitude)
                    mapView.controller.setCenter(puntoInicio)
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        
        // Info de cosecha y hectáreas
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            InfoBox("Hectáreas", "${zona.hectares} ha", Color(0xFF007AFF))
            InfoBox("F. Siembra", zona.plantingDate.ifEmpty { "--" }, Color(0xFF8E9196))
            InfoBox("Días restantes", "${zona.daysToHarvest}", zona.color)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Estado e IA
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFFF2F4F7)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Estado del Cultivo (IA)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1C1E))
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = zona.cropStatus, fontSize = 16.sp, color = if (zona.cropStatus.contains("Saludable")) Color(0xFF2ECC71) else Color(0xFF007AFF), fontWeight = FontWeight.SemiBold)
                
                if (zona.observations.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Observaciones", fontSize = 12.sp, color = Color.Gray)
                    Text(text = zona.observations, fontSize = 14.sp, color = Color.Black)
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
fun TarjetaClima(data: WeatherData) {
    val fechaActual = remember { SimpleDateFormat("dd MMMM yyyy", Locale.forLanguageTag("es-PE")).format(Date()) }
    Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFF007AFF), contentColor = Color.White, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(text = "Condiciones Actuales", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                    Text(text = "${data.location} - $fechaActual", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                }
                Icon(Icons.Default.Thermostat, contentDescription = null, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                InfoClimaItem(Icons.Default.DeviceThermostat, data.temp)
                InfoClimaItem(Icons.Default.WaterDrop, data.humidity)
                InfoClimaItem(Icons.Default.Air, data.wind)
                InfoClimaItem(Icons.Default.WbSunny, data.uv)
                InfoClimaItem(Icons.Default.CloudQueue, data.rain)
            }
        }
    }
}

@Composable
fun InfoClimaItem(icon: ImageVector, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun TarjetaAlerta(title: String, subtitle: String, icon: ImageVector, containerColor: Color, contentColor: Color) {
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
