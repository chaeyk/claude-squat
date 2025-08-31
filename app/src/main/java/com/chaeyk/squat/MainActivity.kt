package com.chaeyk.squat

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview as CameraPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import com.google.mlkit.vision.pose.Pose
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview as ComposePreview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.chaeyk.squat.ui.theme.SquatTheme
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private lateinit var cameraExecutor: ExecutorService
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            Toast.makeText(this, "권한이 필요합니다", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
        
        setContent {
            SquatTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SquatScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
    
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun startCamera() {
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
    
    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    }
}

@Composable
fun SquatScreen(modifier: Modifier = Modifier) {
    var squatCount by remember { mutableStateOf(0) }
    var currentPose by remember { mutableStateOf<Pose?>(null) }
    var imageSize by remember { mutableStateOf(Pair(640, 480)) }
    var kneeAngles by remember { mutableStateOf("") }
    var squatStatus by remember { mutableStateOf("📱 준비") }
    var debugInfo by remember { mutableStateOf("") }
    
    Box(modifier = modifier.fillMaxSize()) {
        CameraScreen(
            modifier = Modifier.fillMaxSize(),
            onSquatCountChanged = { count -> squatCount = count },
            onPoseDetected = { pose, width, height -> 
                currentPose = pose
                imageSize = Pair(width, height)
                debugInfo = "이미지: ${width}x${height}"
            },
            onKneeAnglesChanged = { angles -> kneeAngles = angles },
            onSquatStatusChanged = { status -> squatStatus = status }
        )
        
        // 자세 오버레이
        if (currentPose != null) {
            PoseOverlay(
                pose = currentPose,
                previewWidth = imageSize.first,
                previewHeight = imageSize.second,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // 스쿼트 카운터 UI
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(32.dp)
                .background(
                    Color.Black.copy(alpha = 0.7f),
                    RoundedCornerShape(16.dp)
                )
                .padding(24.dp)
        ) {
            Text(
                text = "$squatCount",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        
        // 스쿼트 상태 표시 UI
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(16.dp)
                .background(
                    Color.Black.copy(alpha = 0.8f),
                    RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = squatStatus,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        
        // 무릎 각도 표시 UI
        if (kneeAngles.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(
                        Color.Black.copy(alpha = 0.7f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(16.dp)
            ) {
                Text(
                    text = "$kneeAngles\n$debugInfo",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun CameraScreen(
    modifier: Modifier = Modifier,
    onSquatCountChanged: (Int) -> Unit,
    onPoseDetected: (Pose, Int, Int) -> Unit = { _, _, _ -> },
    onKneeAnglesChanged: (String) -> Unit = {},
    onSquatStatusChanged: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = context as LifecycleOwner
    val squatDetector = remember { SquatDetector(onSquatCountChanged, onKneeAnglesChanged, onSquatStatusChanged) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    
    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
        },
        modifier = modifier.fillMaxSize(),
        update = { previewView ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    
                    val preview = CameraPreview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    
                    // 이미지 분석 설정
                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analyzer ->
                            analyzer.setAnalyzer(cameraExecutor) { imageProxy ->
                                try {
                                    val mediaImage = imageProxy.image
                                    if (mediaImage != null) {
                                        val image = InputImage.fromMediaImage(
                                            mediaImage,
                                            imageProxy.imageInfo.rotationDegrees
                                        )
                                        
                                        // ML Kit Pose Detection (기본 모드)
                                        val options = PoseDetectorOptions.Builder()
                                            .setDetectorMode(PoseDetectorOptions.SINGLE_IMAGE_MODE)
                                            .build()
                                        val poseDetector = PoseDetection.getClient(options)
                                        
                                        poseDetector.process(image)
                                            .addOnSuccessListener { pose ->
                                                try {
                                                    onPoseDetected(pose, image.width, image.height)
                                                    squatDetector.analyzePose(pose)
                                                } catch (e: Exception) {
                                                    
                                                }
                                            }
                                            .addOnFailureListener { e ->
                                                
                                            }
                                            .addOnCompleteListener {
                                                imageProxy.close()
                                            }
                                    } else {
                                        imageProxy.close()
                                    }
                                } catch (e: Exception) {
                                    imageProxy.close()
                                }
                            }
                        }
                    
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalyzer
                    )
                    
                } catch (exc: Exception) {
                    
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )
}