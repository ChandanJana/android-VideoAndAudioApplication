package com.videoandaudioapplication.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.videoandaudioapplication.ui.CloseActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Created by Chandan Jana on 14-03-2024.
 * Company name: Mindteck
 * Email: chandan.jana@mindteck.com
 */
@AndroidEntryPoint
class MainServiceReceiver : BroadcastReceiver() {

    @Inject
    lateinit var serviceRepository: MainServiceRepository
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "ACTION_EXIT"){
            //we want to exit the whole application
            serviceRepository.stopService()
            context?.startActivity(Intent(context, CloseActivity::class.java))

        }

    }
}