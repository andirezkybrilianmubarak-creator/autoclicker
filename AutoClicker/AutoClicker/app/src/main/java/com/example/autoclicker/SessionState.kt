package com.example.autoclicker

/**
 * Single in-memory source of truth for whether an automation session is active.
 * This flag is the ONLY thing that allows MyAccessibilityService to execute gestures.
 * It is set to true exclusively by explicit user action in MainActivity, and is
 * reset to false on app process death, so a killed app can never leave gestures enabled.
 */
object SessionState {

    @Volatile
    var isSessionActive: Boolean = false
        private set

    private val listeners = mutableListOf<(Boolean) -> Unit>()

    @Synchronized
    fun startSession() {
        isSessionActive = true
        notifyListeners()
    }

    @Synchronized
    fun stopSession() {
        isSessionActive = false
        notifyListeners()
    }

    @Synchronized
    fun addListener(listener: (Boolean) -> Unit) {
        listeners.add(listener)
    }

    @Synchronized
    fun removeListener(listener: (Boolean) -> Unit) {
        listeners.remove(listener)
    }

    private fun notifyListeners() {
        listeners.forEach { it(isSessionActive) }
    }
}
