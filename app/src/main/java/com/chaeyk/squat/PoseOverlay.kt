package com.chaeyk.squat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark

@Composable
fun PoseOverlay(
    pose: Pose?,
    previewWidth: Int,
    previewHeight: Int,
    modifier: Modifier = Modifier
) {
    if (pose == null) return
    
    Canvas(modifier = modifier.fillMaxSize()) {
        drawPose(pose, previewWidth, previewHeight)
    }
}

private fun DrawScope.drawPose(pose: Pose, previewWidth: Int, previewHeight: Int) {
    // 카메라 이미지와 화면의 비율을 맞춰서 스케일링
    val imageAspectRatio = previewWidth.toFloat() / previewHeight.toFloat()
    val screenAspectRatio = size.width / size.height
    
    val scaleX: Float
    val scaleY: Float
    val offsetX: Float
    val offsetY: Float
    
    if (imageAspectRatio > screenAspectRatio) {
        // 이미지가 더 가로로 길 때
        scaleY = size.height / previewHeight
        scaleX = scaleY
        offsetX = (size.width - previewWidth * scaleX) / 2f
        offsetY = 0f
    } else {
        // 이미지가 더 세로로 길 때  
        scaleX = size.width / previewWidth
        scaleY = scaleX
        offsetX = 0f
        offsetY = (size.height - previewHeight * scaleY) / 2f
    }
    
    // 모든 랜드마크를 점으로 그리기
    pose.allPoseLandmarks.forEach { landmark ->
        val x = landmark.position.x * scaleX + offsetX
        val y = landmark.position.y * scaleY + offsetY
        
        drawCircle(
            color = Color.Green,
            radius = 8f,
            center = androidx.compose.ui.geometry.Offset(x, y)
        )
    }
    
    // 스켈레톤 연결선 그리기
    drawPoseConnections(pose, scaleX, scaleY, offsetX, offsetY)
}

private fun DrawScope.drawPoseConnections(pose: Pose, scaleX: Float, scaleY: Float, offsetX: Float, offsetY: Float) {
    // 왼쪽 다리 (스쿼트에서 중요!) - 굵게 표시
    drawConnection(pose, PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE, scaleX, scaleY, offsetX, offsetY, Color.Magenta, 10f)
    drawConnection(pose, PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE, scaleX, scaleY, offsetX, offsetY, Color.Magenta, 10f)
    
    // 오른쪽 다리 (스쿼트에서 중요!) - 굵게 표시  
    drawConnection(pose, PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE, scaleX, scaleY, offsetX, offsetY, Color.Magenta, 10f)
    drawConnection(pose, PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE, scaleX, scaleY, offsetX, offsetY, Color.Magenta, 10f)
    
    // 몸통
    drawConnection(pose, PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER, scaleX, scaleY, offsetX, offsetY, Color.Blue)
    drawConnection(pose, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP, scaleX, scaleY, offsetX, offsetY, Color.Blue)
    drawConnection(pose, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP, scaleX, scaleY, offsetX, offsetY, Color.Blue)
    drawConnection(pose, PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP, scaleX, scaleY, offsetX, offsetY, Color.Blue)
}

private fun DrawScope.drawConnection(
    pose: Pose,
    startLandmark: Int,
    endLandmark: Int,
    scaleX: Float,
    scaleY: Float,
    offsetX: Float,
    offsetY: Float,
    color: Color,
    strokeWidth: Float = 6f
) {
    val start = pose.getPoseLandmark(startLandmark)
    val end = pose.getPoseLandmark(endLandmark)
    
    if (start != null && end != null) {
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(
                start.position.x * scaleX + offsetX,
                start.position.y * scaleY + offsetY
            ),
            end = androidx.compose.ui.geometry.Offset(
                end.position.x * scaleX + offsetX,
                end.position.y * scaleY + offsetY
            ),
            strokeWidth = strokeWidth
        )
    }
}