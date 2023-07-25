package com.androidstrike.schoolprojects.thefreighterapp.features.home.client.dispatch.pending

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
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
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.STATUS_AWAITING_DRIVER
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.STATUS_AWAITING_WEIGHER
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.STATUS_DELIVERED
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.STATUS_DRAFT
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.STATUS_IN_TRANSIT
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.STATUS_NEGOTIATING_PRICE
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.STATUS_PENDING_DRIVER
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.STATUS_RATED
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.dispatchCollectionRef
import com.androidstrike.schoolprojects.thefreighterapp.utils.getDate
import com.androidstrike.schoolprojects.thefreighterapp.utils.hideProgress
import com.androidstrike.schoolprojects.thefreighterapp.utils.showProgress
import com.androidstrike.schoolprojects.thefreighterapp.utils.toast
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PendingDispatch : Fragment() {

    private var _binding: FragmentPendingDispatchBinding? = null
    private val binding get() = _binding!!

    private var pendingDispatchAdapter: FirestoreRecyclerAdapter<Dispatch, PendingDispatchAdapter>? =
        null

    private lateinit var pendingDispatches: Query

    private var interestedDriversMutableMap: MutableMap<String, String> = mutableMapOf()

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


        getUser("")

        with(binding) {
            fabAddDispatch.setOnClickListener {
                getUser(resources.getString(R.string.add_dispatch))
            }
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

    private fun getUser(request: String) {
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

                        if (request == resources.getString(R.string.add_dispatch)){
                            parentFragment?.let {parentFragment ->
                                val navController = parentFragment.findNavController()
                                val navToNewDispatch = DispatchScreenLandingDirections.actionPendingDispatchToCreateNewDispatch(loggedUser, "")
                                navController.navigate(navToNewDispatch)
                            }
                        }else{
                            getRealtimePendingDispatch(loggedUser)
                        }
                    }
                }
        }
    }

    private fun getRealtimePendingDispatch(loggedUser: UserData) {

        Log.d(TAG, "getRealtimePendingDispatch: $loggedUser")

        when(loggedUser.role){
            resources.getString(R.string.client) ->{
                pendingDispatches =
                    dispatchCollectionRef.whereEqualTo("client", loggedUser.userId)
                        .whereNotEqualTo("status", STATUS_DELIVERED)
            }
            resources.getString(R.string.weigher) ->{
                 pendingDispatches = dispatchCollectionRef.whereEqualTo("weigher", loggedUser.userId)
                    .whereEqualTo("status", STATUS_AWAITING_WEIGHER)
            }
            resources.getString(R.string.driver) ->{
                pendingDispatches = if (loggedUser.dispatch.isEmpty()){
                    dispatchCollectionRef
                        .whereEqualTo("status", STATUS_PENDING_DRIVER)
                }else{
                    dispatchCollectionRef.whereEqualTo("driver", loggedUser)
                        .whereNotEqualTo("status", STATUS_DELIVERED)
                }
            }
        }

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
                    holder.pendingDispatchDateCreated.text =
                        getDate(model.dateCreated.toLong(), DATE_FORMAT)
                    holder.pendingDispatchDestination.text = resources.getString(
                        R.string.tv_dispatch_destination,
                        model.dropOffProvince,
                        model.dropOffCountry
                    ).ifEmpty { resources.getString(R.string.no_destination) }
                    holder.pendingDispatchDriverName.text = if (model.driver.isNotEmpty()) {
                        resources.getString(
                            R.string.dispatch_driver,
                            getDispatchDriver(model.driver).fullName
                        )
                    } else {
                        resources.getString(R.string.no_driver)
                    }


                    holder.itemView.setOnClickListener {
                        //launch a new screen
                        if (model.status == STATUS_DRAFT){
                            //open the add new dispatch screen and display the filled details
                            parentFragment?.let { parentFragment ->
                                val navController = parentFragment.findNavController()
                            val navToCreateDispatch = DispatchScreenLandingDirections.actionPendingDispatchToCreateNewDispatch(loggedUser, model.dispatchId)
                            navController.navigate(navToCreateDispatch)
                            }
                        }else{
                            launchDispatchDetailDialog(model, loggedUser)
                        }


                        // TODO: see details of order
                    }
                }
            }

        } catch (e: Exception) {
            requireActivity().toast(e.message.toString())
        }
        pendingDispatchAdapter?.startListening()
        binding.rvOngoingDispatchList.adapter = pendingDispatchAdapter


    }

    private fun launchDispatchDetailDialog(model: Dispatch, loggedUser: UserData) {
        when (model.status) {
            STATUS_AWAITING_WEIGHER -> {
                //open dialog for weigher to input the weight
            }

            STATUS_PENDING_DRIVER -> {
                //display dialog to select desired driver and begin negotiation.
                when (loggedUser.role) {
                    resources.getString(R.string.driver) -> {
                        //if (current time - model.statusChangeTime is equal or greater than 10 minutes){
                        // and if that time has elapsed, if there is any driver, they are told they cannot indicate anymore

                        // }else{
                        //if(model.interestedDrivers.isEmpty()){
                        //model.statusChangeTime = current time
                        // }
                        //if it is being accessed by a driver, he should indicate interest and enter his charge
//                                        tvInterested.setOnClickListener{
                        //set price box shows
//                                         dialog to set price
                        //interestedDriversMutableMap[loggedUser.userId] = "price from edit text"
                        //dispatch is updated with their id being added to the interested drivers map
//                                        }
                        //else he navigates away from the screen
                        // tvNotInterested.setOnClickListener{
//                                         navigate out of screen
//                                        }
                        //system calculates 10 minutes from when the status changed to pending driver,
                        // but if there is no driver, another 10 minutes is added.


                    }

                    resources.getString(R.string.client) -> {
                        //if it is being accessed by a client, they are to wait for drivers time to elapse
                        //if the time has elapsed...
                        // they see a dialog with the list of interested drivers and select who to continue negotiation with
                        //val listOfDrivers = model.interestedDrivers

                        // when they select the driver, n0 is set to the driver's price and round is round 0
                        //status changes to negotiating price
                    }
                }
            }

            STATUS_NEGOTIATING_PRICE -> {
                if (model.lastUpdater != loggedUser.userId) {
                    //should wait for the other person to say
                } else {
                    val n0 = resources.getString(R.string.negotiation_zero)
                    val n1 = resources.getString(R.string.negotiation_one)
                    val n2 = resources.getString(R.string.negotiation_two)
                    val n3 = resources.getString(R.string.negotiation_three)

                    //tvOriginalAmount.text = model.amount
                    when (model.negotiationRound) {
                        n0 -> {
                            //tvN0Amount is equal to the original amount the driver stated
                            //tv n2 and n3 are invisible
                            //while tv n1 is disabled until user clicks to negotiate
                            //if user clicks accept, the dispatch amount will be set to the n0 amount
                        }

                        n1 -> {
                            //enters as n0, when user submits price, changes to n1
                            //tv n1 amount .text = model.negotiationPrice1
                            //user is to agree or say price
                            //price is user price
                        }

                        n2 -> {
                            //tvN2Amount
                            //driver is to agree to n1 amount or say price
                            //price is driver counter price
                        }

                        n3 -> {
                            //client is to agree or say price
                        }
                    }
                }

            }

            STATUS_AWAITING_DRIVER -> {
                //if the client clicks in this status, a dialog pops up asking if the driver has arrived,
                //if yes, it changes the status if nom it remains same

                //if driver clicks in this status, they are told that the client is waiting for them and will update status when they pick dispatch

            }

            STATUS_IN_TRANSIT -> {
                //if client clicks in this stage, they see the card of the dispatch detail and have the options to call or locate driver
                //if they request location, the driver gets alert on his own card to share location, which is updated to the server and user can click it.

                //there will also be a place to confirm dispatch delivery
                //driver will first indicate and the client is prompted to confirm
            }

            STATUS_DELIVERED -> {
                //dispatch goes to concluded screen and client is prompted to rate

            }

            STATUS_RATED -> {

            }
        }

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

    private fun updateDispatchInfo(dispatch: Dispatch){

        requireContext().showProgress()

        //use this function for every update to the dispatch
        CoroutineScope(Dispatchers.IO).launch {
            val dispatchRef =
                //userCollectionRef.document(auth.uid.toString()).collection(GAME_DETAILS_REF)
                dispatchCollectionRef.document(dispatch.dispatchId)


            val updates = hashMapOf<String, Any>(
                "status" to dispatch.status
            )


            dispatchRef.update(updates).addOnSuccessListener {
                requireContext().toast("Update successful")
                hideProgress()
                //dismiss the dialog

            }.addOnFailureListener { e ->
                // Handle error
                hideProgress()
                Log.d(TAG, "updateGameDetails: ${e.toString()}")
                requireContext().toast(e.message.toString())
            }

        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}