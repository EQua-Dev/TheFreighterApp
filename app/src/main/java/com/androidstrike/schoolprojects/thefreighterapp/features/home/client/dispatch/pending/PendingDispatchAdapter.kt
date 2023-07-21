package com.androidstrike.schoolprojects.thefreighterapp.features.home.client.dispatch.pending

import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.androidstrike.schoolprojects.thefreighterapp.R

class PendingDispatchAdapter(itemView: View): RecyclerView.ViewHolder(itemView) {

    var pendingDispatchDateCreated: TextView
    var pendingDispatchStatus: TextView
    var pendingDispatchDestination: TextView
    var pendingDispatchDriverName: TextView





    init {
        pendingDispatchDateCreated = itemView.findViewById(R.id.tv_date_created)
        pendingDispatchStatus = itemView.findViewById(R.id.tv_dispatch_status)
        pendingDispatchDestination = itemView.findViewById(R.id.tv_dispatch_destination)
        pendingDispatchDriverName = itemView.findViewById(R.id.tv_dispatch_driver_name)


    }
}