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
import com.tecsup.agroscan.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

@Composable
fun AIScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    
    var isAnalyzing by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf<String?>(null) }
    var distanceStatus by remember { mutableStateOf("ideal") }
    
    // Referencia para capturar la imagen
    val imageCapture = remember { ImageCapture.Builder().build() }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (viewModel.isCameraGranted) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                    val executor = Executors.newSingleThreadExecutor()

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        imageAnalysis.setAnalyzer(executor) { imageProxy ->
                            // Lógica de proximidad (simulada por ahora)
                            imageProxy.close()
                        }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner, 
                                cameraSelector, 
                                preview, 
                                imageCapture, 
                                imageAnalysis
                            )
                        } catch (e: Exception) {
                            Log.e("AIScreen", "Error binding camera", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
            
            LaunchedEffect(Unit) {
                while(true) {
                    delay(3000)
                    distanceStatus = listOf("lejos", "cerca", "ideal").random()
                }
            }
        }

        // Overlay y UI
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val borderColor = when(distanceStatus) {
                "ideal" -> Color(0xFF2ECC71)
                "cerca" -> Color(0xFFFFB300)
                else -> Color(0xFFE57373)
            }
            Surface(
                modifier = Modifier.size(280.dp),
                color = Color.Transparent,
                shape = RoundedCornerShape(32.dp),
                border = androidx.compose.foundation.BorderStroke(3.dp, borderColor.copy(alpha = 0.8f))
            ) {
                if (isAnalyzing) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        HorizontalDivider(color = Color(0xFF007AFF), thickness = 2.dp, modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
                    }
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(32.dp))
            DistanceBadge(distanceStatus)
            Spacer(modifier = Modifier.weight(1f))

            if (resultText != null) {
                ResultPanel(resultText!!, onDismiss = { resultText = null })
            }

            Spacer(modifier = Modifier.height(24.dp))

            ScanButton(
                isAnalyzing = isAnalyzing, 
                enabled = distanceStatus == "ideal" && viewModel.isCameraGranted,
                onClick = { 
                    isAnalyzing = true
                    captureAndAnalyze(context, imageCapture, viewModel) { result ->
                        resultText = result
                        isAnalyzing = false
                    }
                }
            )
            
            if (!viewModel.isCameraGranted) {
                Text("Habilite cámara en Ajustes", color = Color.White, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

/**
 * Captura la foto y la envía a Roboflow
 */
private fun captureAndAnalyze(
    context: android.content.Context,
    imageCapture: ImageCapture,
    viewModel: MainViewModel,
    onFinish: (String) -> Unit
) {
    imageCapture.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val bitmap = imageProxyToBitmap(image)
                val base64 = bitmapToBase64(bitmap)
                viewModel.analyzePlantImage(base64) { result ->
                    onFinish(result)
                }
                image.close()
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("AIScreen", "Error captura", exception)
                onFinish("Error al capturar la imagen.")
            }
        }
    )
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
    val buffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}

private fun bitmapToBase64(bitmap: Bitmap): String {
    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
    return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
}

@Composable
fun DistanceBadge(status: String) {
    val (text, color) = when(status) {
        "ideal" -> "¡Perfecto!" to Color(0xFF2ECC71)
        "cerca" -> "Demasiado Cerca" to Color(0xFFFFB300)
        else -> "Acérquese más" to Color(0xFFE57373)
    }
    Surface(shape = RoundedCornerShape(24.dp), color = color) {
        Text(text, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ResultPanel(result: String, onDismiss: () -> Unit) {
    Surface(shape = RoundedCornerShape(28.dp), color = Color.White, modifier = Modifier.fillMaxWidth(), shadowElevation = 8.dp) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF007AFF))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Detección IA", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(result, color = Color.DarkGray)
        }
    }
}

@Composable
fun ScanButton(isAnalyzing: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled && !isAnalyzing,
        modifier = Modifier.size(80.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = if (enabled) Color(0xFF007AFF) else Color.Gray.copy(alpha = 0.5f)),
        contentPadding = PaddingValues(0.dp)
    ) {
        if (isAnalyzing) CircularProgressIndicator(color = Color.White)
        else Icon(Icons.Default.Camera, null, modifier = Modifier.size(36.dp))
    }
}
