package de.axelcypher.healthconnectbridge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WeightBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != WeightIntentParser.ACTION_WRITE_WEIGHT) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (val parsed = WeightIntentParser.parse(intent)) {
                    is ParseResult.Success -> WeightSyncRepository(appContext).process(parsed.value)
                    is ParseResult.Error -> AppPrefs(appContext).recordError(parsed.message)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

