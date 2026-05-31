package com.tecsup.agroscan.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tecsup.agroscan.viewmodel.MainViewModel

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onLocationToggle: () -> Unit = {},
    onCameraToggle: () -> Unit = {}
) {
    val user = viewModel.currentUser

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Ajustes",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Datos Reales del Usuario
        ProfileHeader(
            name = user?.nombre ?: "Invitado",
            role = user?.rol ?: "Sin Rol",
            id = "Empresa ID: ${user?.empresaId ?: 0}"
        )

        Spacer(modifier = Modifier.height(32.dp))

        SettingsGroup(title = "Aplicación y Permisos") {
            SettingsItem(
                icon = Icons.Outlined.LocationOn,
                title = "Ubicación",
                subtitle = if (viewModel.isLocationGranted) "Permiso concedido" else "Activar GPS",
                trailing = {
                    Switch(
                        checked = viewModel.isLocationGranted, 
                        onCheckedChange = { onLocationToggle() },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF007AFF))
                    )
                }
            )
            SettingsItem(
                icon = Icons.Outlined.CameraAlt,
                title = "Cámara",
                subtitle = if (viewModel.isCameraGranted) "Permiso activo" else "Requerido para IA",
                trailing = {
                    Switch(
                        checked = viewModel.isCameraGranted, 
                        onCheckedChange = { onCameraToggle() },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF007AFF))
                    )
                }
            )
            SettingsItem(
                icon = Icons.Outlined.DarkMode,
                title = "Tema",
                subtitle = "Claro / Oscuro",
                trailing = {
                    Switch(
                        checked = viewModel.isDarkTheme, 
                        onCheckedChange = { viewModel.toggleTheme(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF007AFF))
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsGroup(title = "Cuenta y Privacidad") {
            SettingsItem(icon = Icons.Outlined.Lock, title = "Privacidad", subtitle = "Gestión de datos", onClick = { })
            SettingsItem(icon = Icons.AutoMirrored.Filled.HelpOutline, title = "Soporte", onClick = { })
            SettingsItem(
                icon = Icons.AutoMirrored.Filled.Logout, 
                title = "Cerrar Sesión", 
                titleColor = Color.Red, 
                onClick = { viewModel.logout() } // Logout Real
            )
        }
        
        Spacer(modifier = Modifier.height(120.dp)) // Espacio para el Dock flotante
    }
}

@Composable
fun ProfileHeader(name: String, role: String, id: String) {
    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth(), shadowElevation = 1.dp) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                Surface(shape = CircleShape, color = Color(0xFFE5F1FF), modifier = Modifier.size(80.dp)) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.padding(16.dp).size(48.dp), tint = Color(0xFF007AFF))
                }
                Surface(shape = CircleShape, color = Color(0xFF007AFF), modifier = Modifier.size(24.dp).align(Alignment.BottomEnd), border = androidx.compose.foundation.BorderStroke(2.dp, Color.White)) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.padding(4.dp))
                }
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text(text = name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(text = role, fontSize = 14.sp, color = Color(0xFF007AFF), fontWeight = FontWeight.Medium)
                Text(text = id, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF007AFF), modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth(), shadowElevation = 1.dp) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) { content() }
        }
    }
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, subtitle: String? = null, titleColor: Color? = null, trailing: @Composable (() -> Unit)? = null, onClick: (() -> Unit)? = null) {
    Row(modifier = Modifier.fillMaxWidth().clickable(enabled = onClick != null) { onClick?.invoke() }.padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(40.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.padding(10.dp), tint = titleColor ?: MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = titleColor ?: MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) { Text(text = subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        if (trailing != null) { trailing() } else if (onClick != null) { Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
