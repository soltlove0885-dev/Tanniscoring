package com.tanniscoring.app.serve

import com.tanniscoring.shared.ServePhase
import com.tanniscoring.shared.ServeSessionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * After a serve peak, if no return / point within [noReturnMs], treat as fault → 2nd serve.
 */
class ServeTimeoutWatcher(
    private val scope: CoroutineScope,
    private val engine: ServeSessionEngine,
    private val noReturnMs: Long = 3_800L,
    private val onChanged: () -> Unit,
) {
    private var job: Job? = null

    fun armForPhase(phase: ServePhase) {
        job?.cancel()
        if (phase != ServePhase.AFTER_FIRST && phase != ServePhase.AFTER_SECOND) return
        job = scope.launch {
            delay(noReturnMs)
            val before = engine.snapshot().phase
            if (before == ServePhase.AFTER_FIRST || before == ServePhase.AFTER_SECOND) {
                engine.onNoReturnTimeout()
                onChanged()
            }
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
    }
}
