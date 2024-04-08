package com.videoandaudioapplication.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.videoandaudioapplication.R
import com.videoandaudioapplication.repository.MainRepository
import com.videoandaudioapplication.callback.DataChangeListener
import com.videoandaudioapplication.model.DataModel
import com.videoandaudioapplication.model.DataModelType
import com.videoandaudioapplication.callback.EndCallListener
import com.videoandaudioapplication.callback.ReceivedCallListener
import com.videoandaudioapplication.model.isValid
import com.videoandaudioapplication.util.FirebaseFieldNames
import com.videoandaudioapplication.webrtc.RTCAudioManager
import dagger.hilt.android.AndroidEntryPoint
import org.webrtc.SurfaceViewRenderer
import javax.inject.Inject

/**
 * Created by Chandan Jana on 14-03-2024.
 * Company name: Mindteck
 * Email: chandan.jana@mindteck.com
 */
@AndroidEntryPoint
class MainService : Service(), DataChangeListener {

    private val TAG = "MainService"

    private var isServiceRunning = false
    private var username: String? = null

    @Inject
    lateinit var mainRepository: MainRepository

    private lateinit var notificationManager: NotificationManager
    private lateinit var rtcAudioManager: RTCAudioManager
    private var isPreviousCallStateVideo = true


    companion object {
        var receivedCallListener: ReceivedCallListener? = null
        var endCallListener: EndCallListener?=null
        var localSurfaceView: SurfaceViewRenderer?=null
        var remoteSurfaceView: SurfaceViewRenderer?=null
        var screenPermissionIntent : Intent?=null
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "TAGG onCreate: ")
        rtcAudioManager = RTCAudioManager.create(this)
        rtcAudioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.SPEAKER_PHONE)
        notificationManager = getSystemService(
            NotificationManager::class.java
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "TAGG onStartCommand: intent $intent, flags $flags, startId $startId")
        intent?.let { incomingIntent ->
            when (incomingIntent.action) {
                MainServiceActions.START_SERVICE.name -> handleStartService(incomingIntent)
                MainServiceActions.SETUP_VIEWS.name -> handleSetupViews(incomingIntent)
                MainServiceActions.END_CALL.name -> handleEndCall()
                MainServiceActions.SWITCH_CAMERA.name -> handleSwitchCamera()
                MainServiceActions.TOGGLE_AUDIO.name -> handleToggleAudio(incomingIntent)
                MainServiceActions.TOGGLE_VIDEO.name -> handleToggleVideo(incomingIntent)
                MainServiceActions.TOGGLE_AUDIO_DEVICE.name -> handleToggleAudioDevice(incomingIntent)
                MainServiceActions.TOGGLE_SCREEN_SHARE.name -> handleToggleScreenShare(incomingIntent)
                MainServiceActions.STOP_SERVICE.name -> handleStopService()
                else -> Unit
            }
        }

        return START_STICKY
    }

    private fun handleStopService() {
        Log.d(TAG, "TAGG handleStopService: ")
        mainRepository.endCall()
        mainRepository.logOff {
            isServiceRunning = false
            stopSelf()
        }
    }

    private fun handleToggleScreenShare(incomingIntent: Intent) {
        Log.d(TAG, "TAGG handleToggleScreenShare: incomingIntent $incomingIntent")
        val isStarting = incomingIntent.getBooleanExtra("isStarting",true)
        if (isStarting){
            // we should start screen share
            //but we have to keep it in mind that we first should remove the camera streaming first
            if (isPreviousCallStateVideo){
                mainRepository.toggleVideo(true)
            }
            mainRepository.setScreenCaptureIntent(screenPermissionIntent!!)
            mainRepository.toggleScreenShare(true)

        }else{
            //we should stop screen share and check if camera streaming was on so we should make it on back again
            mainRepository.toggleScreenShare(false)
            if (isPreviousCallStateVideo){
                mainRepository.toggleVideo(false)
            }
        }
    }

    private fun handleToggleAudioDevice(incomingIntent: Intent) {
        Log.d(TAG, "TAGG handleToggleAudioDevice: incomingIntent $incomingIntent")
        val type = when(incomingIntent.getStringExtra("type")){
            RTCAudioManager.AudioDevice.EARPIECE.name -> RTCAudioManager.AudioDevice.EARPIECE
            RTCAudioManager.AudioDevice.SPEAKER_PHONE.name -> RTCAudioManager.AudioDevice.SPEAKER_PHONE
            else -> null
        }

        type?.let {
            rtcAudioManager.setDefaultAudioDevice(it)
            rtcAudioManager.selectAudioDevice(it)
            Log.d(TAG, "TAGG handleToggleAudioDevice: $it")
        }


    }

    private fun handleToggleVideo(incomingIntent: Intent) {
        Log.d(TAG, "TAGG handleToggleVideo: incomingIntent $incomingIntent")
        val shouldBeMuted = incomingIntent.getBooleanExtra("shouldBeMuted",true)
        this.isPreviousCallStateVideo = !shouldBeMuted
        mainRepository.toggleVideo(shouldBeMuted)
    }

    private fun handleToggleAudio(incomingIntent: Intent) {
        Log.d(TAG, "TAGG handleToggleAudio: incomingIntent $incomingIntent")
        val shouldBeMuted = incomingIntent.getBooleanExtra("shouldBeMuted",true)
        mainRepository.toggleAudio(shouldBeMuted)
    }

    private fun handleSwitchCamera() {
        Log.d(TAG, "TAGG handleSwitchCamera: ")
        mainRepository.switchCamera()
    }

    private fun handleEndCall() {
        Log.d(TAG, "TAGG handleEndCall: ")
        //1. we have to send a signal to other peer that call is ended
        mainRepository.sendEndCall()
        //2.end out call process and restart our webrtc client
        endCallAndRestartRepository()
    }

    private fun endCallAndRestartRepository(){
        Log.d(TAG, "TAGG endCallAndRestartRepository: ")
        mainRepository.endCall()
        endCallListener?.onCallEnded()
        mainRepository.initWebrtcClient(username!!)
    }

    private fun handleSetupViews(incomingIntent: Intent) {
        Log.d(TAG, "TAGG handleSetupViews: incomingIntent $incomingIntent")
        val isAudioCall = incomingIntent.getBooleanExtra(FirebaseFieldNames.IS_AUDIO_CALL,false)
        val isVideoCall = incomingIntent.getBooleanExtra(FirebaseFieldNames.IS_VIDEO_CALL,true)
        val receiver = incomingIntent.getStringExtra(FirebaseFieldNames.RECEIVER)
        this.isPreviousCallStateVideo = isVideoCall
        mainRepository.setReceiver(receiver!!)
        //initialize our widgets and start streaming our video and audio source
        //and get prepared for call
        mainRepository.initLocalSurfaceView(localSurfaceView!!,isVideoCall)
        mainRepository.initRemoteSurfaceView(remoteSurfaceView!!)


        if (!isAudioCall){
            //start the video call
            mainRepository.startCall()
        }

    }

    private fun handleStartService(incomingIntent: Intent) {
        Log.d(TAG, "TAGG handleStartService: incomingIntent $incomingIntent")
        //start our foreground service
        if (!isServiceRunning) {
            isServiceRunning = true
            username = incomingIntent.getStringExtra("username")
            startServiceWithNotification()

            //setup my clients
            mainRepository.dataChangeListener = this
            mainRepository.initFirebase()
            mainRepository.initWebrtcClient(username!!)

        }
    }

    private fun startServiceWithNotification() {

        Log.d(TAG, "TAGG startServiceWithNotification: ")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                "channel1", "foreground", NotificationManager.IMPORTANCE_HIGH
            )

            val intent = Intent(this,MainServiceReceiver::class.java).apply {
                action = "ACTION_EXIT"
            }
            val pendingIntent : PendingIntent =
                PendingIntent.getBroadcast(this,0 ,intent,PendingIntent.FLAG_IMMUTABLE)

            notificationManager.createNotificationChannel(notificationChannel)
            val notification = NotificationCompat.Builder(
                this, "channel1"
            )
                .setSmallIcon(R.mipmap.ic_launcher)
                .addAction(R.drawable.ic_end_call,"Exit",pendingIntent)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE).build()

            // promote service to foreground service
            /*startForeground(
                this,
                1,
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                } else {
                    0
                }
            )*/

            startForeground(1, notification)
            //Service.startForeground(0, notification, FOREGROUND_SERVICE_TYPE_LOCATION)
        }
    }


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDataChange(data: DataModel) {
        Log.d(TAG, "TAGG onLatestEventReceived: data $data")
        if (data.isValid()) {
            when (data.type) {
                DataModelType.StartVideoCall,
                DataModelType.StartAudioCall -> {
                    receivedCallListener?.onCallReceived(data)
                }
                else -> Unit
            }
        }
    }

    override fun endCall() {
        Log.d(TAG, "TAGG endCall: ")
        //we are receiving end call signal from remote peer
        endCallAndRestartRepository()
    }

}