package com.androidstrike.schoolprojects.thefreighterapp.features.home.client.dispatch.concluded

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.androidstrike.schoolprojects.thefreighterapp.R
import com.androidstrike.schoolprojects.thefreighterapp.databinding.FragmentConcludedDispatchBinding
import com.androidstrike.schoolprojects.thefreighterapp.models.Dispatch
import com.androidstrike.schoolprojects.thefreighterapp.models.UserData
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.DATE_FORMAT
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.auth
import com.androidstrike.schoolprojects.thefreighterapp.utils.enable
import com.androidstrike.schoolprojects.thefreighterapp.utils.getDate
import com.androidstrike.schoolprojects.thefreighterapp.utils.hideProgress
import com.androidstrike.schoolprojects.thefreighterapp.utils.showProgress
import com.androidstrike.schoolprojects.thefreighterapp.utils.snackbar
import com.androidstrike.schoolprojects.thefreighterapp.utils.toast
import com.androidstrike.schoolprojects.thefreighterapp.utils.visible
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.Query
import com.iarcuschin.simpleratingbar.SimpleRatingBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import java.text.DecimalFormat

class ConcludedDispatch : Fragment() {

    private var _binding: FragmentConcludedDispatchBinding? = null
    private val binding get() = _binding!!

    private var concludedDispatchAdapter: FirestoreRecyclerAdapter<Dispatch, ConcludedDispatchAdapter>? =
        null
    private lateinit var concludedDispatches: Query
    private lateinit var clientRole: String
    private lateinit var driverRole: String
    private lateinit var weigherRole: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentConcludedDispatchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        clientRole = resources.getString(R.string.client)
        driverRole = resources.getString(R.string.driver)
        weigherRole = resources.getString(R.string.weigher)

        getRealtimeConcludedDispatch()
        with(binding) {
            val layoutManager = LinearLayoutManager(requireContext())
            rvConcludedDispatchList.layoutManager = layoutManager
            rvConcludedDispatchList.addItemDecoration(
                DividerItemDecoration(
                    requireContext(),
                    layoutManager.orientation
                )
            )
        }
    }

    private fun getUsern() {
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

                        getRealtimeConcludedDispatch()
                    }
                }
        }
    }

    private fun getRealtimeConcludedDispatch() {

        val loggedUser = getUser(auth.uid!!)!!
        when (loggedUser.role) {
            clientRole -> {
                concludedDispatches =
                    Common.dispatchCollectionRef.whereEqualTo("client", loggedUser.userId)
                        .whereEqualTo("status", Common.STATUS_DELIVERED)
            }

            weigherRole -> {
                concludedDispatches =
                    Common.dispatchCollectionRef.whereEqualTo("weigher", loggedUser.userId)
                        .whereNotEqualTo("status", Common.STATUS_AWAITING_WEIGHER)
            }

            driverRole -> {
                concludedDispatches =
                    Common.dispatchCollectionRef.whereEqualTo("driver", loggedUser.userId)
                        .whereEqualTo("status", Common.STATUS_DELIVERED)
            }
        }

        val options = FirestoreRecyclerOptions.Builder<Dispatch>()
            .setQuery(concludedDispatches, Dispatch::class.java).build()
        try {
            concludedDispatchAdapter = object :
                FirestoreRecyclerAdapter<Dispatch, ConcludedDispatchAdapter>(options) {
                override fun onCreateViewHolder(
                    parent: ViewGroup,
                    viewType: Int
                ): ConcludedDispatchAdapter {
                    val itemView = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_concluded_dispatch_layout, parent, false)
                    return ConcludedDispatchAdapter(itemView)
                }

                override fun onBindViewHolder(
                    holder: ConcludedDispatchAdapter,
                    position: Int,
                    model: Dispatch
                ) {
                    //val timeToDeliver = model.supposedTime
                    holder.concludedDeliveredDispatchStatus.text = model.status
                    holder.concludedDispatchDateDelivered.text = if (loggedUser.role == weigherRole)
                        getDate(model.dateWeighed.toLong(), DATE_FORMAT)
                    else
                        getDate(model.dateDelivered.toLong(), Common.DATE_FORMAT)
                    holder.concludedDeliveredDispatchDestination.text = resources.getString(
                        R.string.tv_dispatch_destination,
                        model.dropOffProvince,
                        model.dropOffCountry
                    )

                    holder.concludedDeliveredDispatchDriverName.text =
                        if (loggedUser.role == clientRole) {
                            getDispatchDriver(model.driver)!!.fullName
                        } else {
                            getDispatchDriver(model.client)!!.fullName
                        }
                    val decimalFormat = DecimalFormat("#.#")
                    val formattedNumber = if (model.rating.isNotEmpty())
                        decimalFormat.format(model.rating.toFloat())
                    else
                        decimalFormat.format(0.0)

                    holder.concludedDeliveredDispatchRating.text =
                        formattedNumber.ifEmpty { resources.getString(R.string.not_rated) }


                    holder.itemView.setOnClickListener {
                        //launch a new screen

                        handleDispatchesStatuses(model)
                    }
                }
            }

        } catch (e: Exception) {
            requireActivity().toast(e.message.toString())
        }
        concludedDispatchAdapter?.startListening()
        binding.rvConcludedDispatchList.adapter = concludedDispatchAdapter


    }

    private fun handleDispatchesStatuses(dispatch: Dispatch) {

        val builder =
            layoutInflater.inflate(R.layout.concluded_dispatch_details_dialog, null)

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
        val dispatchWeigherName =
            builder.findViewById<TextView>(R.id.dispatch_detail_weigher_name)
        val dispatchStatus =
            builder.findViewById<TextView>(R.id.dispatch_detail_status)
        val dispatchAmount =
            builder.findViewById<TextView>(R.id.dispatch_detail_amount)
        val dispatchCallDriver =
            builder.findViewById<TextView>(R.id.dispatch_detail_call_driver)
        val dispatchTvPickupAddress =
            builder.findViewById<TextView>(R.id.tv_pickup_address)
        val dispatchTvDropOffAddress =
            builder.findViewById<TextView>(R.id.tv_drop_off_address)

        val dispatchCallWeigher =
            builder.findViewById<TextView>(R.id.dispatch_detail_call_weigher)

        val dispatchSubmitRating =
            builder.findViewById<TextView>(R.id.submit_rating)
        val dispatchDetailOkayBtn =
            builder.findViewById<Button>(R.id.dispatch_details_okay_button)
        val ratingBar = builder.findViewById<SimpleRatingBar>(R.id.client_dispatch_rating_bar)


        val dialog = AlertDialog.Builder(requireContext())
            .setView(builder)
            .setCancelable(false)
            .create()

        val loggedUser = getUser(auth.uid!!)!!

        var customerRating = 0.0F



        dispatchDateCreated.text = resources.getString(
            R.string.dispatch_detail_date_created,
            getDate(dispatch.dateCreated.toLong(), Common.DATE_FORMAT_SHORT)
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
        dispatchStatus.text = dispatch.status

        if (loggedUser.role == weigherRole) {
            dispatchDatePickedUp.visible(false)
            dispatchDateDelivered.visible(false)
            dispatchPickerName.visible(false)
            dispatchPickerName.visible(false)
            dispatchTvDropOffAddress.visible(false)
            dispatchTvPickupAddress.visible(false)
            dispatchDropOffAddress.visible(false)
            dispatchWeigherName.visible(false)
            dispatchPickupAddress.visible(false)
            dispatchCallWeigher.visible(false)
            ratingBar.visible(false)
            dispatchDatePickedUp.text = resources.getString(R.string.date_weighed)
            dispatchAmount.text = getDispatchDriver(dispatch.weigher)!!.weigherCost
        }

        dispatchDatePickedUp.text = if (dispatch.datePickedUp.isNotEmpty()) {
            resources.getString(
                R.string.dispatch_date_picked_up,
                getDate(dispatch.datePickedUp.toLong(), Common.DATE_FORMAT_SHORT)
            )
        } else {
            resources.getString(R.string.dispatch_date_picked_up, Common.NOT_AVAILABLE)
        }
        dispatchDateDelivered.text = if (dispatch.dateDelivered.isNotEmpty()) {
            resources.getString(
                R.string.dispatch_date_delivered,
                getDate(dispatch.dateDelivered.toLong(), Common.DATE_FORMAT_SHORT)
            )
        } else {
            resources.getString(R.string.dispatch_date_delivered, Common.NOT_AVAILABLE)
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

        dispatchAmount.text = dispatch.amount
        dispatchCallDriver.setOnClickListener {
            if (loggedUser.role == clientRole) {
                makeCall(getDispatchDriver(dispatch.driver)!!.phoneNumber)
            } else {
                makeCall(getDispatchDriver(dispatch.client)!!.phoneNumber)
            }
        }
        if (loggedUser.role == clientRole) {
            dispatchCallWeigher.apply {
                visible(dispatch.weigher.isNotEmpty())
                setOnClickListener {
                    makeCall(getDispatchDriver(dispatch.weigher)!!.phoneNumber)
                }
            }
            dispatchWeigherName.apply {
                if (dispatch.weigher.isEmpty()) {
                    visible(false)
                } else {
                    text = getDispatchDriver(dispatch.weigher)!!.fullName
                }
            }
        } else {
            dispatchWeigherName.visible(false)
            dispatchCallWeigher.visible(false)
        }
        ratingBar.apply {

            val decimalFormat = DecimalFormat("#.#")

            enable(loggedUser.role == clientRole && dispatch.rating.isEmpty())
            if (dispatch.rating.isEmpty()) {
                rating = 0.0F
            } else {
                rating = dispatch.rating.toFloat()
            }
            setOnRatingBarChangeListener { ratingBar, rating, fromUser ->
                // Handle the rating change event
                if (rating.isNaN()) {
                    requireView().snackbar("Give at least one star")
                } else {
                    customerRating = rating
                }
                // `rating` represents the selected rating value
            }
        }


        dispatchSubmitRating.setOnClickListener {
            requireContext().showProgress()
            CoroutineScope(Dispatchers.IO).launch {
                val dispatchCollectionRef =
                    Common.dispatchCollectionRef.document(dispatch.dispatchId)

                val updates = hashMapOf<String, Any>(
                    "rating" to customerRating.toString(),
                )

                dispatchCollectionRef.update(updates)
                    .addOnSuccessListener {
                        hideProgress()
                        //launch dialog to enter picker details
                        dialog.dismiss()
                        getRealtimeConcludedDispatch()

                    }
            }
        }

        dispatchDetailOkayBtn.setOnClickListener {
            dialog.dismiss()
        }


        dialog.show()


    }

    private fun makeCall(number: String) {
        val dialIntent = Intent(Intent.ACTION_DIAL)
        //dialIntent.data = Uri.fromParts("tel",phoneNumber,null)
        dialIntent.data = Uri.fromParts("tel", number, null)
        startActivity(dialIntent)

    }

    private fun getDispatchDriver(driver: String): UserData? {

        requireContext().showProgress()
        val deferred = CoroutineScope(Dispatchers.IO).async {
            try {
                val snapshot = Common.userCollectionRef.document(driver).get().await()
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


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}