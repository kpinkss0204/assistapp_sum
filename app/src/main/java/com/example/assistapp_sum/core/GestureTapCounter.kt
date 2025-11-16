package com.example.assistapp_sum.core

class GestureTapCounter(
    private val windowMs: Long = 800L
) {
    private var lastTap = 0L
    private var count = 0

    fun onTap(): Int {
        val now = System.currentTimeMillis()
        count = if (now - lastTap <= windowMs) count + 1 else 1
        lastTap = now
        return count
    }

    fun reset() { count = 0; lastTap = 0L }
}
