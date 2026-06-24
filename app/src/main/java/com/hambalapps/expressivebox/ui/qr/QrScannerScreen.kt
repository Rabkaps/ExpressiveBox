package com.hambalapps.expressivebox.ui.qr

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.hambalapps.expressivebox.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine


@Composable
fun QrScannerScreen(
    onScanSuccess: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    LaunchedEffect(key1 = true) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
            CameraPreviewScanner(
                onScanSuccess = onScanSuccess,
                onClose = onClose
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.cam_perm_req),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.cam_perm_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { launcher.launch(Manifest.permission.CAMERA) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.grant_perm))
                }
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onClose) {
                    Text(stringResource(R.string.cancel), color = Color.White)
                }
            }
        }
    }
}

@Composable
fun CameraPreviewScanner(
    onScanSuccess: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    
    var camera by remember { mutableStateOf<Camera?>(null) }
    var flashEnabled by remember { mutableStateOf(false) }
    
    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    var activeCameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    val options = remember {
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    }
    val scanner = remember { BarcodeScanning.getClient(options) }

    DisposableEffect(activeCameraProvider) {
        onDispose {
            try {
                activeCameraProvider?.unbindAll()
            } catch (e: Exception) {
                // Ignore
            }
            cameraExecutor.shutdown()
            try {
                scanner.close()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Scanner overlay HUD (finder box + scanning line)
        ScannerOverlayHUD()

        // Control Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.5f)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }

            IconButton(
                onClick = {
                    val currentCamera = camera
                    if (currentCamera != null && currentCamera.cameraInfo.hasFlashUnit()) {
                        flashEnabled = !flashEnabled
                        currentCamera.cameraControl.enableTorch(flashEnabled)
                    } else {
                        Toast.makeText(context, context.getString(R.string.flash_not_avail), Toast.LENGTH_SHORT).show()
                    }
                },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.5f)
                )
            ) {
                Icon(
                    imageVector = if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Toggle Flash",
                    tint = Color.White
                )
            }
        }

        // Setup CameraX Lifecycle
        LaunchedEffect(cameraProviderFuture) {
            val cameraProvider = suspendCancellableCoroutine<ProcessCameraProvider> { continuation ->
                cameraProviderFuture.addListener(
                    {
                        try {
                            continuation.resume(cameraProviderFuture.get())
                        } catch (e: Exception) {
                            continuation.resumeWithException(e)
                        }
                    },
                    ContextCompat.getMainExecutor(context)
                )
            }
            activeCameraProvider = cameraProvider

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            var isScanningActive = true

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null && isScanningActive) {
                            val image = InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )
                            
                            scanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    for (barcode in barcodes) {
                                        val rawValue = barcode.rawValue
                                        if (rawValue != null && isScanningActive) {
                                            isScanningActive = false
                                            onScanSuccess(rawValue)
                                            break
                                        }
                                    }
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        } else {
                            imageProxy.close()
                        }
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (exc: Exception) {
                Toast.makeText(context, context.getString(R.string.use_cam_fail, exc.message), Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
fun ScannerOverlayHUD() {
    val infiniteTransition = rememberInfiniteTransition(label = "LaserTransition")
    
    val lineOffsetFraction by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LaserOffset"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        // Find square size in the center
        val finderSize = minOf(width, height) * 0.65f
        val finderLeft = (width - finderSize) / 2
        val finderTop = (height - finderSize) / 2
        val finderRight = finderLeft + finderSize
        val finderBottom = finderTop + finderSize

        val finderRect = Rect(finderLeft, finderTop, finderRight, finderBottom)
        val roundRectPath = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = finderRect,
                    cornerRadius = CornerRadius(24.dp.toPx())
                )
            )
        }

        // Draw translucent overlay background outside finder box
        clipPath(path = roundRectPath, clipOp = ClipOp.Difference) {
            drawRect(
                color = Color.Black.copy(alpha = 0.65f)
            )
        }

        // Draw finder box white borders
        val borderThickness = 4.dp.toPx()
        val cornerLength = 24.dp.toPx()
        
        // Draw custom corner highlights in cyan
        val highlightColor = Color(0xFF00E5FF)
        
        // Top-Left corner
        drawRect(color = highlightColor, topLeft = Offset(finderLeft, finderTop), size = androidx.compose.ui.geometry.Size(cornerLength, borderThickness))
        drawRect(color = highlightColor, topLeft = Offset(finderLeft, finderTop), size = androidx.compose.ui.geometry.Size(borderThickness, cornerLength))

        // Top-Right corner
        drawRect(color = highlightColor, topLeft = Offset(finderRight - cornerLength, finderTop), size = androidx.compose.ui.geometry.Size(cornerLength, borderThickness))
        drawRect(color = highlightColor, topLeft = Offset(finderRight - borderThickness, finderTop), size = androidx.compose.ui.geometry.Size(borderThickness, cornerLength))

        // Bottom-Left corner
        drawRect(color = highlightColor, topLeft = Offset(finderLeft, finderBottom - borderThickness), size = androidx.compose.ui.geometry.Size(cornerLength, borderThickness))
        drawRect(color = highlightColor, topLeft = Offset(finderLeft, finderBottom - cornerLength), size = androidx.compose.ui.geometry.Size(borderThickness, cornerLength))

        // Bottom-Right corner
        drawRect(color = highlightColor, topLeft = Offset(finderRight - cornerLength, finderBottom - borderThickness), size = androidx.compose.ui.geometry.Size(cornerLength, borderThickness))
        drawRect(color = highlightColor, topLeft = Offset(finderRight - borderThickness, finderBottom - cornerLength), size = androidx.compose.ui.geometry.Size(borderThickness, cornerLength))

        // Draw animated laser scanning line
        val laserY = finderTop + (finderSize * lineOffsetFraction)
        drawLine(
            color = Color(0xFF00E5FF).copy(alpha = 0.85f),
            start = Offset(finderLeft + 12.dp.toPx(), laserY),
            end = Offset(finderRight - 12.dp.toPx(), laserY),
            strokeWidth = 3.dp.toPx()
        )
    }
}
