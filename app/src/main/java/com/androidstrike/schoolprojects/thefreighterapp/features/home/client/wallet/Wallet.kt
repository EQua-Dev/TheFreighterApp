package com.androidstrike.schoolprojects.thefreighterapp.features.home.client.wallet

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.androidstrike.schoolprojects.thefreighterapp.R
import com.androidstrike.schoolprojects.thefreighterapp.databinding.FragmentWalletBinding
import com.androidstrike.schoolprojects.thefreighterapp.features.home.client.dispatch.concluded.ConcludedDispatchAdapter
import com.androidstrike.schoolprojects.thefreighterapp.models.Dispatch
import com.androidstrike.schoolprojects.thefreighterapp.models.UserData
import com.androidstrike.schoolprojects.thefreighterapp.models.WalletData
import com.androidstrike.schoolprojects.thefreighterapp.models.WalletHistory
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.DATE_FORMAT_LONG
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.DATE_FORMAT_SHORT
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.REASON_ACCOUNT_FUND
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.WALLET_HISTORY_REF
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.auth
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.userCollectionRef
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.walletCollectionRef
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.walletHistoryCollectionRef
import com.androidstrike.schoolprojects.thefreighterapp.utils.enable
import com.androidstrike.schoolprojects.thefreighterapp.utils.getDate
import com.androidstrike.schoolprojects.thefreighterapp.utils.hashString
import com.androidstrike.schoolprojects.thefreighterapp.utils.hideProgress
import com.androidstrike.schoolprojects.thefreighterapp.utils.showProgress
import com.androidstrike.schoolprojects.thefreighterapp.utils.toast
import com.androidstrike.schoolprojects.thefreighterapp.utils.visible
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.QuerySnapshot
import com.hbb20.CountryCodePicker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.DecimalFormat


class Wallet : Fragment() {

    private var _binding: FragmentWalletBinding? = null
    private val binding get() = _binding!!

    private val TAG = "Wallet"


    private var walletHistoryAdapter: FirestoreRecyclerAdapter<WalletHistory, WalletHistoryAdapter>? =
        null

    private lateinit var clientRole: String
    private lateinit var driverRole: String
    private lateinit var weigherRole: String
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentWalletBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        clientRole = resources.getString(R.string.client)
        driverRole = resources.getString(R.string.driver)
        weigherRole = resources.getString(R.string.weigher)

        with(binding) {
            walletLayout.visible(false)
            noWalletLayout.visible(false)
            val layoutManager = LinearLayoutManager(requireContext())
            rvWalletHistory.layoutManager = layoutManager
            rvWalletHistory.addItemDecoration(
                DividerItemDecoration(
                    requireContext(),
                    layoutManager.orientation
                )
            )
        }

        getWalletDetails()

    }

    private fun getWalletDetails() {
        val user = getUser(auth.uid!!)
        requireContext().showProgress()
        CoroutineScope(Dispatchers.IO).launch {

            walletCollectionRef.document(user!!.wallet)
                .get()
                .addOnSuccessListener {
                    if (it.exists()){
                        hideProgress()
                        binding.walletLayout.visible(true)
                        fetchWalletDetails(it.id)
                        Log.d(TAG, "getWalletDetails: ${it.id}")
                    }else{
                        hideProgress()
                        binding.noWalletLayout.visible(true)
                        binding.createWallet.setOnClickListener {
                            val newWallet = WalletData(
                                walletId = hashString(
                                    "${auth.uid}${
                                        System.currentTimeMillis().toString()
                                    }"
                                ),
                                walletOwner = auth.uid!!,
                                walletBalance = "0.0"
                            )
                            //create wallet
                            createWallet(newWallet)
                        }

                    }

                }
        }
    }

    private fun fetchWalletDetails(walletId: String) {

        //requireContext().showProgress()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snapshot = Common.walletCollectionRef.document(walletId).get().await()
                if (snapshot.exists()) {
                    hideProgress()
                    val walletInfo = snapshot.toObject(WalletData::class.java)
                    withContext(Dispatchers.Main){
                        binding.walletBalance.apply {
                            visible(true)
                            text = resources.getString(
                                R.string.money_text,
                                walletInfo!!.walletBalance.toString()
                            )
                        }
                        binding.walletAddFundsText.apply {
                            visible(getUser(auth.uid!!)!!.role == clientRole)
                            setOnClickListener {
                                launchFundWalletDialog(walletId)
                            }
                        }

                        getWalletHistory(walletId)
                    }

                }
            } catch (e: Exception) {
                requireContext().toast(e.message.toString())
            }
        }
    }

    private fun launchFundWalletDialog(walletId: String) {
        val builder =
            layoutInflater.inflate(
                R.layout.custom_dialog_fund_wallet_layout,
                null
            )

        val tilFundAmount =
            builder.findViewById<TextInputLayout>(R.id.text_input_layout_fund_wallet_amount)
        val etFundAmount =
            builder.findViewById<TextInputEditText>(R.id.fund_wallet_amount)
        val btnFundWallet =
            builder.findViewById<Button>(R.id.btn_fund_wallet)
        val btnCancelFundWallet =
            builder.findViewById<Button>(R.id.btn_cancel_fund)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(builder)
            .setCancelable(false)
            .create()

        var fundAmount = ""

        btnFundWallet.enable(false)
        btnCancelFundWallet.setOnClickListener {
            dialog.dismiss()
        }

        etFundAmount.addTextChangedListener { amount ->
            fundAmount = amount.toString().trim()
            btnFundWallet.apply {
                enable(fundAmount.isNotEmpty())
                setOnClickListener {
                    val newBalance =
                        getWalletInfo(walletId)!!.walletBalance.toDouble() + fundAmount.toDouble()
                    CoroutineScope(Dispatchers.IO).launch {
                        val walletReference =
                            walletCollectionRef.document(walletId)

                        val updates = hashMapOf<String, Any>(
                            "walletBalance" to newBalance.toString(),
                        )

                        walletReference.update(updates)
                            .addOnSuccessListener {
                                //hideProgress()
                                //launch dialog to enter picker details
                                //update wallet transaction
                                CoroutineScope(Dispatchers.IO).launch {
                                    val walletHistoryReference =
                                        walletCollectionRef.document(walletId).collection(
                                            WALLET_HISTORY_REF
                                        )
                                    val walletTransaction = WalletHistory(
                                        transactionDate = getDate(
                                            System.currentTimeMillis(),
                                            DATE_FORMAT_LONG
                                        ),
                                        transactionType = "CR",
                                        transactionAmount = resources.getString(
                                            R.string.money_text,
                                            fundAmount
                                        ),
                                        transactionReason = REASON_ACCOUNT_FUND
                                    )

                                    walletHistoryReference.document(
                                        System.currentTimeMillis().toString()
                                    ).set(walletTransaction).await()
                                    fetchWalletDetails(walletId)

                                    dialog.dismiss()

                                }

                            }
                    }
                }
            }

        }
        dialog.show()
    }


    private fun getWalletHistory(walletId: String) {


        val walletHistory =
            walletCollectionRef.document(walletId).collection(WALLET_HISTORY_REF)

        val options = FirestoreRecyclerOptions.Builder<WalletHistory>()
            .setQuery(walletHistory, WalletHistory::class.java).build()
        try {
            walletHistoryAdapter = object :
                FirestoreRecyclerAdapter<WalletHistory, WalletHistoryAdapter>(options) {
                override fun onCreateViewHolder(
                    parent: ViewGroup,
                    viewType: Int
                ): WalletHistoryAdapter {
                    val itemView = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_wallet_history_layout, parent, false)
                    return WalletHistoryAdapter(itemView)
                }

                override fun onBindViewHolder(
                    holder: WalletHistoryAdapter,
                    position: Int,
                    model: WalletHistory
                ) {
                    //val timeToDeliver = model.supposedTime
                    holder.walletTransactionDate.text = model.transactionDate
                    holder.walletTransactionType.text = model.transactionType
                    holder.walletTransactionReason.text = model.transactionReason
                    holder.walletTransactionAmount.text = model.transactionAmount
                }
            }

        } catch (e: Exception) {
            requireActivity().toast(e.message.toString())
        }
        walletHistoryAdapter?.startListening()
        binding.rvWalletHistory.adapter = walletHistoryAdapter


    }

    private fun createWallet(newWallet: WalletData) {
        requireContext().showProgress()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                walletCollectionRef.document(newWallet.walletId).set(newWallet).addOnSuccessListener {
                    userCollectionRef.document(auth.uid!!).update("wallet", newWallet.walletId)
                hideProgress()
                binding.noWalletLayout.visible(false)
                binding.walletLayout.visible(true)
                fetchWalletDetails(newWallet.walletId)
                }//.await()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    activity?.toast(e.message.toString())
                }
            }
        }


    }


    private fun getUser(userId: String): UserData? {
        requireContext().showProgress()
        val deferred = CoroutineScope(Dispatchers.IO).async {
            try {
                val snapshot = Common.userCollectionRef.document(userId).get().await()
                if (snapshot.exists()) {
                    return@async snapshot.toObject(UserData::class.java)
                } else {
                    return@async null
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    requireContext().toast(e.message.toString())
                }
                return@async null
            }
        }

        val driverUser = runBlocking { deferred.await() }
        hideProgress()

        return driverUser
    }

    private fun getWalletInfo(walletId: String): WalletData? {

        requireContext().showProgress()
        val deferred = CoroutineScope(Dispatchers.IO).async {
            try {
                val snapshot = Common.walletCollectionRef.document(walletId).get().await()
                if (snapshot.exists()) {
                    return@async snapshot.toObject(WalletData::class.java)
                } else {
                    return@async null
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    requireContext().toast(e.message.toString())
                }
                return@async null
            }
        }

        val walletInfo = runBlocking { deferred.await() }
        hideProgress()

        return walletInfo
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding
    }
}