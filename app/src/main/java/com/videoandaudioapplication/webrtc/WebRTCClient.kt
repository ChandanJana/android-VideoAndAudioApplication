package com.videoandaudioapplication.webrtc

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import com.google.gson.Gson
import com.videoandaudioapplication.model.DataModel
import com.videoandaudioapplication.model.DataModelType
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.ScreenCapturerAndroid
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoCapturer
import org.webrtc.VideoTrack
import javax.inject.Inject

/**
 * Created by Chandan Jana on 14-03-2024.
 * Company name: Mindteck
 * Email: chandan.jana@mindteck.com
 */
class WebRTCClient @Inject constructor(
    private val context: Context,
    private val gson: Gson
) {
    private val TAG = "FirebaseClient"
    //class variables
    var listener: Listener? = null
    private lateinit var username: String

    //Creating a WebRTC connection consists of two steps:
    //
    //1. Establishing a logical connection – devices must agree on the data format, codecs, etc.
    //2. Establishing a physical connection – devices must know each other’s addresses
    //To begin with, note that at the initiation of a connection, to exchange data
    // between devices, a signaling mechanism is used. The signaling mechanism can
    // be any channel for transmitting data, such as sockets.
    //
    //Suppose we want to establish a video connection between two devices.
    // To do this we need to establish a logical connection between them.

    //webrtc variables
    private val eglBaseContext = EglBase.create().eglBaseContext
    private val peerConnectionFactory by lazy { createPeerConnectionFactory() }
    private var peerConnection: PeerConnection? = null
    private val iceServer = listOf(
        PeerConnection.IceServer.builder("turn:a.relay.metered.ca:443?transport=tcp")
            .setUsername("83eebabf8b4cce9d5dbcb649")
            .setPassword("2D7JvfkOQtBdYW3R").createIceServer()
    )
    private val localVideoSource by lazy { peerConnectionFactory.createVideoSource(false) }
    private val localAudioSource by lazy { peerConnectionFactory.createAudioSource(MediaConstraints())}
    private val videoCapturer = getVideoCapturer(context)
    private var surfaceTextureHelper: SurfaceTextureHelper?=null
    private val mediaConstraint = MediaConstraints().apply {
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo","true"))
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio","true"))
    }

    //call variables
    private lateinit var localSurfaceView: SurfaceViewRenderer
    private lateinit var remoteSurfaceView: SurfaceViewRenderer
    private var localStream: MediaStream? = null
    private var localTrackId = ""
    private var localStreamId = ""
    private var localAudioTrack: AudioTrack?=null
    private var localVideoTrack: VideoTrack?=null

    //screen casting
    private var permissionIntent: Intent?=null
    private var screenCapturer: VideoCapturer?=null
    private val localScreenVideoSource by lazy { peerConnectionFactory.createVideoSource(false) }
    private var localScreenShareVideoTrack:VideoTrack?=null

    //installing requirements section
    init {
        initPeerConnectionFactory()
    }
    private fun initPeerConnectionFactory() {
        Log.d(TAG, "TAGG initPeerConnectionFactory: ")
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true).setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)
    }

    // Logical connection
    //
    //A logical connection is established using the Session Description Protocol (SDP),
    // for this one peer:
    //
    //Creates a PeerConnection object.
    //
    //Forms an object on the SDP offer, which contains data about the upcoming
    // session, and sends it to the interlocutor using a signaling mechanism.
    private fun createPeerConnectionFactory(): PeerConnectionFactory {
        Log.d(TAG, "TAGG createPeerConnectionFactory: ")
        return PeerConnectionFactory.builder()
            .setVideoDecoderFactory(
                DefaultVideoDecoderFactory(eglBaseContext)
            ).setVideoEncoderFactory(
                DefaultVideoEncoderFactory(
                    eglBaseContext, true, true
                )
            ).setOptions(PeerConnectionFactory.Options().apply {
                disableNetworkMonitor = false
                disableEncryption = false
            }).createPeerConnectionFactory()
    }
    fun initializeWebrtcClient(
        username: String, observer: PeerConnection.Observer
    ) {
        Log.d(TAG, "TAGG initializeWebrtcClient: username $username")
        this.username = username
        localTrackId = "${username}_track"
        localStreamId = "${username}_stream"
        peerConnection = createPeerConnection(observer)
    }
    private fun createPeerConnection(observer: PeerConnection.Observer): PeerConnection? {
        Log.d(TAG, "TAGG createPeerConnection: ")
        return peerConnectionFactory.createPeerConnection(iceServer, observer)
    }

    //negotiation section
    // Send SDP Offer
    fun call(receiver:String){
        Log.d(TAG, "TAGG call: target $receiver")
        peerConnection?.createOffer(object : MySdpObserver() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                super.onCreateSuccess(desc)
                peerConnection?.setLocalDescription(object : MySdpObserver() {
                    override fun onSetSuccess() {
                        super.onSetSuccess()
                        Log.d(TAG, "TAGG call onSetSuccess: SessionDescription $desc")
                        listener?.onTransferEventToSocket(
                            DataModel(type = DataModelType.Offer,
                                sender = username,
                                receiver = receiver,
                                data = desc?.description)
                        )
                    }
                },desc)
            }
        },mediaConstraint)
    }

    // Forming and sending SDP-answer
    fun answer(receiver:String){
        Log.d(TAG, "TAGG answer: target $receiver")
        peerConnection?.createAnswer(object : MySdpObserver() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                super.onCreateSuccess(desc)
                peerConnection?.setLocalDescription(object : MySdpObserver() {
                    override fun onSetSuccess() {
                        super.onSetSuccess()
                        Log.d(TAG, "TAGG answer onSetSuccess: SessionDescription $desc")
                        listener?.onTransferEventToSocket(
                            DataModel(type = DataModelType.Answer,
                                sender = username,
                                receiver = receiver,
                                data = desc?.description)
                        )
                    }
                },desc)
            }
        },mediaConstraint)
    }

    // Saving the received SDP-offer
    fun onRemoteSessionReceived(sessionDescription: SessionDescription){
        Log.d(TAG, "TAGG onRemoteSessionReceived: sessionDescription $sessionDescription")
        peerConnection?.setRemoteDescription(MySdpObserver(),sessionDescription)
    }

    fun addIceCandidateToPeer(iceCandidate: IceCandidate){
        Log.d(TAG, "TAGG addIceCandidateToPeer: $iceCandidate")
        peerConnection?.addIceCandidate(iceCandidate)
    }

    fun sendIceCandidate(receiver: String,iceCandidate: IceCandidate){
        Log.d(TAG, "TAGG sendIceCandidate: target $receiver, iceCandidate $iceCandidate")
        addIceCandidateToPeer(iceCandidate)
        listener?.onTransferEventToSocket(
            DataModel(
                type = DataModelType.IceCandidates,
                sender = username,
                receiver = receiver,
                data = gson.toJson(iceCandidate)
            )
        )
    }

    fun closeConnection(){
        Log.d(TAG, "TAGG closeConnection: ")
        try {
            videoCapturer.dispose()
            screenCapturer?.dispose()
            localStream?.dispose()
            peerConnection?.close()
        }catch (e:Exception){
            e.printStackTrace()
        }
    }

    fun switchCamera(){
        videoCapturer.switchCamera(null)
    }

    fun toggleAudio(shouldBeMuted:Boolean){
        if (shouldBeMuted){
            localStream?.removeTrack(localAudioTrack)
        }else{
            localStream?.addTrack(localAudioTrack)
        }
    }

    fun toggleVideo(shouldBeMuted: Boolean){
        try {
            if (shouldBeMuted){
                stopCapturingCamera()
            }else{
                startCapturingCamera(localSurfaceView)
            }
        }catch (e:Exception){
            e.printStackTrace()
        }
    }

    //streaming section
    private fun initSurfaceView(view: SurfaceViewRenderer) {
        view.run {
            setMirror(false)
            setEnableHardwareScaler(true)
            init(eglBaseContext, null)
        }
    }
    fun initRemoteSurfaceView(view:SurfaceViewRenderer){
        this.remoteSurfaceView = view
        initSurfaceView(view)
    }
    fun initLocalSurfaceView(localView: SurfaceViewRenderer, isVideoCall: Boolean) {
        Log.d(TAG, "initLocalSurfaceView: ")
        this.localSurfaceView = localView
        initSurfaceView(localView)
        startLocalStreaming(localView, isVideoCall)
    }
    private fun startLocalStreaming(localView: SurfaceViewRenderer, isVideoCall: Boolean) {
        Log.d(TAG, "startLocalStreaming: ")
        localStream = peerConnectionFactory.createLocalMediaStream(localStreamId)
        if (isVideoCall){
            startCapturingCamera(localView)
        }

        localAudioTrack = peerConnectionFactory.createAudioTrack(localTrackId+"_audio",localAudioSource)
        localStream?.addTrack(localAudioTrack)
        peerConnection?.addStream(localStream)
    }
    private fun startCapturingCamera(localView: SurfaceViewRenderer){
        surfaceTextureHelper = SurfaceTextureHelper.create(
            Thread.currentThread().name,eglBaseContext
        )

        videoCapturer.initialize(
            surfaceTextureHelper,context,localVideoSource.capturerObserver
        )

        videoCapturer.startCapture(
            720,480,20
        )

        localVideoTrack = peerConnectionFactory.createVideoTrack(localTrackId+"_video",localVideoSource)
        localVideoTrack?.addSink(localView)
        localStream?.addTrack(localVideoTrack)
    }
    private fun getVideoCapturer(context: Context): CameraVideoCapturer =
        Camera2Enumerator(context).run {
            deviceNames.find {
                isFrontFacing(it)
            }?.let {
                createCapturer(it,null)
            }?:throw IllegalStateException()
        }
    private fun stopCapturingCamera(){

        videoCapturer.dispose()
        localVideoTrack?.removeSink(localSurfaceView)
        localSurfaceView.clearImage()
        localStream?.removeTrack(localVideoTrack)
        localVideoTrack?.dispose()
    }

    //screen capture section

    fun setPermissionIntent(screenPermissionIntent: Intent) {
        this.permissionIntent = screenPermissionIntent
    }

    fun startScreenCapturing() {
        val displayMetrics = DisplayMetrics()
        val windowsManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowsManager.defaultDisplay.getMetrics(displayMetrics)

        val screenWidthPixels = displayMetrics.widthPixels
        val screenHeightPixels = displayMetrics.heightPixels

        val surfaceTextureHelper = SurfaceTextureHelper.create(
            Thread.currentThread().name,eglBaseContext
        )

        screenCapturer = createScreenCapturer()
        screenCapturer!!.initialize(
            surfaceTextureHelper,context,localScreenVideoSource.capturerObserver
        )
        screenCapturer!!.startCapture(screenWidthPixels,screenHeightPixels,15)

        localScreenShareVideoTrack =
            peerConnectionFactory.createVideoTrack(localTrackId+"_video",localScreenVideoSource)
        localScreenShareVideoTrack?.addSink(localSurfaceView)
        localStream?.addTrack(localScreenShareVideoTrack)
        peerConnection?.addStream(localStream)

    }

    fun stopScreenCapturing() {
        screenCapturer?.stopCapture()
        screenCapturer?.dispose()
        localScreenShareVideoTrack?.removeSink(localSurfaceView)
        localSurfaceView.clearImage()
        localStream?.removeTrack(localScreenShareVideoTrack)
        localScreenShareVideoTrack?.dispose()

    }

    private fun createScreenCapturer():VideoCapturer {
        return ScreenCapturerAndroid(permissionIntent, object : MediaProjection.Callback() {
            override fun onStop() {
                super.onStop()
                Log.d("TAGG", "onStop: permission of screen casting is stopped")
            }
        })
    }


    interface Listener {
        fun onTransferEventToSocket(data: DataModel)
    }
}