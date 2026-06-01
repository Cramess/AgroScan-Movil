package com.tecsup.agroscan.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tecsup.agroscan.viewmodel.MainViewModel

/**
 * Pantalla de Inicio de Sesión (Login).
 * Autentica usuarios y gestiona el acceso mediante JWT.
 */
@Composable
fun LoginScreen(viewModel: MainViewModel, alTenerExito: () -> Unit) {
    var correo by remember { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "AgroScan",
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = (-1).sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Acceso para Usuarios Autorizados",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(56.dp))
            
            OutlinedTextField(
                value = correo,
                onValueChange = { correo = it; mensajeError = null },
                label = { Text("Correo Electrónico") },
                leadingIcon = { Icon(Icons.Default.Person, null, tint = Color(0xFF007AFF)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = clave,
                onValueChange = { clave = it; mensajeError = null },
                label = { Text("Contraseña") },
                leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color(0xFF007AFF)) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                singleLine = true
            )
            
            if (mensajeError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = mensajeError!!, color = Color.Red, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(40.dp))
            
            Button(
                onClick = {
                    cargando = true
                    viewModel.login(correo, clave) { exito ->
                        cargando = false
                        if (exito) alTenerExito()
                        else mensajeError = "Credenciales incorrectas"
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                enabled = !cargando
            ) {
                if (cargando) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("Iniciar Sesión", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            TextButton(onClick = { /* Recuperar */ }) {
                Text("¿Olvidaste tu contraseña?", color = Color(0xFF007AFF))
            }
        }
    }
}
