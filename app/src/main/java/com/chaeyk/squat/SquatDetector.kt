package com.chaeyk.squat

import android.util.Log
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.*

class SquatDetector {
    private var isSquatting = false
    private var squatCount = 0
    private var lastKneeAngle = 0.0
    private var squatStartTime = 0L
    private val minSquatDuration = 500L // 0.5초 이상 유지해야 스쿼트로 인정
    
    private val onSquatCountChanged: (Int) -> Unit
    private val onKneeAnglesChanged: (String) -> Unit
    private val onSquatStatusChanged: (String) -> Unit
    
    constructor(
        onSquatCountChanged: (Int) -> Unit, 
        onKneeAnglesChanged: (String) -> Unit = {},
        onSquatStatusChanged: (String) -> Unit = {}
    ) {
        this.onSquatCountChanged = onSquatCountChanged
        this.onKneeAnglesChanged = onKneeAnglesChanged
        this.onSquatStatusChanged = onSquatStatusChanged
    }
    
    fun analyzePose(pose: Pose) {
        
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
        
        // 한쪽 다리라도 감지되면 처리
        if ((leftHip != null && leftKnee != null && leftAnkle != null) ||
            (rightHip != null && rightKnee != null && rightAnkle != null)) {
            
            var leftKneeAngle: Double? = null
            var rightKneeAngle: Double? = null
            var averageKneeAngle = 180.0
            
            if (leftHip != null && leftKnee != null && leftAnkle != null) {
                leftKneeAngle = calculateAngle(leftHip, leftKnee, leftAnkle)
            }
            
            if (rightHip != null && rightKnee != null && rightAnkle != null) {
                rightKneeAngle = calculateAngle(rightHip, rightKnee, rightAnkle)
            }
            
            // 각도 계산 및 화면 표시
            if (leftKneeAngle != null && rightKneeAngle != null) {
                averageKneeAngle = (leftKneeAngle + rightKneeAngle) / 2.0
                onKneeAnglesChanged("왼쪽: ${leftKneeAngle.toInt()}°\n오른쪽: ${rightKneeAngle.toInt()}°\n평균: ${averageKneeAngle.toInt()}°")
            } else if (leftKneeAngle != null) {
                averageKneeAngle = leftKneeAngle
                onKneeAnglesChanged("왼쪽: ${leftKneeAngle.toInt()}°")
            } else if (rightKneeAngle != null) {
                averageKneeAngle = rightKneeAngle
                onKneeAnglesChanged("오른쪽: ${rightKneeAngle.toInt()}°")
            }
            
            val wasSquatting = isSquatting
            val currentTime = System.currentTimeMillis()
            
            // 스쿼트 감지 임계값 (더 간단하게)
            val squatThreshold = 100.0  // 100도 이하면 스쿼트
            val standThreshold = 140.0  // 140도 이상으로 일어나면 카운트
            
            val shouldBeSquatting = averageKneeAngle < squatThreshold
            
            // 스쿼트 시작 감지
            if (!wasSquatting && shouldBeSquatting) {
                isSquatting = true
                squatStartTime = currentTime
                onSquatStatusChanged("⬇️ 스쿼트 시작!")
            }
            // 스쿼트 완료 감지
            else if (wasSquatting && !shouldBeSquatting && averageKneeAngle >= standThreshold) {
                // 최소 지속 시간 체크
                if (currentTime - squatStartTime >= minSquatDuration) {
                    squatCount++
                    onSquatCountChanged(squatCount)
                    onSquatStatusChanged("🎉 완료!")
                    isSquatting = false
                } else {
                    onSquatStatusChanged("⚡ 너무 빨라요")
                }
            }
            // 현재 상태 표시
            else if (isSquatting) {
                onSquatStatusChanged("🔥 스쿼트 중")
            } else {
                onSquatStatusChanged("📱 준비")
            }
            
            lastKneeAngle = averageKneeAngle
        } else {
            onKneeAnglesChanged("감지 안됨")
            onSquatStatusChanged("❌ 사람 감지 안됨")
        }
    }

    private fun calculateAngle(
        firstPoint: PoseLandmark,  // 엉덩이
        midPoint: PoseLandmark,    // 무릎
        lastPoint: PoseLandmark    // 발목
    ): Double {
        // 벡터 계산: 무릎에서 엉덩이로, 무릎에서 발목으로
        val vector1X = (firstPoint.position.x - midPoint.position.x).toDouble()
        val vector1Y = (firstPoint.position.y - midPoint.position.y).toDouble()
        
        val vector2X = (lastPoint.position.x - midPoint.position.x).toDouble()
        val vector2Y = (lastPoint.position.y - midPoint.position.y).toDouble()
        
        // 내적과 벡터 크기 계산
        val dotProduct = vector1X * vector2X + vector1Y * vector2Y
        val magnitude1 = sqrt(vector1X * vector1X + vector1Y * vector1Y)
        val magnitude2 = sqrt(vector2X * vector2X + vector2Y * vector2Y)
        
        if (magnitude1 == 0.0 || magnitude2 == 0.0) return 180.0
        
        // 코사인 값 계산 (범위 제한)
        val cosValue = (dotProduct / (magnitude1 * magnitude2)).coerceIn(-1.0, 1.0)
        
        // 각도 계산 (내각)
        val angleInRadians = acos(cosValue)
        return Math.toDegrees(angleInRadians)
    }
    
    fun getSquatCount(): Int = squatCount
    
    fun resetCount() {
        squatCount = 0
        onSquatCountChanged(squatCount)
    }
}