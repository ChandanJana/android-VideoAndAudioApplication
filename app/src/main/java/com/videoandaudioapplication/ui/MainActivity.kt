package com.videoandaudioapplication.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.videoandaudioapplication.adapter.MainRecyclerViewAdapter
import com.videoandaudioapplication.callback.AudioVideoClickListener
import com.videoandaudioapplication.callback.ReceivedCallListener
import com.videoandaudioapplication.databinding.ActivityMainBinding
import com.videoandaudioapplication.repository.MainRepository
import com.videoandaudioapplication.service.MainService
import com.videoandaudioapplication.service.MainServiceRepository
import com.videoandaudioapplication.model.DataModel
import com.videoandaudioapplication.model.DataModelType
import com.videoandaudioapplication.util.FirebaseFieldNames
import com.videoandaudioapplication.util.getCameraAndMicPermission
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Created by Chandan Jana on 14-03-2024.
 * Company name: Mindteck
 * Email: chandan.jana@mindteck.com
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity(), AudioVideoClickListener, ReceivedCallListener {
    private val TAG = "MainActivity"

    private lateinit var views: ActivityMainBinding
    private var username: String? = null

    @Inject
    lateinit var mainRepository: MainRepository
    @Inject
    lateinit var mainServiceRepository: MainServiceRepository
    private var mainAdapter: MainRecyclerViewAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        views = ActivityMainBinding.inflate(layoutInflater)
        setContentView(views.root)
        init()
    }

    private fun init() {
        username = intent.getStringExtra("username")
        if (username == null)
            finish()
        //1. observe other users status
        subscribeObservers()
        //2. start foreground service to listen negotiations and calls.
        startMyService()
    }

    private fun subscribeObservers() {
        setupRecyclerView()
        MainService.receivedCallListener = this
        mainRepository.observeUsersStatus {
            Log.d(TAG, "TAGG subscribeObservers: $it")
            mainAdapter?.updateList(it)
        }
    }

    private fun setupRecyclerView() {
        mainAdapter = MainRecyclerViewAdapter(this)
        val layoutManager = LinearLayoutManager(this)
        views.mainRecyclerView.apply {
            setLayoutManager(layoutManager)
            adapter = mainAdapter
        }
    }

    private fun startMyService() {
        Log.d(TAG, "TAGG startMyService")
        mainServiceRepository.startService(username!!)
    }

    override fun onVideoCallClicked(username: String) {
        //check if permission of mic and camera is taken
        getCameraAndMicPermission {
            mainRepository.sendConnectionRequest(username, true) {
                if (it){
                    Log.d(TAG, "TAGG onVideoCallClicked: target: "+ username+", isVideoCall: true, isCaller: true")
                    //we have to start video call
                    //we wanna create an intent to move to call activity
                    startActivity(Intent(this,CallActivity::class.java).apply {
                        putExtra(FirebaseFieldNames.RECEIVER,username)
                        putExtra(FirebaseFieldNames.IS_VIDEO_CALL,true)
                        putExtra(FirebaseFieldNames.IS_AUDIO_CALL,true)
                    })

                }
            }

        }
    }

    override fun onAudioCallClicked(username: String) {
        getCameraAndMicPermission {
            mainRepository.sendConnectionRequest(username, false) {
                if (it){
                    Log.d(TAG, "TAGG onAudioCallClicked: target: "+ username+", isVideoCall: false, isCaller: true")

                    //we have to start audio call
                    //we wanna create an intent to move to call activity
                    startActivity(Intent(this,CallActivity::class.java).apply {
                        putExtra(FirebaseFieldNames.RECEIVER,username)
                        putExtra(FirebaseFieldNames.IS_VIDEO_CALL,false)
                        putExtra(FirebaseFieldNames.IS_AUDIO_CALL,true)
                    })
                }
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        mainServiceRepository.stopService()
    }

    override fun onCallReceived(model: DataModel) {
        Log.d(TAG, "TAGG onCallReceived: DataModel: $model")
        runOnUiThread {
            views.apply {
                val isVideoCall = model.type == DataModelType.StartVideoCall
                val isVideoCallText = if (isVideoCall) "Video" else "Audio"
                incomingCallTitleTv.text = "${model.sender} is $isVideoCallText Calling you"
                incomingCallLayout.isVisible = true
                acceptButton.setOnClickListener {
                    getCameraAndMicPermission {
                        incomingCallLayout.isVisible = false
                        //create an intent to go to video call activity
                        startActivity(Intent(this@MainActivity,CallActivity::class.java).apply {
                            putExtra(FirebaseFieldNames.RECEIVER,model.sender)
                            putExtra(FirebaseFieldNames.IS_VIDEO_CALL,isVideoCall)
                            putExtra(FirebaseFieldNames.IS_AUDIO_CALL,false)
                        })
                    }
                }
                declineButton.setOnClickListener {
                    incomingCallLayout.isVisible = false
                }

            }
        }
    }


}