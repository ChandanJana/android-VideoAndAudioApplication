package com.videoandaudioapplication.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.videoandaudioapplication.util.FirebaseFieldNames
import javax.inject.Inject

/**
 * Created by Chandan Jana on 14-03-2024.
 * Company name: Mindteck
 * Email: chandan.jana@mindteck.com
 */
class MainServiceRepository @Inject constructor(
    private val context: Context
) {
    private val TAG = "MainServiceRepository"
    fun startService(username:String){
        Log.d(TAG, "TAGG startService: username $username")
        Thread{
            val intent = Intent(context, MainService::class.java)
            intent.putExtra("username",username)
            intent.action = MainServiceActions.START_SERVICE.name
            startServiceIntent(intent)
        }.start()
    }

    private fun startServiceIntent(intent: Intent){
        Log.d(TAG, "TAGG startServiceIntent: intent $intent")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            context.startForegroundService(intent)
        }else{
            context.startService(intent)
        }
    }

    fun setupViews(videoCall: Boolean, audioCall: Boolean, receiver: String) {
        Log.d(TAG, "TAGG setupViews: videoCall $videoCall, caller $audioCall, target $receiver")
        val intent = Intent(context,MainService::class.java)
        intent.apply {
            action = MainServiceActions.SETUP_VIEWS.name
            putExtra(FirebaseFieldNames.IS_VIDEO_CALL,videoCall)
            putExtra(FirebaseFieldNames.RECEIVER,receiver)
            putExtra(FirebaseFieldNames.IS_AUDIO_CALL,audioCall)
        }
        startServiceIntent(intent)
    }

    fun sendEndCall() {
        Log.d(TAG, "TAGG sendEndCall: ")
        val intent = Intent(context,MainService::class.java)
        intent.action = MainServiceActions.END_CALL.name
        startServiceIntent(intent)
    }

    fun switchCamera() {
        Log.d(TAG, "TAGG switchCamera: ")
        val intent = Intent(context,MainService::class.java)
        intent.action = MainServiceActions.SWITCH_CAMERA.name
        startServiceIntent(intent)
    }

    fun toggleAudio(shouldBeMuted: Boolean) {
        Log.d(TAG, "TAGG toggleAudio: shouldBeMuted $shouldBeMuted")
        val intent = Intent(context, MainService::class.java)
        intent.action = MainServiceActions.TOGGLE_AUDIO.name
        intent.putExtra("shouldBeMuted",shouldBeMuted)
        startServiceIntent(intent)
    }

    fun toggleVideo(shouldBeMuted: Boolean) {
        Log.d(TAG, "TAGG toggleVideo: shouldBeMuted $shouldBeMuted")
        val intent = Intent(context, MainService::class.java)
        intent.action = MainServiceActions.TOGGLE_VIDEO.name
        intent.putExtra("shouldBeMuted",shouldBeMuted)
        startServiceIntent(intent)
    }

    fun toggleAudioDevice(type: String) {
        Log.d(TAG, "TAGG toggleAudioDevice: type $type")
        val intent = Intent(context, MainService::class.java)
        intent.action = MainServiceActions.TOGGLE_AUDIO_DEVICE.name
        intent.putExtra("type",type)
        startServiceIntent(intent)
    }

    fun toggleScreenShare(isStarting: Boolean) {
        Log.d(TAG, "TAGG toggleScreenShare: isStarting $isStarting")
        val intent = Intent(context,MainService::class.java)
        intent.action = MainServiceActions.TOGGLE_SCREEN_SHARE.name
        intent.putExtra("isStarting",isStarting)
        startServiceIntent(intent)
    }

    fun stopService() {
        Log.d(TAG, "TAGG stopService: ")
        val intent = Intent(context,MainService::class.java)
        intent.action = MainServiceActions.STOP_SERVICE.name
        startServiceIntent(intent)
    }

}