package com.tanniscoring.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tanniscoring.app.R
import com.tanniscoring.app.serve.MotionServeAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

private const val TAG = "ServeCamera"

/**
 * Optional landscape-compatible camera preview (corner overlay) when 스피드 온.
 * Binds CameraX Preview + ImageAnalysis to [analyzer].
 */
@Composable
fun ServeCameraOverlay(
    enabled: Boolean,
    analyzer: MotionServeAnalyzer?,
    modifier: Modifier = Modifier,
) {
    if (!enabled || analyzer == null) return

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    if (!hasPermission) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(10.dp))
                .background(CourtColors.SurfaceElevated)
                .border(1.dp, CourtColors.Border, RoundedCornerShape(10.dp))
                .padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.serve_camera_permission),
                color = CourtColors.TextMuted,
                fontSize = 10.sp,
            )
        }
        return
    }

    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    LaunchedEffect(lifecycleOwner, analyzer, hasPermission) {
        analyzer.setEnabled(true)
        val cameraProvider = withContext(Dispatchers.IO) {
            ProcessCameraProvider.getInstance(context).get()
        }
        try {
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build()
                .also { it.setAnalyzer(analysisExecutor, analyzer) }
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Camera bind failed", e)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            analyzer.setEnabled(false)
            try {
                ProcessCameraProvider.getInstance(context).get().unbindAll()
            } catch (_: Exception) {
            }
            analysisExecutor.shutdown()
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, CourtColors.ServeDim, RoundedCornerShape(10.dp)),
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize(),
        )
        Text(
            text = stringResource(R.string.serve_camera_hint),
            color = CourtColors.Serve,
            fontSize = 9.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .background(CourtColors.Black.copy(alpha = 0.55f))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
