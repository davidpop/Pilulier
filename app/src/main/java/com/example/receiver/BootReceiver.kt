package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.notification.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d("BootReceiver", "Received broadcast: $action")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AlarmScheduler.rescheduleAllActiveAlarms(context)
                Log.d("BootReceiver", "Successfully rescheduled alarms after $action")
            } catch (e: Exception) {
                Log.e("BootReceiver", "Failed to reschedule alarms: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
