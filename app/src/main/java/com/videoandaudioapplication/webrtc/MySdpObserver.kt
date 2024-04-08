package com.videoandaudioapplication.webrtc

import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

/**
 * Created by Chandan Jana on 14-03-2024.
 * Company name: Mindteck
 * Email: chandan.jana@mindteck.com
 */
open class MySdpObserver : SdpObserver {
    override fun onCreateSuccess(desc: SessionDescription?) {

    }

    override fun onSetSuccess() {
    }

    override fun onCreateFailure(p0: String?) {
    }

    override fun onSetFailure(p0: String?) {
    }
}