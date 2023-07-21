package com.androidstrike.schoolprojects.thefreighterapp.features.home.client.dispatch.pending

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.androidstrike.schoolprojects.thefreighterapp.R
import com.androidstrike.schoolprojects.thefreighterapp.databinding.FragmentPendingDispatchBinding
import com.androidstrike.schoolprojects.thefreighterapp.features.home.client.dispatch.DispatchScreenLandingDirections
import com.androidstrike.schoolprojects.thefreighterapp.models.Dispatch
import com.androidstrike.schoolprojects.thefreighterapp.models.UserData
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.DATE_FORMAT
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.STATUS_DELIVERED
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.dispatchCollectionRef
import com.androidstrike.schoolprojects.thefreighterapp.utils.getDate
import com.androidstrike.schoolprojects.thefreighterapp.utils.toast
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class PendingDispatch : Fragment() {

    private var _binding: FragmentPendingDispatchBinding? = null
    private val binding get() = _binding!!

    private var pendingDispatchAdapter: FirestoreRecyclerAdapter<Dispatch, PendingDispatchAdapter>? =
        null

    private val TAG = "PendingDispatch"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentPendingDispatchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getUser()

        with(binding) {
            val layoutManager = LinearLayoutManager(requireContext())
            rvOngoingDispatchList.layoutManager = layoutManager
            rvOngoingDispatchList.addItemDecoration(
                DividerItemDecoration(
                    requireContext(),
                    layoutManager.orientation
                )
            )
        }

    }

    private fun getUser(){
        var loggedUser = UserData()

        CoroutineScope(Dispatchers.IO).launch {
            Common.userCollectionRef.document(Common.auth.uid.toString())
//            Common.userCollectionRef.document("ElE9dfN1rXVaJ0MD8IsJv046BnV2")
                .addSnapshotListener { value, error ->
                    if (error != null) {
                        requireContext().toast(error.message.toString())
                        return@addSnapshotListener
                    }
                    if (value != null && value.exists()) {
                        loggedUser = value.toObject(UserData::class.java)!!

                        getRealtimePendingDispatch(loggedUser)



                    }
                }
//
        }
    }

    private fun getRealtimePendingDispatch(loggedUser: UserData) {

        Log.d(TAG, "getRealtimePendingDispatch: $loggedUser")
        //remember to use the user's role but ch
        val pendingDispatches =
            dispatchCollectionRef.whereEqualTo("client", loggedUser.userId).whereNotEqualTo("status", STATUS_DELIVERED)
        val options = FirestoreRecyclerOptions.Builder<Dispatch>()
            .setQuery(pendingDispatches, Dispatch::class.java).build()
        try {
            pendingDispatchAdapter = object :
                FirestoreRecyclerAdapter<Dispatch, PendingDispatchAdapter>(options) {
                override fun onCreateViewHolder(
                    parent: ViewGroup,
                    viewType: Int
                ): PendingDispatchAdapter {
                    val itemView = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_ongoing_dispatch_layout, parent, false)
                    return PendingDispatchAdapter(itemView)
                }

                override fun onBindViewHolder(
                    holder: PendingDispatchAdapter,
                    position: Int,
                    model: Dispatch
                ) {

                    Log.d(TAG, "onBindViewHolder: $model")

                    //val timeToDeliver = model.supposedTime
                    holder.pendingDispatchStatus.text = model.status
                    holder.pendingDispatchDateCreated.text = getDate(model.dateCreated.toLong(), DATE_FORMAT)
                    holder.pendingDispatchDestination.text = resources.getString(R.string.tv_dispatch_destination, model.dropOffProvince, model.dropOffCountry).ifEmpty { resources.getString(R.string.no_destination) }
                    holder.pendingDispatchDriverName.text = if (model.driver.isNotEmpty()){
                        resources.getString(R.string.dispatch_driver, getDispatchDriver(model.driver).fullName)} else { resources.getString(R.string.no_driver) }


                    holder.itemView.setOnClickListener {
                        // TODO: see details of order
                    }

                }

                //holder.managerPendingOrderTimeToDeliver.text = checkStaffStatus(model)

            }

        } catch (e: Exception) {
            requireActivity().toast(e.message.toString())
        }
        pendingDispatchAdapter?.startListening()
        binding.rvOngoingDispatchList.adapter = pendingDispatchAdapter



    }

    private fun getDispatchDriver(driver: String): UserData {

        var driverUser = UserData()

        CoroutineScope(Dispatchers.IO).launch {
            Common.userCollectionRef.document(driver)
//            Common.userCollectionRef.document("ElE9dfN1rXVaJ0MD8IsJv046BnV2")
                .addSnapshotListener { value, error ->
                    if (error != null) {
                        requireContext().toast(error.message.toString())
                        return@addSnapshotListener
                    }
                    if (value != null && value.exists()) {
                        driverUser = value.toObject(UserData::class.java)!!


                    }

                }
//
        }
        return driverUser
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}