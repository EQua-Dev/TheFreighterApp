package com.androidstrike.schoolprojects.thefreighterapp.features.home.client.dispatch.pending

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat.getColor
import androidx.recyclerview.widget.RecyclerView
import com.androidstrike.schoolprojects.thefreighterapp.R
import com.androidstrike.schoolprojects.thefreighterapp.models.InterestedDriverDetail

class InterestedDriversAdapter(private val items: List<InterestedDriverDetail>) : RecyclerView.Adapter<InterestedDriversAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Define the views in the item layout
        val driverItemImage: ImageView = itemView.findViewById(R.id.iv_choose_external_weigher_item)
        val interestedDriverTextView: TextView = itemView.findViewById(R.id.external_weigher_name)
        val driverPriceTextView: TextView = itemView.findViewById(R.id.external_weigher_price)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Inflate the item layout
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_external_weighers_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Bind data to the views
        val item = items[position]
        holder.interestedDriverTextView.text = item.driverName
        holder.driverPriceTextView.text = item.driverCharge

        holder.itemView.setOnClickListener {
            it.setBackgroundResource(R.color.primary_faded)
            holder.driverItemImage.setImageResource(R.drawable.ic_selected)
            onDriverClickListener?.invoke(item)
        }
    }

    private var onDriverClickListener: ((InterestedDriverDetail) -> Unit)? = null

    fun createOnDriverClickListener(listener: (InterestedDriverDetail) -> Unit){
        onDriverClickListener = listener
    }

    override fun getItemCount(): Int {
        return items.size
    }
}
