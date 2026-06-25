package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.scheduler.DailyHydraScheduler

class BootReceiver : BroadcastReceiver() {
    private val TAG = "DailyHydra_BootReceiver"

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        Log.d(TAG, "Boot event captured: Action = $action")
        
        if (action == Intent.ACTION_BOOT_COMPLETED || 
            action == Intent.ACTION_MY_PACKAGE_REPLACED || 
            action == "android.intent.action.QUICKBOOT_POWERON") {
            
            Log.d(TAG, "Reboot/upgrade detected. Automatically restoring exact alarms.")
            DailyHydraScheduler.scheduleNextReminder(context)
        }
    }
}
