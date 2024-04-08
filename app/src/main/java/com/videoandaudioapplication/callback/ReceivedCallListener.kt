package com.videoandaudioapplication.callback

import com.videoandaudioapplication.model.DataModel

/**
 * Created by Chandan Jana on 22-03-2024.
 * Company name: Mindteck
 * Email: chandan.jana@mindteck.com
 */
interface ReceivedCallListener {
    fun onCallReceived(model: DataModel)
}