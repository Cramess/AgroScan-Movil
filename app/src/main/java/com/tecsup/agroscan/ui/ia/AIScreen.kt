package com.tecsup.agroscan.ui.ia

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.tecsup.agroscan.data.AnalysisResult
import com.tecsup.agroscan.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

/**
 * Pantalla de Análisis con Inteligencia Artificial.
 * Escanea plantas y detecta enfermedades en tiempo real.
 */
@Composable
fun AIScreen(viewModel: MainViewModel) {
    val contexto = LocalContext.current
    val cicloVida = LocalLifecycleOwner.current
    val camaraProductor = remember { ProcessCameraProvider.getInstance(contexto) }
    
    var analizando by remember { mutableStateOf(false) }
    var textoResultado by remember { mutableStateOf<String?>(null) }
    var estadoDistancia by remember { mutableStateOf("ideal") }
    var flashlightEnabled by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    
    val capturaImagen = remember { ImageCapture.Builder().build() }

    // Controlar linterna cuando cambia el estado o la cámara se vincula
    LaunchedEffect(flashlightEnabled, camera) {
        camera?.cameraControl?.enableTorch(flashlightEnabled)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (viewModel.isCameraGranted) {
            AndroidView(
                factory = { ctx ->
                    val vistaPrevia = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                    val ejecutor = Executors.newSingleThreadExecutor()

                    camaraProductor.addListener({
                        val proveedor = camaraProductor.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(vistaPrevia.surfaceProvider)
                        }

                        val analisisImagen = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        analisisImagen.setAnalyzer(ejecutor) { frame ->
                            // Futura lógica de proximidad con YOLOv8
                            frame.close()
                        }

                        val selector = CameraSelector.DEFAULT_BACK_CAMERA
                        try {
                            proveedor.unbindAll()
                            camera = proveedor.bindToLifecycle(
                                cicloVida, 
                                selector, 
                                preview, 
                                capturaImagen, 
                                analisisImagen
                            )
                        } catch (e: Exception) {
                            Log.e("AIScreen", "Error vinculando cámara", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    vistaPrevia
                },
                modifier = Modifier.fillMaxSize()
            )
            
            LaunchedEffect(Unit) {
                while(true) {
                    delay(3000)
                    estadoDistancia = listOf("lejos", "cerca", "ideal").random()
                }
            }
        }

        // --- INTERFAZ DE USUARIO (OVERLAY) ---
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val colorBorde = when(estadoDistancia) {
                "ideal" -> Color(0xFF2ECC71)
                "cerca" -> Color(0xFFFFB300)
                else -> Color(0xFFE57373)
            }
            Surface(
                modifier = Modifier.size(280.dp),
                color = Color.Transparent,
                shape = RoundedCornerShape(32.dp),
                border = androidx.compose.foundation.BorderStroke(3.dp, colorBorde.copy(alpha = 0.8f))
            ) {
                if (analizando) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        HorizontalDivider(color = Color(0xFF007AFF), thickness = 2.dp, modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
                    }
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = { flashlightEnabled = !flashlightEnabled },
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (flashlightEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Linterna",
                        tint = if (flashlightEnabled) Color.Yellow else Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            EtiquetaDistancia(estadoDistancia)
            Spacer(modifier = Modifier.weight(1f))

            if (textoResultado != null) {
                PanelResultado(textoResultado!!, alCerrar = { textoResultado = null })
            }

            Spacer(modifier = Modifier.height(24.dp))

            BotonEscanear(
                estaAnalizando = analizando, 
                habilitado = estadoDistancia == "ideal" && viewModel.isCameraGranted,
                alClickear = { 
                    analizando = true
                    capturarYAnalizar(contexto, capturaImagen, viewModel) { resultado ->
                        textoResultado = resultado
                        analizando = false
                    }
                }
            )
            
            if (!viewModel.isCameraGranted) {
                Text("Active la cámara en Ajustes", color = Color.White, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

private fun capturarYAnalizar(
    contexto: android.content.Context,
    captura: ImageCapture,
    viewModel: MainViewModel,
    alFinalizar: (String) -> Unit
) {
    captura.takePicture(
        ContextCompat.getMainExecutor(contexto),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(imagen: ImageProxy) {
                val bitmap = proxyABitmap(imagen)
                val base64 = bitmapABase64(bitmap)
                viewModel.analyzePlantImage(base64) { resultado ->
                    alFinalizar(resultado)
                }
                imagen.close()
            }

            override fun onError(error: ImageCaptureException) {
                Log.e("AIScreen", "Error captura", error)
                alFinalizar("Error al capturar imagen")
            }
        }
    )
}

private fun proxyABitmap(image: ImageProxy): Bitmap {
    val buffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}

private fun bitmapABase64(bitmap: Bitmap): String {
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream)
    return Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT)
}

@Composable
fun EtiquetaDistancia(estado: String) {
    val texto = when(estado) {
        "ideal" -> "¡Perfecto!"
        "cerca" -> "Demasiado Cerca"
        else -> "Acérquese más"
    }
    val color = when(estado) {
        "ideal" -> Color(0xFF2ECC71)
        "cerca" -> Color(0xFFFFB300)
        else -> Color(0xFFE57373)
    }
    Surface(shape = RoundedCornerShape(24.dp), color = color) {
        Text(texto, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PanelResultado(resultado: String, alCerrar: () -> Unit) {
    Surface(shape = RoundedCornerShape(28.dp), color = Color.White, modifier = Modifier.fillMaxWidth(), shadowElevation = 8.dp) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF007AFF))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Detección IA", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = alCerrar) { Icon(Icons.Default.Close, null) }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(resultado, color = Color.DarkGray)
        }
    }
}

@Composable
fun BotonEscanear(estaAnalizando: Boolean, habilitado: Boolean, alClickear: () -> Unit) {
    Button(
        onClick = alClickear,
        enabled = habilitado && !estaAnalizando,
        modifier = Modifier.size(80.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = if (habilitado) Color(0xFF007AFF) else Color.Gray.copy(alpha = 0.5f)),
        contentPadding = PaddingValues(0.dp)
    ) {
        if (estaAnalizando) CircularProgressIndicator(color = Color.White)
        else Icon(Icons.Default.Camera, null, modifier = Modifier.size(36.dp))
    }
}
