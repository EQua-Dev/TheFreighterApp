package com.androidstrike.schoolprojects.thefreighterapp.features.home.client.dispatch.pending

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.androidstrike.schoolprojects.thefreighterapp.R
import com.androidstrike.schoolprojects.thefreighterapp.databinding.FragmentPendingDispatchBinding
import com.androidstrike.schoolprojects.thefreighterapp.features.home.client.dispatch.DispatchScreenLandingDirections
import com.androidstrike.schoolprojects.thefreighterapp.models.Dispatch
import com.androidstrike.schoolprojects.thefreighterapp.models.InterestedDriverDetail
import com.androidstrike.schoolprojects.thefreighterapp.models.UserData
import com.androidstrike.schoolprojects.thefreighterapp.models.WalletData
import com.androidstrike.schoolprojects.thefreighterapp.models.WalletHistory
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.DATE_FORMAT
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.DATE_FORMAT_SHORT
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.NOT_AVAILABLE
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.STATUS_AWAITING_DRIVER
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.STATUS_AWAITING_WEIGHER
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.STATUS_DELIVERED
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.STATUS_DRAFT
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.STATUS_IN_TRANSIT
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.STATUS_NEGOTIATING_PRICE
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.STATUS_PENDING_DRIVER
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.STATUS_RATED
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.auth
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.dispatchCollectionRef
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.userCollectionRef
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.walletCollectionRef
import com.androidstrike.schoolprojects.thefreighterapp.utils.enable
import com.androidstrike.schoolprojects.thefreighterapp.utils.getDate
import com.androidstrike.schoolprojects.thefreighterapp.utils.hideProgress
import com.androidstrike.schoolprojects.thefreighterapp.utils.isUpToTenMinutes
import com.androidstrike.schoolprojects.thefreighterapp.utils.showProgress
import com.androidstrike.schoolprojects.thefreighterapp.utils.toast
import com.androidstrike.schoolprojects.thefreighterapp.utils.visible
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.gms.location.LocationServices
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.hbb20.CountryCodePicker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class PendingDispatch : Fragment() {

    private var _binding: FragmentPendingDispatchBinding? = null
    private val binding get() = _binding!!

    private var pendingDispatchAdapter: FirestoreRecyclerAdapter<Dispatch, PendingDispatchAdapter>? =
        null

    private lateinit var interestedDriversAdapter: InterestedDriversAdapter

    private lateinit var pendingDispatches: Query

    private var interestedDriversMutableMap: MutableMap<String, String> = mutableMapOf()

    private val TAG = "PendingDispatch"

    private lateinit var clientRole: String
    private lateinit var driverRole: String
    private lateinit var weigherRole: String

    private lateinit var driverLocation: String

    private var dispatchForLocation = Dispatch()
    //private lateinit var driverLatitude: String

    private val locationPermissionCode = 101


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        Log.d(TAG, "onCreateView: ")
        _binding = FragmentPendingDispatchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: ")
        clientRole = resources.getString(R.string.client)
        driverRole = resources.getString(R.string.driver)
        weigherRole = resources.getString(R.string.weigher)

        getRealtimePendingDispatch()

        with(binding) {
            fabAddDispatch.apply {
                visible(getUser(auth.uid!!)!!.role == clientRole)
                setOnClickListener {
                    parentFragment?.let { parentFragment ->
                        val navController = parentFragment.findNavController()
                        val navToNewDispatch =
                            DispatchScreenLandingDirections.actionPendingDispatchToCreateNewDispatch(
                                getUser(auth.uid!!)!!,
                                ""
                            )
                        navController.navigate(navToNewDispatch)
                    }
                    //getUser(resources.getString(R.string.add_dispatch))
                }
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

    private fun getRealtimePendingDispatch() {

        val loggedUser = getUser(auth.uid!!)!!
        when (loggedUser.role) {
            resources.getString(R.string.client) -> {
                pendingDispatches =
                    dispatchCollectionRef.whereEqualTo("client", loggedUser.userId)
                        .whereNotEqualTo("status", STATUS_DELIVERED)
            }

            resources.getString(R.string.weigher) -> {
                pendingDispatches = dispatchCollectionRef.whereEqualTo("weigher", loggedUser.userId)
                    .whereEqualTo("status", STATUS_AWAITING_WEIGHER)
            }

            resources.getString(R.string.driver) -> {
                Log.d(TAG, "getRealtimePendingDispatch: ${loggedUser.dispatch.isEmpty()}")
                pendingDispatches = if (loggedUser.dispatch.isEmpty()) {
                    dispatchCollectionRef
                        .whereEqualTo("status", STATUS_PENDING_DRIVER)
                } else {
                    dispatchCollectionRef.whereEqualTo("driver", loggedUser.userId)
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
                            getDispatchDriver(model.driver)!!.fullName
                        )
                    } else {
                        resources.getString(R.string.no_driver)
                    }

                    if (model.client == loggedUser.userId) {
                        Log.d(TAG, "onBindViewHolder: ${model.statusChangeTime}")
                        if (!isUpToTenMinutes(model.statusChangeTime.toLong()) && model.interestedDrivers.isEmpty()) {
                            holder.pendingDispatchDriverName.apply {
                                text = resources.getString(R.string.add_driver_search_time)
                                setTextColor(resources.getColor(R.color.primary))
                                setOnClickListener {

                                    CoroutineScope(Dispatchers.IO).launch {
                                        val dispatchCollectionRef =
                                            dispatchCollectionRef.document(model.dispatchId)

                                        val updates = hashMapOf<String, Any>(
                                            "statusChangeTime" to System.currentTimeMillis()
                                                .toString(),
                                        )

                                        dispatchCollectionRef.update(updates).addOnSuccessListener {
                                            hideProgress()
                                            requireContext().toast(resources.getString(R.string.time_added))
                                            //Log.d(TAG, "onBindViewHolder: ")
                                            getRealtimePendingDispatch()
                                        }
                                    }
                                }
                            }
                        }
                    }

                    holder.pendingDispatchBadge.visible(model.driver == loggedUser.userId && model.userLocationRequest)

                    holder.itemView.setOnClickListener {
                        //launch a new screen
                        if (model.status == STATUS_DRAFT) {
                            //open the add new dispatch screen and display the filled details
                            parentFragment?.let { parentFragment ->
                                val navController = parentFragment.findNavController()
                                val navToCreateDispatch =
                                    DispatchScreenLandingDirections.actionPendingDispatchToCreateNewDispatch(
                                        loggedUser,
                                        model.dispatchId
                                    )
                                navController.navigate(navToCreateDispatch)
                            }
                        } else {
                            handleDispatchesStatuses(model)
                        }
                    }
                }
            }

        } catch (e: Exception) {
            requireActivity().toast(e.message.toString())
        }
        pendingDispatchAdapter?.startListening()
        binding.rvOngoingDispatchList.adapter = pendingDispatchAdapter


    }

    private fun handleDispatchesStatuses(model: Dispatch) {

        val loggedUser = getUser(auth.uid!!)!!
        when (model.status) {
            STATUS_AWAITING_WEIGHER -> {
                //open dialog for weigher to input the weight
                if (model.weigher == loggedUser.userId) {
                    launchWeigherAddWeightDialog(model)
                } else {
                    requireContext().toast(resources.getString(R.string.waiting_for_weight))
                }
            }

            STATUS_PENDING_DRIVER -> {
                //display dialog to select desired driver and begin negotiation.
                when (loggedUser.role) {
                    resources.getString(R.string.driver) -> {
                        if (!isUpToTenMinutes(model.statusChangeTime.toLong())) {// && model.interestedDrivers.isNotEmpty()){
                            //if (current time - model.statusChangeTime is equal or greater than 10 minutes){
                            // and if that time has elapsed, if there is any driver, they are told they cannot indicate anymore
                            Log.d(TAG, "launchDispatchDetailDialog: ${model.statusChangeTime}")

                            requireContext().toast(resources.getString(R.string.time_elapsed))
                        } else if (model.interestedDrivers.keys.contains(loggedUser.userId)) {
                            requireContext().toast(resources.getString(R.string.already_interested))
                        } else {

                            // TODO: extract this to an individual function
                            //display the dialog for driver to indicate interest or not
                            val builder =
                                layoutInflater.inflate(
                                    R.layout.custom_driver_indicate_interest_dialog,
                                    null
                                )

                            val tvDispatchPickUpDate =
                                builder.findViewById<TextView>(R.id.driver_interest_dialog_pickup_date)
                            val tvDispatchWeight =
                                builder.findViewById<TextView>(R.id.driver_interest_dialog_pickup_package_type)
                            val tvDispatchPickUpAddress =
                                builder.findViewById<TextView>(R.id.driver_interest_dialog_pickup_address_text)
                            val tvDispatchDropOffAddress =
                                builder.findViewById<TextView>(R.id.driver_interest_dialog_drop_off_address_text)
                            val cbDriverInterested =
                                builder.findViewById<CheckBox>(R.id.driver_interest_dialog_interested_checkbox)
                            val tilDriverCharge =
                                builder.findViewById<TextInputLayout>(R.id.text_input_layout_driver_charge)
                            val etDriverCharge =
                                builder.findViewById<TextInputEditText>(R.id.set_driver_charge)

                            val tvDriverNotInterested =
                                builder.findViewById<TextView>(R.id.driver_interest_dialog_not_interested_text)
                            val tvSubmitDriverInterested =
                                builder.findViewById<TextView>(R.id.driver_interest_dialog_interested_submit_text)

                            tvDispatchPickUpDate.text =
                                resources.getString(R.string.driver_pickup_date, model.pickupDate)
                            tvDispatchWeight.text = resources.getString(
                                R.string.driver_pickup_package_type,
                                model.packageType,
                                model.weight
                            )
                            tvDispatchPickUpAddress.text = resources.getString(
                                R.string.dispatch_detail_address,
                                model.pickupAddress,
                                model.pickupProvince,
                                model.pickupCountry
                            )
                            tvDispatchDropOffAddress.text = resources.getString(
                                R.string.dispatch_detail_address,
                                model.dropOffAddress,
                                model.dropOffProvince,
                                model.dropOffCountry
                            )

                            var driverCharge = ""

                            val dialog = AlertDialog.Builder(requireContext())
                                .setView(builder)
                                .setCancelable(false)
                                .create()

                            tvSubmitDriverInterested.visible(false)
                            tilDriverCharge.enable(false)

                            cbDriverInterested.setOnCheckedChangeListener { buttonView, isChecked ->
                                if (isChecked) {
                                    tilDriverCharge.enable(true)
                                    tvDriverNotInterested.visible(false)
                                    etDriverCharge.addTextChangedListener {
                                        driverCharge = it.toString().trim()
                                        tvSubmitDriverInterested.apply {
                                            visible(driverCharge.isNotEmpty())
                                            setOnClickListener {
                                                //add the driver and his price to the dispatch
                                                interestedDriversMutableMap =
                                                    model.interestedDrivers.toMutableMap()
                                                interestedDriversMutableMap[loggedUser.userId] =
                                                    driverCharge


                                                CoroutineScope(Dispatchers.IO).launch {
                                                    val dispatchCollectionRef =
                                                        dispatchCollectionRef.document(model.dispatchId)

                                                    val updates = hashMapOf<String, Any>(
                                                        "interestedDrivers" to interestedDriversMutableMap,
                                                    )

                                                    dispatchCollectionRef.update(updates)
                                                        .addOnSuccessListener {
                                                            hideProgress()
                                                            dialog.dismiss()
                                                            getRealtimePendingDispatch()

                                                        }

                                                }

                                            }
                                        }
                                    }
                                } else {
                                    tilDriverCharge.enable(false)
                                    tvDriverNotInterested.visible(true)
                                    tvSubmitDriverInterested.visible(false)
                                }
                            }

                            tvDriverNotInterested.setOnClickListener {
                                dialog.dismiss()
                            }


                            dialog.show()
                        }
                    }

                    resources.getString(R.string.client) -> {
                        //if it is being accessed by a client, they are to wait for drivers time to elapse
                        if (!isUpToTenMinutes(model.statusChangeTime.toLong())) {
                            if (model.interestedDrivers.isEmpty()) {
                                requireContext().toast(resources.getString(R.string.no_driver_add_time))
                            } else {
                                //display dialog to show list of interested drivers
                                launchAssignDriverDialog(model)
                            }
                        } else {
                            //if not up to 10 minutes
                            requireContext().toast(resources.getString(R.string.still_pending_drivers))
                        }

                        //if the time has elapsed...
                        // they see a dialog with the list of interested drivers and select who to continue negotiation with
                        //val listOfDrivers = model.interestedDrivers

                        // when they select the driver, n0 is set to the driver's price and round is round 0
                        //status changes to negotiating price
                    }
                }
            }

            STATUS_NEGOTIATING_PRICE -> {
                if (model.lastUpdater == loggedUser.userId) {
                    //should wait for the other person to say
                    requireContext().toast(resources.getString(R.string.wait_for_response))
                } else {
                    launchNegotiationDialog(model, loggedUser)
                }
            }

            STATUS_AWAITING_DRIVER -> {

                launchDispatchDetails(model, STATUS_AWAITING_DRIVER)
                //if the client clicks in this status, a dialog pops up asking if the driver has arrived,
                //if yes, it changes the status if nom it remains same

                //if driver clicks in this status, they are told that the client is waiting for them
                // and will update status when they pick dispatch

            }

            STATUS_IN_TRANSIT -> {
                launchDispatchDetails(model, STATUS_IN_TRANSIT)

                //if client clicks in this stage, they see the card of the dispatch detail
                // and have the options to call or locate driver
                //if they request location, the driver gets alert on his own card to share location,
                // which is updated to the server and user can click it.

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

    private fun launchDispatchDetails(dispatch: Dispatch, status: String) {

        val builder =
            layoutInflater.inflate(R.layout.fragment_dispatch_details, null)

        val dispatchDateCreated =
            builder.findViewById<TextView>(R.id.dispatch_detail_date_created)
        val dispatchPackageType =
            builder.findViewById<TextView>(R.id.dispatch_detail_package_type)
        val dispatchDriverName =
            builder.findViewById<TextView>(R.id.dispatch_detail_driver_name)
        val dispatchDatePickedUp =
            builder.findViewById<TextView>(R.id.dispatch_detail_date_picked_up)
        val dispatchDateDelivered =
            builder.findViewById<TextView>(R.id.dispatch_detail_date_delivered)
        val dispatchPickupAddress =
            builder.findViewById<TextView>(R.id.dispatch_detail_pickup_address)
        val dispatchDropOffAddress =
            builder.findViewById<TextView>(R.id.dispatch_detail_drop_off_address)
        val dispatchPickerName =
            builder.findViewById<TextView>(R.id.dispatch_detail_picker_name)
        val dispatchStatus =
            builder.findViewById<TextView>(R.id.dispatch_detail_status)
        val dispatchAmount =
            builder.findViewById<TextView>(R.id.dispatch_detail_amount)
        val dispatchCallDriver =
            builder.findViewById<TextView>(R.id.dispatch_detail_call_driver)
        val dispatchConfirmPickup =
            builder.findViewById<TextView>(R.id.dispatch_detail_confirm_pickup)
        val dispatchConfirmDelivery =
            builder.findViewById<TextView>(R.id.dispatch_detail_confirm_delivery)
        val dispatchPickupDirection =
            builder.findViewById<ImageView>(R.id.dispatch_navigate_pickup_address)
        val dispatchDropOffDirection =
            builder.findViewById<ImageView>(R.id.dispatch_navigate_drop_off_address)
        val dispatchCallPicker =
            builder.findViewById<TextView>(R.id.dispatch_detail_call_picker)
        val dispatchDetailOkayBtn =
            builder.findViewById<Button>(R.id.dispatch_details_okay_button)
        val dispatchDetailRequestTracking =
            builder.findViewById<TextView>(R.id.dispatch_detail_request_tracking)
        val dispatchDetailDriverLocation =
            builder.findViewById<TextView>(R.id.dispatch_detail_driver_location)


        val dialog = AlertDialog.Builder(requireContext())
            .setView(builder)
            .setCancelable(false)
            .create()

        dispatchDetailOkayBtn.setOnClickListener {
            dialog.dismiss()
        }

        val loggedUser = getUser(auth.uid!!)!!



        dispatchDateCreated.text = resources.getString(
            R.string.dispatch_detail_date_created,
            getDate(dispatch.dateCreated.toLong(), DATE_FORMAT_SHORT)
        )
        dispatchPackageType.text = resources.getString(
            R.string.driver_pickup_package_type,
            dispatch.packageType,
            dispatch.weight
        )
        dispatchDriverName.text = if (loggedUser.role == clientRole) {
            resources.getString(
                R.string.dispatch_driver,
                getDispatchDriver(dispatch.driver)!!.fullName
            )
        } else {
            resources.getString(
                R.string.dispatch_client,
                getDispatchDriver(dispatch.client)!!.fullName
            )
        }
        dispatchDatePickedUp.text = if (dispatch.datePickedUp.isNotEmpty()) {
            resources.getString(
                R.string.dispatch_date_picked_up,
                getDate(dispatch.datePickedUp.toLong(), DATE_FORMAT_SHORT)
            )
        } else {
            resources.getString(R.string.dispatch_date_picked_up, NOT_AVAILABLE)
        }
        dispatchDateDelivered.text = if (dispatch.dateDelivered.isNotEmpty()) {
            resources.getString(
                R.string.dispatch_date_delivered,
                getDate(dispatch.dateDelivered.toLong(), DATE_FORMAT_SHORT)
            )
        } else {
            resources.getString(R.string.dispatch_date_delivered, NOT_AVAILABLE)
        }
        dispatchPickupAddress.text = resources.getString(
            R.string.dispatch_detail_address,
            dispatch.pickupAddress,
            dispatch.pickupProvince,
            dispatch.pickupCountry
        )
        dispatchDropOffAddress.text = resources.getString(
            R.string.dispatch_detail_address,
            dispatch.dropOffAddress,
            dispatch.dropOffProvince,
            dispatch.dropOffCountry
        )
        dispatchPickerName.text =
            resources.getString(R.string.dispatch_detail_picker_name, dispatch.pickerName)
        dispatchStatus.text = dispatch.status
        dispatchAmount.text = if (status == STATUS_IN_TRANSIT) {
            resources.getString(R.string.dispatch_detail_money_text, dispatch.amount, "40%")
        } else if (status == STATUS_DELIVERED) {
            resources.getString(R.string.dispatch_detail_money_text, dispatch.amount, "100%")
        } else {
            resources.getString(R.string.dispatch_detail_money_text, dispatch.amount, "0%")

        }
        dispatchCallDriver.setOnClickListener {
            if (loggedUser.role == clientRole) {
                makeCall(getDispatchDriver(dispatch.driver)!!.phoneNumber)
            } else {
                makeCall(getDispatchDriver(dispatch.client)!!.phoneNumber)
            }
        }
        dispatchConfirmPickup.apply {
            enable(status != STATUS_IN_TRANSIT && loggedUser.role == clientRole)
            setOnClickListener {
                val alertDialogBuilder = AlertDialog.Builder(context)
                alertDialogBuilder.setTitle(resources.getString(R.string.confirm_service_creation_title))
                alertDialogBuilder.setMessage(
                    resources.getString(R.string.confirm_service_creation_text)
                )//, dispatch.amount))
                alertDialogBuilder.setPositiveButton("OK") { confirmDialog, _ ->
                    // Code to execute when the OK button is clicked
                    //change the status to in transit
                    //set pick up date
                    //update dispatch
                    requireContext().showProgress()
                    CoroutineScope(Dispatchers.IO).launch {
                        val dispatchCollectionRef =
                            dispatchCollectionRef.document(dispatch.dispatchId)
                        val fortyPercent = dispatch.amount.toDouble().times(0.4)
                        addDriverFunds(
                            fortyPercent,
                            dispatch.client,
                            dispatch.driver,
                            Common.REASON_PICKUP_PAY
                        )
                        val updates = hashMapOf<String, Any>(
                            "statusChangeTime" to System.currentTimeMillis()
                                .toString(),
                            "status" to STATUS_IN_TRANSIT,
                            "datePickedUp" to System.currentTimeMillis().toString(),
                        )

                        dispatchCollectionRef.update(updates)
                            .addOnSuccessListener {
                                hideProgress()
                                confirmDialog.dismiss()
                                lifecycleScope.launch {
                                    userCollectionRef.document(dispatch.driver)
                                        .update("dispatch", dispatch.dispatchId).await()
                                }

                                requireContext().toast(resources.getString(R.string.update_success))
                                dialog.dismiss()
                                getRealtimePendingDispatch()

                            }

                    }
                    confirmDialog.dismiss()
                }
                alertDialogBuilder.setNegativeButton("Cancel") { dialog, _ ->
                    // Code to execute when the Cancel button is clicked
                    dialog.dismiss()
                }

                val alertDialog: AlertDialog = alertDialogBuilder.create()
                alertDialog.show()
            }
        }
        dispatchConfirmDelivery.apply {
            enable(status == STATUS_IN_TRANSIT || loggedUser.role == clientRole)
            setOnClickListener {
                val alertDialogBuilder = AlertDialog.Builder(context)
                alertDialogBuilder.setTitle(resources.getString(R.string.confirm_service_delivery_title))
                alertDialogBuilder.setMessage(resources.getString(R.string.confirm_service_delivery_text))
                alertDialogBuilder.setPositiveButton("OK") { confirmDialog, _ ->
                    // Code to execute when the OK button is clicked
                    //change the status to in transit
                    //set pick up date
                    //update dispatch
                    CoroutineScope(Dispatchers.IO).launch {
                        val dispatchCollectionRef =
                            dispatchCollectionRef.document(dispatch.dispatchId)
                        val sixtyPercent = dispatch.amount.toDouble().times(0.6)
                        addDriverFunds(
                            sixtyPercent,
                            dispatch.client,
                            dispatch.driver,
                            Common.REASON_DELIVERY_PAY
                        )
                        val updates = hashMapOf<String, Any>(
                            "statusChangeTime" to System.currentTimeMillis()
                                .toString(),
                            "status" to STATUS_DELIVERED,
                            "rating" to "0",
                            "dateDelivered" to System.currentTimeMillis().toString(),
                        )

                        dispatchCollectionRef.update(updates)
                            .addOnSuccessListener {

                                //remove dispatch from driver
                                userCollectionRef.document(dispatch.dispatchId)
                                    .update("dispatch", "").addOnSuccessListener {

                                    hideProgress()
                                    confirmDialog.dismiss()
                                    requireContext().toast(resources.getString(R.string.update_success))
                                    dialog.dismiss()
                                    getRealtimePendingDispatch()
                                }

                            }

                    }
                    confirmDialog.dismiss()
                }
                alertDialogBuilder.setNegativeButton("Cancel") { dialog, _ ->
                    // Code to execute when the Cancel button is clicked
                    dialog.dismiss()
                }

                val alertDialog: AlertDialog = alertDialogBuilder.create()
                alertDialog.show()
            }
        }
        dispatchPickupDirection.setOnClickListener {
            locateCustomer(dispatchPickupAddress.text.toString())
        }
        dispatchDropOffDirection.setOnClickListener {
            locateCustomer(dispatchPickupAddress.text.toString())
        }
        dispatchCallPicker.setOnClickListener {
            makeCall(dispatch.pickerNumber)
        }
        dispatchDetailRequestTracking.apply {
            visible(loggedUser.role == clientRole && dispatch.status == STATUS_IN_TRANSIT && !dispatch.userLocationRequest)
            enable(!dispatch.userLocationRequest)
            setOnClickListener {
                //update the dispatch userRequestLocation to true, when the driver updates, it changes back to false
                CoroutineScope(Dispatchers.IO).launch {
                    val dispatchCollectionRef =
                        dispatchCollectionRef.document(dispatch.dispatchId)

                    val updates = hashMapOf<String, Any>(
                        "userLocationRequest" to true
                    )

                    dispatchCollectionRef.update(updates).addOnSuccessListener {
                        hideProgress()
                        requireContext().toast(resources.getString(R.string.location_request_sent))
                        dialog.dismiss()
                    }

                }

            }
        }
        dispatchDetailDriverLocation.apply {
            //if a driver is logged in, the text is "update location"
            if (status == STATUS_IN_TRANSIT) {

                if (loggedUser.role == driverRole) {
                    visible(dispatch.userLocationRequest)
//                    enable(
//                        dispatch.userLocationRequest
//                    )
                    text = resources.getString(R.string.update_location)
                    setOnClickListener {
                        //get the driver's current location and populate the database
                        //set the dispatch.userLocationRequest to false
                        dispatchForLocation = dispatch
                        checkLocationPermission(dispatch)
                    }
                } else {
                    text = resources.getString(R.string.show_in_map)
                        .ifEmpty { resources.getString(R.string.not_available) }
                    enable(dispatch.transitLocation.isNotEmpty())
                    setOnClickListener {
                        navigateToLocation(dispatch.transitLocation)
                        //locateCustomer(dispatch.transitLocation)
                    }
                }
            } else {
                visible(false)
            }
        }

        dialog.show()
    }

    private fun addDriverFunds(amount: Double, client: String, driver: String, reason: String) {
        CoroutineScope(Dispatchers.IO).launch {
            walletCollectionRef
                .get()
                .addOnSuccessListener { querySnapshot: QuerySnapshot ->
                    for (document in querySnapshot.documents) {
                        val item = document.toObject(WalletData::class.java)
                        if (item?.walletOwner == client) {
                            val newClientBalance = item.walletBalance.toDouble() - amount
                            CoroutineScope(Dispatchers.IO).launch {
                                walletCollectionRef.document(item.walletId)
                                    .update("walletBalance", newClientBalance.toString())
                                    .addOnSuccessListener {
                                        CoroutineScope(Dispatchers.IO).launch {
                                            val walletHistoryReference =
                                                walletCollectionRef.document(item.walletId)
                                                    .collection(
                                                        Common.WALLET_HISTORY_REF
                                                    )
                                            val walletTransaction = WalletHistory(
                                                transactionDate = getDate(
                                                    System.currentTimeMillis(),
                                                    Common.DATE_FORMAT_LONG
                                                ),
                                                transactionType = "DR",
                                                transactionAmount = resources.getString(
                                                    R.string.money_text,
                                                    amount.toString()
                                                ),
                                                transactionReason = reason
                                            )

                                            walletHistoryReference.document(
                                                System.currentTimeMillis().toString()
                                            ).set(walletTransaction).await()
                                        }

                                        //add to driver wallet
                                        walletCollectionRef
                                            .get()
                                            .addOnSuccessListener { driverWalletSnapshot: QuerySnapshot ->
                                                for (wallet in driverWalletSnapshot.documents) {
                                                    val driverWallet =
                                                        wallet.toObject(WalletData::class.java)
                                                    if (driverWallet?.walletOwner == driver) {
                                                        //add to weigher's wallet balance
                                                        val newBalance =
                                                            driverWallet.walletBalance.toDouble() + amount
                                                        walletCollectionRef.document(driverWallet.walletId)
                                                            .update(
                                                                "walletBalance",
                                                                newBalance.toString()
                                                            ).addOnSuccessListener {
                                                                CoroutineScope(Dispatchers.IO).launch {
                                                                    val walletHistoryReference =
                                                                        walletCollectionRef.document(
                                                                            driverWallet.walletId
                                                                        ).collection(
                                                                            Common.WALLET_HISTORY_REF
                                                                        )
                                                                    val walletTransaction =
                                                                        WalletHistory(
                                                                            transactionDate = getDate(
                                                                                System.currentTimeMillis(),
                                                                                Common.DATE_FORMAT_LONG
                                                                            ),
                                                                            transactionType = "CR",
                                                                            transactionAmount = resources.getString(
                                                                                R.string.money_text,
                                                                                amount.toString()
                                                                            ),
                                                                            transactionReason = reason
                                                                        )

                                                                    walletHistoryReference.document(
                                                                        System.currentTimeMillis()
                                                                            .toString()
                                                                    ).set(walletTransaction).await()
                                                                }
                                                                hideProgress()
                                                            }
                                                    }
                                                }

                                            }
                                    }
                            }
                        }
                        //get the wallet of the client
                        //subtract the weigher's cost
                        //update the new balance
                        //get the weigher's wallet
                        //add the funds to the weigher's wallet
                        //update the new balance
                    }
                }
        }

    }

    private fun launchWeigherAddWeightDialog(model: Dispatch) {

        val builder =
            layoutInflater.inflate(R.layout.set_weigher_charge_dialog, null)

        val etPackageWeight =
            builder.findViewById<TextInputEditText>(R.id.set_weigher_charge)
        val tilPackageWeight =
            builder.findViewById<TextInputLayout>(R.id.text_input_layout_weigher_charge)
        val weigherChargeTitle =
            builder.findViewById<TextView>(R.id.weigher_set_price_subtitle)

        weigherChargeTitle.text = resources.getString(R.string.dispatch_package_weight)
        tilPackageWeight.setHint(resources.getString(R.string.package_weight))


        val btnContinue =
            builder.findViewById<Button>(R.id.set_weigher_charge_btn)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(builder)
            .setCancelable(false)
            .create()

        btnContinue.enable(false)

        etPackageWeight.addTextChangedListener {
            val packageWeight = it.toString().trim()
            btnContinue.apply {
                enable(packageWeight.isNotEmpty())
                setOnClickListener {
                    CoroutineScope(Dispatchers.IO).launch {
                        val dispatchCollectionRef =
                            dispatchCollectionRef.document(model.dispatchId)

                        val updates = hashMapOf<String, Any>(
                            "status" to STATUS_PENDING_DRIVER,
                            "weight" to packageWeight,
                            "dateWeighed" to System.currentTimeMillis().toString(),
                            "statusChangeTime" to System.currentTimeMillis().toString()
                        )

                        dispatchCollectionRef.update(updates).addOnSuccessListener {
                            hideProgress()
                            dialog.dismiss()
                            addWeighingFunds(model.dispatchId, model.weigher, model.client)
                            getRealtimePendingDispatch()

                        }

                    }
                }
            }
        }

        dialog.show()

    }

    private fun addWeighingFunds(dispatchId: String, weigher: String, client: String) {
        requireContext().showProgress()
        CoroutineScope(Dispatchers.IO).launch {
            walletCollectionRef
                .get()
                .addOnSuccessListener { querySnapshot: QuerySnapshot ->

                    for (document in querySnapshot.documents) {
                        val item = document.toObject(WalletData::class.java)
                        if (item?.walletOwner == client) {
                            val newClientBalance =
                                item.walletBalance.toDouble() - getDispatchDriver(weigher)?.weigherCost?.toDouble()!!
                            CoroutineScope(Dispatchers.IO).launch {
                                walletCollectionRef.document(item.walletId)
                                    .update("walletBalance", newClientBalance.toString())
                                    .addOnSuccessListener {
                                        CoroutineScope(Dispatchers.IO).launch {
                                            val walletHistoryReference =
                                                walletCollectionRef.document(item.walletId)
                                                    .collection(
                                                        Common.WALLET_HISTORY_REF
                                                    )

                                            val walletTransaction = WalletHistory(
                                                transactionDate = getDate(
                                                    System.currentTimeMillis(),
                                                    Common.DATE_FORMAT_LONG
                                                ),
                                                transactionType = "DR",
                                                transactionAmount = resources.getString(
                                                    R.string.money_text,
                                                    getDispatchDriver(weigher)?.weigherCost
                                                ),
                                                transactionReason = Common.REASON_WEIGH_PAY
                                            )

                                            walletHistoryReference.document(
                                                System.currentTimeMillis().toString()
                                            ).set(walletTransaction).await()


                                        }
                                        walletCollectionRef
                                            .get()
                                            .addOnSuccessListener { weigherWalletSnapshot: QuerySnapshot ->
                                                val weigherInfo = getDispatchDriver(weigher)!!
                                                val weigherWallet =
                                                    getDispatchDriver(weigher)!!.wallet
                                                val weigherWalletBalance =
                                                    getWalletInfo(weigher)!!.walletBalance
                                                val newWeigherBalance =
                                                    weigherWalletBalance.toDouble()
                                                        .plus(weigherInfo.weigherCost.toDouble())
                                                walletCollectionRef.document(weigherWallet).update(
                                                    "walletBalance",
                                                    newWeigherBalance.toString()
                                                ).addOnSuccessListener {
                                                    val walletHistoryReference =
                                                        walletCollectionRef.document(
                                                            weigherWallet
                                                        ).collection(
                                                            Common.WALLET_HISTORY_REF
                                                        )
                                                    val walletTransaction =
                                                        WalletHistory(
                                                            transactionDate = getDate(
                                                                System.currentTimeMillis(),
                                                                Common.DATE_FORMAT_LONG
                                                            ),
                                                            transactionType = "CR",
                                                            transactionAmount = resources.getString(
                                                                R.string.money_text,
                                                                getDispatchDriver(
                                                                    weigher
                                                                )?.weigherCost
                                                            ),
                                                            transactionReason = Common.REASON_WEIGH_PAY
                                                        )

                                                    walletHistoryReference.document(
                                                        System.currentTimeMillis()
                                                            .toString()
                                                    ).set(walletTransaction).addOnCompleteListener {
                                                        hideProgress()

                                                    }
                                                }
                                            }
                                    }
                            }
                        }
                        //get the wallet of the client
                        //subtract the weigher's cost
                        //update the new balance
                        //get the weigher's wallet
                        //add the funds to the weigher's wallet
                        //update the new balance
                    }

                }
        }
        // }

    }

    private fun launchNegotiationDialog(model: Dispatch, loggedUser: UserData) {
        val builder =
            layoutInflater.inflate(
                R.layout.dialog_negotiate_dispatch_price_layout,
                null
            )

        val dispatchDriverName =
            builder.findViewById<TextView>(R.id.dispatch_driver_name)
        val dispatchDriverPrice =
            builder.findViewById<TextView>(R.id.dispatch_driver_price)
        val dispatchPickupDate =
            builder.findViewById<TextView>(R.id.dispatch_pickup_date)
        val dispatchCallDriver =
            builder.findViewById<TextView>(R.id.call_dispatch_driver)
        val cbClientNegotiate =
            builder.findViewById<CheckBox>(R.id.cb_negotiate_dispatch_price)
        val negotiation1Price =
            builder.findViewById<TextView>(R.id.negotiation_one_price)
        val etClientCounterPrice1 =
            builder.findViewById<EditText>(R.id.et_counter_price_one)
        val negotiation2Price =
            builder.findViewById<TextView>(R.id.negotiation_two_price)
        val etClientCounterPrice2 =
            builder.findViewById<EditText>(R.id.et_counter_price_two)
        val negotiation3Price =
            builder.findViewById<TextView>(R.id.negotiation_three_price)
        val etClientCounterPrice3 =
            builder.findViewById<EditText>(R.id.et_counter_price_three)
        val tvNegotiationInfo =
            builder.findViewById<TextView>(R.id.negotiation_info)
        val btnCancelNegotiation =
            builder.findViewById<Button>(R.id.negotiation_cancel_btn)
        val btnSubmitNegotiation =
            builder.findViewById<Button>(R.id.negotiation_submit_btn)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(builder)
            .setCancelable(false)
            .create()

        val n0 = resources.getString(R.string.negotiation_zero)
        val n1 = resources.getString(R.string.negotiation_one)
        val n2 = resources.getString(R.string.negotiation_two)
        val n3 = resources.getString(R.string.negotiation_three)
        val driverDetail = getDispatchDriver(model.driver)

        // TODO:  handle cases of when user unchecks the checkbox to make view states to be how they were

        negotiation1Price.visible(false)
        negotiation2Price.visible(false)
        negotiation3Price.visible(false)
        etClientCounterPrice1.enable(false)
        etClientCounterPrice2.enable(false)
        etClientCounterPrice3.enable(false)

        dispatchDriverName.text = driverDetail!!.fullName
        dispatchDriverPrice.text = model.negotiationPrice0
        dispatchPickupDate.text = resources.getString(R.string.driver_pickup_date, model.pickupDate)
        dispatchCallDriver.setOnClickListener {
            val number = if (loggedUser.role == resources.getString(R.string.client)) {
                getDispatchDriver(model.driver)!!.phoneNumber
            } else {
                getDispatchDriver(model.client)!!.phoneNumber
            }
            makeCall(number)
        }

        var negotiationPrice2 = ""
        var negotiationPrice3 = ""

        when (model.negotiationRound) {
            n0 -> {
                //tvN0Amount is equal to the original amount the driver stated
                //tv n2 and n3 are invisible
                //while tv n1 is disabled until user clicks to negotiate
                //if user clicks accept, the dispatch amount will be set to the n0 amount
                //last updater here is null and is set to driver and this will never be the case
            }

            n1 -> {

                //means user has given counter price
                etClientCounterPrice1.visible(false)
                negotiation1Price.visible(true)
                negotiation1Price.text = model.negotiationPrice1
                if (loggedUser.role == resources.getString(R.string.client)) {
                    cbClientNegotiate.enable(false)
                    btnSubmitNegotiation.apply {
                        text = resources.getString(R.string.okay)
                        setOnClickListener {
                            dialog.dismiss()
                        }
                    }

                } else {
                    cbClientNegotiate.setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked) {
                            etClientCounterPrice2.enable(true)
                            btnSubmitNegotiation.enable(false)
                            etClientCounterPrice2.addTextChangedListener {
                                negotiationPrice2 = it.toString().trim()
                                btnSubmitNegotiation.apply {
                                    enable(negotiationPrice2.isNotEmpty())
                                    setOnClickListener {
                                        CoroutineScope(Dispatchers.IO).launch {
                                            val dispatchCollectionRef =
                                                dispatchCollectionRef.document(model.dispatchId)

                                            val updates = hashMapOf<String, Any>(
                                                "statusChangeTime" to System.currentTimeMillis()
                                                    .toString(),
                                                "lastUpdater" to loggedUser.userId,
                                                "negotiationRound" to resources.getString(R.string.negotiation_two),
                                                "negotiationPrice2" to
                                                        negotiationPrice2

                                            )

                                            dispatchCollectionRef.update(updates)
                                                .addOnSuccessListener {
                                                    hideProgress()
                                                    dialog.dismiss()
                                                    requireContext().toast(resources.getString(R.string.update_success))
                                                    getRealtimePendingDispatch()

                                                }

                                        }
                                    }
                                }
                            }
                        } else {
                            btnSubmitNegotiation.apply {
                                text = resources.getString(R.string.tv_accept_price)
                                setOnClickListener {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        val dispatchCollectionRef =
                                            dispatchCollectionRef.document(model.dispatchId)

                                        val updates = hashMapOf<String, Any>(
                                            "statusChangeTime" to System.currentTimeMillis()
                                                .toString(),
                                            "lastUpdater" to loggedUser.userId,
                                            "status" to STATUS_AWAITING_DRIVER,
                                            "amount" to model.negotiationPrice1,
                                        )

                                        dispatchCollectionRef.update(updates)
                                            .addOnSuccessListener {
                                                hideProgress()
                                                requireContext().toast(resources.getString(R.string.update_success))
                                                dialog.dismiss()

                                            }
                                    }
                                }
                            }
                        }
                    }
                }
                //last updater here is driver and is set user and this will also most likely never be the case
                //tv n1 amount .text = model.negotiationPrice1
                // etn1 will be disabled if model.negotiationPrice1 is not empty
                //user is to agree or say price
                //price is user price


            }

            n2 -> {
                //means driver has given counter price
                etClientCounterPrice1.visible(false)
                negotiation1Price.visible(true)
                negotiation1Price.text = resources.getString(
                    R.string.money_text, model.negotiationPrice1
                )
                etClientCounterPrice2.visible(false)
                negotiation2Price.visible(true)
                negotiation2Price.text = resources.getString(
                    R.string.money_text, model.negotiationPrice2
                )
                if (loggedUser.role == resources.getString(R.string.driver)) {
                    cbClientNegotiate.enable(false)
                    btnSubmitNegotiation.apply {
                        text = resources.getString(R.string.okay)
                        setOnClickListener {
                            dialog.dismiss()
                        }
                    }

                } else {
                    cbClientNegotiate.setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked) {
                            etClientCounterPrice3.enable(true)
                            btnSubmitNegotiation.enable(false)
                            etClientCounterPrice3.addTextChangedListener {
                                negotiationPrice3 = it.toString().trim()
                                btnSubmitNegotiation.apply {
                                    enable(negotiationPrice3.isNotEmpty())
                                    setOnClickListener {
                                        CoroutineScope(Dispatchers.IO).launch {
                                            val dispatchCollectionRef =
                                                dispatchCollectionRef.document(model.dispatchId)

                                            val updates = hashMapOf<String, Any>(
                                                "statusChangeTime" to System.currentTimeMillis()
                                                    .toString(),
                                                "lastUpdater" to loggedUser.userId,
                                                "negotiationRound" to resources.getString(R.string.negotiation_three),
                                                "negotiationPrice3" to
                                                        negotiationPrice3

                                            )

                                            dispatchCollectionRef.update(updates)
                                                .addOnSuccessListener {
                                                    hideProgress()
                                                    dialog.dismiss()
                                                    requireContext().toast(resources.getString(R.string.update_success))
                                                    launchAddPickerDetailsDialog(model)

                                                }

                                        }
                                    }
                                }
                            }
                        } else {
                            btnSubmitNegotiation.apply {
                                text = resources.getString(R.string.tv_accept_price)
                                setOnClickListener {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        val dispatchCollectionRef =
                                            dispatchCollectionRef.document(model.dispatchId)

                                        val updates = hashMapOf<String, Any>(
                                            "statusChangeTime" to System.currentTimeMillis()
                                                .toString(),
                                            "lastUpdater" to loggedUser.userId,
                                            "status" to STATUS_AWAITING_DRIVER,
                                            "amount" to model.negotiationPrice2,
                                        )

                                        dispatchCollectionRef.update(updates)
                                            .addOnSuccessListener {
                                                hideProgress()

                                                requireContext().toast(resources.getString(R.string.update_success))
                                                dialog.dismiss()

                                            }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            n3 -> {
                //last updater is client
                //driver either accept of cancels
                etClientCounterPrice1.visible(false)
                negotiation1Price.visible(true)
                negotiation1Price.text = resources.getString(
                    R.string.money_text, model.negotiationPrice1
                )
                etClientCounterPrice2.visible(false)
                negotiation2Price.visible(true)
                negotiation2Price.text = resources.getString(
                    R.string.money_text, model.negotiationPrice2
                )
                etClientCounterPrice3.visible(false)
                negotiation3Price.visible(true)
                negotiation3Price.text = resources.getString(
                    R.string.money_text, model.negotiationPrice3
                )
                if (loggedUser.role == resources.getString(R.string.driver)) {
                    tvNegotiationInfo.apply {
                        setTextColor(resources.getColor(R.color.reject))
                        text = resources.getString(R.string.negotiation_last_round_warning)
                        typeface.isBold
                    }
                    cbClientNegotiate.enable(false)
                    btnSubmitNegotiation.apply {
                        text = resources.getString(R.string.tv_accept_price)
                        setOnClickListener {
                            //
                            CoroutineScope(Dispatchers.IO).launch {
                                val dispatchCollectionRef =
                                    dispatchCollectionRef.document(model.dispatchId)

                                val updates = hashMapOf<String, Any>(
                                    "statusChangeTime" to System.currentTimeMillis()
                                        .toString(),
                                    "lastUpdater" to loggedUser.userId,
                                    "status" to STATUS_AWAITING_DRIVER,
                                    "amount" to model.negotiationPrice3,
                                )

                                dispatchCollectionRef.update(updates)
                                    .addOnSuccessListener {
                                        hideProgress()
                                        requireContext().toast(resources.getString(R.string.update_success))
                                        dialog.dismiss()
                                    }
                            }
                            dialog.dismiss()
                        }
                    }
                    btnCancelNegotiation.apply {
                        text = resources.getString(R.string.cancel_negotiation)
                        setOnClickListener {
                            val emptyList = listOf<String>()
                            //remove driver from dispatch
                            //change dispatch status to pending driver
                            //remove all negotiation prices and rounds
                            //remove dispatch from driver list
                            val cancelledDispatch = Dispatch(
                                status = STATUS_PENDING_DRIVER,
                                packageType = model.packageType,
                                weight = model.weight,
                                amount = "",
                                client = model.client,
                                driver = "",
                                weigher = model.weigher,
                                statusChangeTime = System.currentTimeMillis().toString(),
                                weighingDate = model.weighingDate,
                                lastUpdater = model.client,
                                pickupAddress = model.pickupAddress,
                                pickupProvince = model.pickupProvince,
                                pickupCountry = model.pickupCountry,
                                pickupCountryCode = model.pickupCountryCode,
                                pickupDate = model.pickupDate,
                                dropOffAddress = model.dropOffAddress,
                                dropOffProvince = model.dropOffProvince,
                                dropOffCountry = model.dropOffCountry,
                                dropOffCountryCode = model.dropOffCountryCode,
                                transitLocation = model.transitLocation,
                                pickerName = model.pickerName,
                                pickerNumber = model.pickerNumber,
                                interestedDrivers = mapOf(),
                                negotiationRound = "",
                                negotiationPrice0 = "",
                                negotiationPrice1 = "",
                                negotiationPrice2 = "",
                                negotiationPrice3 = "",
                                dateCreated = model.dateCreated,
                                dateWeighed = model.dateWeighed,
                                datePickedUp = model.datePickedUp,
                                dateDelivered = model.dateDelivered,
                                dispatchId = model.dispatchId,
                            )
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    dispatchCollectionRef.document(model.dispatchId)
                                        .set(cancelledDispatch)
                                        .await()
                                    hideProgress()
                                    dialog.dismiss()
                                    getRealtimePendingDispatch()


                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        activity?.toast(e.message.toString())
                                        Log.d(TAG, "createNewDispatch: ${e.message.toString()}")
                                    }
                                }
                            }
                        }
                    }

                } else {
                    tvNegotiationInfo.apply {
                        setTextColor(resources.getColor(R.color.primary))
                        text = resources.getString(R.string.negotiation_last_round_client_warning)
                        typeface.isBold
                    }
                    cbClientNegotiate.enable(false)
                    btnCancelNegotiation.visible(false)
                    btnSubmitNegotiation.apply {
                        text = resources.getString(R.string.okay)
                        setOnClickListener {
                            dialog.dismiss()
                        }
                    }
                }
            }
        }

        dialog.show()

    }

    private fun makeCall(number: String) {
        val dialIntent = Intent(Intent.ACTION_DIAL)
        //dialIntent.data = Uri.fromParts("tel",phoneNumber,null)
        dialIntent.data = Uri.fromParts("tel", number, null)
        startActivity(dialIntent)

    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun locateCustomer(location: String) {
        // Encode the address for the URI
        val encodedAddress = Uri.encode(location)
        // Create a URI with the address query
        val uri = Uri.parse("geo:0,0?q=$encodedAddress")
        // Create an intent with the ACTION_VIEW action and the URI
        val mapIntent = Intent(Intent.ACTION_VIEW, uri)
        // Set the package name to ensure the intent opens in Google Maps app
        mapIntent.setPackage("com.google.android.apps.maps")
        // Check if there's a compatible activity to handle the intent
        if (mapIntent.resolveActivity(requireContext().packageManager) != null) {
            // Start the intent
            startActivity(mapIntent)

        }
    }

    private fun launchAssignDriverDialog(dispatch: Dispatch) {

        val builder =
            layoutInflater.inflate(R.layout.dialog_choose_external_weigher_layout, null)

        val tvDialogTitle =
            builder.findViewById<TextView>(R.id.tv_choose_external_weigher_title)
        val tilWeighingDate =
            builder.findViewById<TextInputLayout>(R.id.text_input_layout_choose_external_weigher_date)
        val rvInterestedDrivers = builder.findViewById<RecyclerView>(R.id.rv_external_weighers)
        val btnCancel =
            builder.findViewById<Button>(R.id.choose_external_weigher_cancel_btn)
        val btnSubmit =
            builder.findViewById<Button>(R.id.choose_external_weigher_submit_btn)


        val layoutManager = LinearLayoutManager(requireContext())
        rvInterestedDrivers.layoutManager = layoutManager
        rvInterestedDrivers.addItemDecoration(
            DividerItemDecoration(
                requireContext(),
                layoutManager.orientation
            )
        )

        val dialog = AlertDialog.Builder(requireContext())
            .setView(builder)
            .setCancelable(true)
            .create()


        dialog.show()

        tilWeighingDate.visible(false)
        tvDialogTitle.text = resources.getString(R.string.interested_drivers_title)

        var selectedDriver: String = ""
        btnSubmit.enable(false)


        val interestedDriverDetails: MutableList<InterestedDriverDetail> = mutableListOf()
        val interestedDrivers = dispatch.interestedDrivers

        for (driver in interestedDrivers) {
            for (driverId in interestedDrivers.keys) {
                val interestedDriver = getDispatchDriver(driverId)
                Log.d(TAG, "launchAssignDriverDialog: $interestedDriver")
                val interestedDriverDetail = InterestedDriverDetail(
                    driverId = interestedDriver!!.userId,
                    driverName = interestedDriver.fullName,
                    driverCharge = driver.value
                )
                interestedDriverDetails.add(interestedDriverDetail)
            }

        }

        interestedDriversAdapter = InterestedDriversAdapter(interestedDriverDetails)

        rvInterestedDrivers.adapter = interestedDriversAdapter
        interestedDriversAdapter.createOnDriverClickListener { driver ->
            selectedDriver = driver.driverId
            btnSubmit.apply {
                enable(selectedDriver.isNotEmpty())
                setOnClickListener {

                    //after updating the chosen driver, dialog to accept price or negotiate pops up
                    CoroutineScope(Dispatchers.IO).launch {
                        val dispatchCollectionRef =
                            dispatchCollectionRef.document(dispatch.dispatchId)

                        val updates = hashMapOf<String, Any>(
                            "driver" to selectedDriver,
                            "status" to STATUS_NEGOTIATING_PRICE,
                            "statusChangeTime" to System.currentTimeMillis().toString(),
                            "negotiationRound" to resources.getString(R.string.negotiation_zero),
                            "negotiationPrice0" to driver.driverCharge

                        )

                        dispatchCollectionRef.update(updates).addOnSuccessListener {
                            userCollectionRef.document(selectedDriver)
                                .update("dispatch", dispatch.dispatchId).addOnSuccessListener {

                                hideProgress()
                                dialog.dismiss()
                                launchFirstNegotiationDialog(dispatch, driver)
                            }

                        }

                    }
                    //change the status and update the existing dispatch

//                val status = STATUS_AWAITING_WEIGHER
//                dispatch.weighingDate = weighingDate
//                dispatch.weigher = selectedWeigher
//                dispatch.status = status


                    Log.d(TAG, "launchAssignStaffDialog: $dispatch")
                    //createNewDispatch(dispatch)
                    dialog.dismiss()
                    //updateDispatch(dispatchId, selectedWeigher, weighingDate, status, dialog)
                }
            }
        }



        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun launchFirstNegotiationDialog(
        dispatch: Dispatch,
        driver: InterestedDriverDetail
    ) {

        val builder =
            layoutInflater.inflate(
                R.layout.custom_client_counter_driver_init_offer,
                null
            )


        val tvDispatchAcceptPriceQuestion =
            builder.findViewById<TextView>(R.id.client_accept_price_dialog_question)
        val cbClientNegotiate =
            builder.findViewById<CheckBox>(R.id.client_accept_price_dialog_negotiate_checkbox)
        val tilClientCounterOffer =
            builder.findViewById<TextInputLayout>(R.id.text_input_layout_client_counter_offer)
        val etClientCounterOffer =
            builder.findViewById<TextInputEditText>(R.id.et_set_counter_offer)

        val tvAcceptPrice =
            builder.findViewById<TextView>(R.id.client_accept_price_dialog_accept_text)
        val tvSubmitCounterOffer =
            builder.findViewById<TextView>(R.id.client_accept_price_dialog_submit_text)

        tvDispatchAcceptPriceQuestion.text =
            resources.getString(R.string.accept_price_question, driver.driverCharge)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(builder)
            .setCancelable(false)
            .create()

        tvSubmitCounterOffer.visible(false)
        tilClientCounterOffer.enable(false)

        var counterCharge = ""

        cbClientNegotiate.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                tilClientCounterOffer.enable(true)
                tvSubmitCounterOffer.visible(false)
                etClientCounterOffer.addTextChangedListener {
                    counterCharge = it.toString().trim()
                    tvSubmitCounterOffer.apply {
                        visible(driver.driverCharge.isNotEmpty())
                        setOnClickListener {
                            //update the negotiation price and round and last updater


                            CoroutineScope(Dispatchers.IO).launch {
                                val dispatchCollectionRef =
                                    dispatchCollectionRef.document(dispatch.dispatchId)

                                val updates = hashMapOf<String, Any>(
                                    "statusChangeTime" to System.currentTimeMillis().toString(),
                                    "lastUpdater" to auth.uid!!,
                                    "negotiationRound" to resources.getString(R.string.negotiation_one),
                                    "negotiationPrice1" to
                                            counterCharge

                                )

                                dispatchCollectionRef.update(updates)
                                    .addOnSuccessListener {
                                        hideProgress()
                                        dialog.dismiss()
                                        getRealtimePendingDispatch()

                                    }

                            }

                        }
                    }
                }
            } else {
                tilClientCounterOffer.enable(false)
                tvAcceptPrice.visible(true)
                tvSubmitCounterOffer.visible(false)
            }
        }

        tvAcceptPrice.setOnClickListener {
            //update the status to awaiting driver
            CoroutineScope(Dispatchers.IO).launch {
                val dispatchCollectionRef =
                    dispatchCollectionRef.document(dispatch.dispatchId)

                val updates = hashMapOf<String, Any>(
                    "statusChangeTime" to System.currentTimeMillis().toString(),
                    "lastUpdater" to auth.uid!!,
                    "status" to STATUS_AWAITING_DRIVER,
                    "amount" to driver.driverCharge,
                )

                dispatchCollectionRef.update(updates)
                    .addOnSuccessListener {
                        hideProgress()
                        //launch dialog to enter picker details
                        val listOfDispatch = listOf(dispatch.dispatchId)

                        lifecycleScope.launch {
                            userCollectionRef.document(driver.driverId)
                                .update("dispatch", dispatch.dispatchId).await()
                        }

                        dialog.dismiss()
                        launchAddPickerDetailsDialog(dispatch)

                    }

            }
        }


        dialog.show()

    }

    private fun launchAddPickerDetailsDialog(dispatch: Dispatch) {

        val builder =
            layoutInflater.inflate(
                R.layout.custom_picker_details_layout,
                null
            )

        val tilPickerName =
            builder.findViewById<TextInputLayout>(R.id.text_input_layout_picker_name)
        val etPickerName =
            builder.findViewById<TextInputEditText>(R.id.et_picker_name)
        val tilPickerPhone =
            builder.findViewById<TextInputLayout>(R.id.text_input_layout_picker_phone)
        val etPickerPhone =
            builder.findViewById<TextInputEditText>(R.id.et_picker_phone_number)
        val pickerPhoneCode =
            builder.findViewById<CountryCodePicker>(R.id.picker_country_code_picker)

        val btnAddPickerDetails =
            builder.findViewById<Button>(R.id.add_picker_details_btn)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(builder)
            .setCancelable(false)
            .create()

        var pickerName = ""
        var pickerPhoneNumber = ""

        btnAddPickerDetails.enable(false)

        var pickerCountryCode = pickerPhoneCode.defaultCountryCodeWithPlus
        pickerPhoneCode.setOnCountryChangeListener {
            pickerCountryCode = pickerPhoneCode.selectedCountryCodeWithPlus
            // Handle selected country code and name
            pickerPhoneCode.setNumberAutoFormattingEnabled(true)
        }

        etPickerPhone.addTextChangedListener { number ->
            pickerName = etPickerName.text.toString()
            btnAddPickerDetails.apply {
                enable(pickerName.isNotEmpty() && number.toString().isNotEmpty())
                setOnClickListener {
                    pickerName = etPickerName.text.toString()
                    pickerPhoneNumber = "$pickerCountryCode${number.toString()}"
                    CoroutineScope(Dispatchers.IO).launch {
                        val dispatchCollectionRef =
                            dispatchCollectionRef.document(dispatch.dispatchId)

                        val updates = hashMapOf<String, Any>(
                            "pickerName" to pickerName,
                            "pickerNumber" to pickerPhoneNumber
                        )

                        dispatchCollectionRef.update(updates)
                            .addOnSuccessListener {
                                hideProgress()
                                //launch dialog to enter picker details
                                dialog.dismiss()
                                getRealtimePendingDispatch()

                            }
                    }
                }
            }
        }
        dialog.show()
    }

    private fun getDispatchDriver(driver: String): UserData? {

//        requireContext().showProgress()
        val deferred = CoroutineScope(Dispatchers.IO).async {
            try {
                val snapshot = Common.userCollectionRef.document(driver).get().await()
                if (snapshot.exists()) {
                    return@async snapshot.toObject(UserData::class.java)
                } else {
                    return@async null
                }
            } catch (e: Exception) {
                requireContext().toast(e.message.toString())
                return@async null
            }
        }

        val driverUser = runBlocking { deferred.await() }
        //hideProgress()

        return driverUser
    }

    private fun getWalletInfo(weigher: String): WalletData? {

//        requireContext().showProgress()
        val deferred = CoroutineScope(Dispatchers.IO).async {
            try {
                val snapshot = Common.walletCollectionRef.document(weigher).get().await()
                if (snapshot.exists()) {
                    return@async snapshot.toObject(WalletData::class.java)
                } else {
                    return@async null
                }
            } catch (e: Exception) {
                requireContext().toast(e.message.toString())
                return@async null
            }
        }

        val wallet = runBlocking { deferred.await() }
        //hideProgress()

        return wallet
    }

    private fun checkLocationPermission(dispatch: Dispatch) {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is granted, you can request location updates.
            getLocation()
        } else {
            // Request location permission
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                locationPermissionCode
            )
        }
    }

    // Override onRequestPermissionsResult to handle the permission request result.
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == locationPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, you can request location updates.
                getLocation()
            } else {
                // Permission denied, handle this case as needed.
                // For example, show a message to the user or disable location functionality.
                requireContext().toast("Location permission denied. Cannot fetch location")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                // Use the location object to get latitude and longitude.
                val latitude = location.latitude
                val longitude = location.longitude

                driverLocation = "$latitude,$longitude"
                CoroutineScope(Dispatchers.IO).launch {
                    val dispatchCollectionRef =
                        dispatchCollectionRef.document(dispatchForLocation.dispatchId)

                    val updates = hashMapOf<String, Any>(
                        "userLocationRequest" to false,
                        "transitLocation" to driverLocation
                    )

                    dispatchCollectionRef.update(updates).addOnSuccessListener {
                        hideProgress()
                        requireContext().toast(resources.getString(R.string.location_updated))
                    }

                }


                //navigateToLocation(latitude, longitude)

                // Now you have the current location. You can use it as needed.
                // For example, show it on a map or send it to your server.
            } else {
                // Location is null, handle this case as needed.
                // For example, show an error message or request location updates.
            }
        }.addOnFailureListener { exception: Exception ->
            // Handle the failure to get the location.
            // For example, show an error message or request location updates.
            requireContext().toast(exception.message.toString())
        }
    }


    private fun navigateToLocation(location: String) {
        // Create a Uri with the latitude and longitude
        val locationUri = Uri.parse("geo:$location")

        // Create an Intent with the ACTION_VIEW action and the location Uri
        val mapIntent = Intent(Intent.ACTION_VIEW, locationUri)

        // Set the package to Google Maps (optional, in case multiple map apps are installed)
        mapIntent.setPackage("com.google.android.apps.maps")

        // Check if there's an app that can handle the Intent (Google Maps is installed)
        if (mapIntent.resolveActivity(requireActivity().packageManager) != null) {
            // Start the Google Maps activity
            startActivity(mapIntent)
        } else {
            // Handle the case where Google Maps is not installed on the device
            // For example, show an error message or use a different map provider.
            requireContext().toast("Install Google Maps on you device")
        }
    }


    private fun updateDispatchInfo(dispatch: Dispatch) {

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

    private fun getUser(userId: String): UserData? {
        //requireContext().showProgress()
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
        //hideProgress()

        return driverUser
    }


    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView: ")
        _binding = null
    }

}