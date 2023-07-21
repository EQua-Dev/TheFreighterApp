package com.androidstrike.schoolprojects.thefreighterapp.features.home.client.dispatch

import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.androidstrike.schoolprojects.thefreighterapp.R

class ChooseExternalWeighersAdapter(itemView: View): RecyclerView.ViewHolder(itemView) {

    var externalWeigherName: TextView
    var externalWeigherPrice: TextView
    var externalWeigherItemImage: ImageView




    init {
        externalWeigherName = itemView.findViewById(R.id.external_weigher_name)
        externalWeigherPrice = itemView.findViewById(R.id.external_weigher_price)
        externalWeigherItemImage = itemView.findViewById(R.id.iv_choose_external_weigher_item)
    }
}