package com.example.autoclicker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent

/**
 * Accessibility service that performs on-screen gestures.
 *
 * SAFETY GATE: Every gesture entry point checks SessionState.isSessionActive before
 * dispatching anything. That flag can only be flipped to true by explicit user action
 * in MainActivity (tapping "START AUTOMATION SESSION"), and is reset automatically if
 * the app process dies or the foreground service is destroyed. If the flag is false,
 * these methods are no-ops and log why they refused to run.
 */
class MyAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "MyAccessibilityService"

        @Volatile
        private var instance: MyAccessibilityService? = null

        fun getInstance(): MyAccessibilityService? = instance
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "Accessibility service connected.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // This app does not act on incoming accessibility events automatically.
        // Events are only used, if at all, for on-screen state awareness while
        // a user-initiated session is active. No autonomous action is taken here.
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted by the system.")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        SessionState.stopSession()
    }

    /**
     * Performs a single tap at the given screen coordinates.
     * Returns true if the gesture was dispatched, false if it was refused.
     */
    @Synchronized
    fun performClick(x: Float, y: Float, durationMs: Long = 100): Boolean {
        if (!SessionState.isSessionActive) {
            Log.w(TAG, "performClick refused: no active session.")
            return false
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.e(TAG, "performClick refused: gestures require API 24+.")
            return false
        }

        val clickPath = Path().apply {
            moveTo(x, y)
        }

        val gestureBuilder = GestureDescription.Builder()
        val strokeDescription = GestureDescription.StrokeDescription(
            clickPath,
            0L,
            durationMs.coerceAtLeast(1L)
        )
        gestureBuilder.addStroke(strokeDescription)

        return dispatchGesture(gestureBuilder.build(), object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                Log.i(TAG, "Click gesture completed at ($x, $y).")
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                Log.w(TAG, "Click gesture cancelled at ($x, $y).")
            }
        }, null)
    }

    /**
     * Performs a swipe gesture from the start coordinates to the end coordinates.
     * Returns true if the gesture was dispatched, false if it was refused.
     */
    @Synchronized
    fun performSwipe(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        durationMs: Long = 300
    ): Boolean {
        if (!SessionState.isSessionActive) {
            Log.w(TAG, "performSwipe refused: no active session.")
            return false
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.e(TAG, "performSwipe refused: gestures require API 24+.")
            return false
        }

        val swipePath = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }

        val gestureBuilder = GestureDescription.Builder()
        val strokeDescription = GestureDescription.StrokeDescription(
            swipePath,
            0L,
            durationMs.coerceAtLeast(1L)
        )
        gestureBuilder.addStroke(strokeDescription)

        return dispatchGesture(gestureBuilder.build(), object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                Log.i(TAG, "Swipe gesture completed from ($startX, $startY) to ($endX, $endY).")
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                Log.w(TAG, "Swipe gesture cancelled from ($startX, $startY) to ($endX, $endY).")
            }
        }, null)
    }
}
