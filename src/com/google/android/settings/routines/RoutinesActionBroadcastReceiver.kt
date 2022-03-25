package com.google.android.settings.routines

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.util.Log

private val TAG = RoutinesActionBroadcastReceiver::class.qualifiedName!!
private val ACTION = "$TAG.RINGER_MODE_SILENCE_ACTION"

class RoutinesActionBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == ACTION) {
            handleRingerModeSilenceAction(context)
        } else {
            Log.w(TAG, "Unknown action for RoutinesActionBroadcastReceiver: ${intent?.action}")
        }
    }

    private fun handleRingerModeSilenceAction(context: Context?) {
        val audioManager = context?.getSystemService(AudioManager::class.java)
        if (audioManager == null) {
            Log.w(TAG, "AudioManager was null. Not able to handleRingerModeSilenceAction.")
        } else {
            audioManager.ringerModeInternal = 0
        }
    }
}