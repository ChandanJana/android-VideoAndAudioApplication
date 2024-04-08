package com.videoandaudioapplication.callback

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

/**
 * Created by Chandan Jana on 14-03-2024.
 * Company name: Mindteck
 * Email: chandan.jana@mindteck.com
 */

open class MyEventListener : ValueEventListener {
    override fun onDataChange(snapshot: DataSnapshot) {}

    override fun onCancelled(error: DatabaseError) {}
}