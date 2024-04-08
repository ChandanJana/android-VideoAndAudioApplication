package com.videoandaudioapplication.repository

import android.content.Intent
import android.util.Log
import com.google.gson.Gson
import com.videoandaudioapplication.firebaseClient.FirebaseClient
import com.videoandaudioapplication.callback.DataChangeListener
import com.videoandaudioapplication.callback.FirebaseDataListener
import com.videoandaudioapplication.model.DataModel
import com.videoandaudioapplication.model.DataModelType
import com.videoandaudioapplication.util.UserStatus
import com.videoandaudioapplication.webrtc.MyPeerObserver
import com.videoandaudioapplication.webrtc.WebRTCClient
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by Chandan Jana on 14-03-2024.
 * Company name: Mindteck
 * Email: chandan.jana@mindteck.com
 */

@Singleton
class MainRepository @Inject constructor(
    private val firebaseClient: FirebaseClient,
    private val webRTCClient: WebRTCClient,
    private val gson: Gson
) : WebRTCClient.Listener {

    private val TAG = "MainRepository"
    private var receiver: String? = null
    var dataChangeListener: DataChangeListener? = null
    private var remoteView: SurfaceViewRenderer?=null

    fun login(username: String, password: String, isDone: (Boolean, String?) -> Unit) {
        Log.d(TAG, "TAGG login: username $username")
        firebaseClient.login(username, password, isDone)
    }

    fun observeUsersStatus(status: (List<Pair<String, String>>) -> Unit) {
        Log.d(TAG, "TAGG observeUsersStatus: ")
        firebaseClient.observeUsersStatus(status)
    }

    fun initFirebase() {
        firebaseClient.subscribeForLatestEvent(object : FirebaseDataListener {
            override fun onLatestEventReceived(event: DataModel) {
                Log.d(TAG, "TAGG onLatestEventReceived: event $event")
                dataChangeListener?.onDataChange(event)
                when (event.type) {
                    DataModelType.Offer ->{
                        webRTCClient.onRemoteSessionReceived(
                            SessionDescription(
                                SessionDescription.Type.OFFER,
                                event.data.toString()
                            )
                        )
                        webRTCClient.answer(receiver!!)
                    }
                    DataModelType.Answer ->{
                        webRTCClient.onRemoteSessionReceived(
                            SessionDescription(
                                SessionDescription.Type.ANSWER,
                                event.data.toString()
                            )
                        )
                    }
                    DataModelType.IceCandidates ->{
                        val candidate: IceCandidate? = try {
                            gson.fromJson(event.data.toString(),IceCandidate::class.java)
                        }catch (e:Exception){
                            null
                        }
                        candidate?.let {
                            webRTCClient.addIceCandidateToPeer(it)
                        }
                    }
                    DataModelType.EndCall ->{
                        dataChangeListener?.endCall()
                    }
                    else -> Unit
                }
            }

        })
    }

    fun sendConnectionRequest(receiver: String, isVideoCall: Boolean, success: (Boolean) -> Unit) {
        Log.d(TAG, "TAGG sendConnectionRequest: target $receiver")
        firebaseClient.sendMessageToOtherClient(
            DataModel(
                type = if (isVideoCall) DataModelType.StartVideoCall else DataModelType.StartAudioCall,
                receiver = receiver
            ), success
        )
    }

    fun setReceiver(receiver: String) {
        this.receiver = receiver
    }

    fun initWebrtcClient(username: String) {
        Log.d(TAG, "TAGG initWebrtcClient: username $username")
        webRTCClient.listener = this
        webRTCClient.initializeWebrtcClient(username, object : MyPeerObserver() {

            override fun onAddStream(p0: MediaStream?) {
                super.onAddStream(p0)
                try {
                    Log.d(TAG, "TAGG onAddStream: $p0")
                    p0?.videoTracks?.get(0)?.addSink(remoteView)
                }catch (e:Exception){
                    e.printStackTrace()
                }

            }

            override fun onIceCandidate(p0: IceCandidate?) {
                super.onIceCandidate(p0)
                Log.d(TAG, "TAGG onIceCandidate: $p0")
                p0?.let {
                    webRTCClient.sendIceCandidate(receiver!!, it)
                }
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                super.onConnectionChange(newState)
                Log.d(TAG, "TAGG onConnectionChange: $newState")
                if (newState == PeerConnection.PeerConnectionState.CONNECTED) {
                    Log.d(TAG, "TAGG onConnectionChange: PeerConnectionState.CONNECTED")
                    // 1. change my status to in call
                    changeMyStatus(UserStatus.IN_CALL)
                    // 2. clear latest event inside my user section in firebase database
                    firebaseClient.clearLatestEvent()
                }
            }
        })
    }

    fun initLocalSurfaceView(view: SurfaceViewRenderer, isVideoCall: Boolean) {
        Log.d(TAG, "initLocalSurfaceView: ")
        webRTCClient.initLocalSurfaceView(view, isVideoCall)
    }

    fun initRemoteSurfaceView(view: SurfaceViewRenderer) {
        webRTCClient.initRemoteSurfaceView(view)
        this.remoteView = view
    }

    fun startCall() {
        webRTCClient.call(receiver!!)
    }

    fun endCall() {
        webRTCClient.closeConnection()
        changeMyStatus(UserStatus.ONLINE)
    }

    fun sendEndCall() {
        onTransferEventToSocket(
            DataModel(
                type = DataModelType.EndCall,
                receiver = receiver!!
            )
        )
    }

    private fun changeMyStatus(status: UserStatus) {
        firebaseClient.changeMyStatus(status)
    }

    fun toggleAudio(shouldBeMuted: Boolean) {
        webRTCClient.toggleAudio(shouldBeMuted)
    }

    fun toggleVideo(shouldBeMuted: Boolean) {
        webRTCClient.toggleVideo(shouldBeMuted)
    }

    fun switchCamera() {
        webRTCClient.switchCamera()
    }

    override fun onTransferEventToSocket(data: DataModel) {
        Log.d(TAG, "TAGG onTransferEventToSocket: $data")
        firebaseClient.sendMessageToOtherClient(data) {}
    }

    fun setScreenCaptureIntent(screenPermissionIntent: Intent) {
        webRTCClient.setPermissionIntent(screenPermissionIntent)
    }

    fun toggleScreenShare(isStarting: Boolean) {
        if (isStarting){
            webRTCClient.startScreenCapturing()
        }else{
            webRTCClient.stopScreenCapturing()
        }
    }

    fun logOff(function: () -> Unit) = firebaseClient.logOff(function)

}