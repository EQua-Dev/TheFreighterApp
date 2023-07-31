package com.androidstrike.schoolprojects.thefreighterapp.features.home.client.dispatch.concluded

import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.androidstrike.schoolprojects.thefreighterapp.R

class ConcludedDispatchAdapter(itemView: View): RecyclerView.ViewHolder(itemView) {

    var concludedDispatchDateDelivered: TextView
    var concludedDeliveredDispatchStatus: TextView
    var concludedDeliveredDispatchDestination: TextView
    var concludedDeliveredDispatchDriverName: TextView
    var concludedDeliveredDispatchRating: TextView





    init {
        concludedDispatchDateDelivered = itemView.findViewById(R.id.tv_date_delivered)
        concludedDeliveredDispatchStatus = itemView.findViewById(R.id.tv_delivered_dispatch_status)
        concludedDeliveredDispatchDestination = itemView.findViewById(R.id.tv_delivered_dispatch_destination)
        concludedDeliveredDispatchDriverName = itemView.findViewById(R.id.tv_delivered_dispatch_driver_name)
        concludedDeliveredDispatchRating = itemView.findViewById(R.id.tv_delivered_dispatch_rating)
    }
}