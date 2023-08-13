package com.androidstrike.schoolprojects.thefreighterapp.features.home.client.wallet

import android.view.View
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.androidstrike.schoolprojects.thefreighterapp.R

class WalletHistoryAdapter(itemView: View): RecyclerView.ViewHolder(itemView) {

    var walletTransactionDate: TextView
    var walletTransactionType: TextView
    var walletTransactionAmount: TextView
    var walletTransactionReason: TextView

    init {
        walletTransactionDate = itemView.findViewById(R.id.transaction_date)
        walletTransactionType = itemView.findViewById(R.id.transaction_type)
        walletTransactionAmount = itemView.findViewById(R.id.transaction_amount)
        walletTransactionReason = itemView.findViewById(R.id.transaction_reason)
    }
}