package com.videoandaudioapplication.model

/**
 * Created by Chandan Jana on 14-03-2024.
 * Company name: Mindteck
 * Email: chandan.jana@mindteck.com
 */
enum class DataModelType {
    StartAudioCall,StartVideoCall,Offer,Answer,IceCandidates,EndCall
}
data class DataModel(
    val sender:String?=null,
    val receiver:String,
    val type: DataModelType,
    val data:String?=null,
    val timeStamp:Long = System.currentTimeMillis()
)


fun DataModel.isValid(): Boolean {
    return System.currentTimeMillis() - this.timeStamp < 60000
}