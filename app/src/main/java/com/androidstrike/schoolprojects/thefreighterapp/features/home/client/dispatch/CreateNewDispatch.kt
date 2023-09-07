package com.androidstrike.schoolprojects.thefreighterapp.features.home.client.dispatch

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.androidstrike.schoolprojects.thefreighterapp.R
import com.androidstrike.schoolprojects.thefreighterapp.databinding.FragmentCreateNewDispatchBinding
import com.androidstrike.schoolprojects.thefreighterapp.models.Dispatch
import com.androidstrike.schoolprojects.thefreighterapp.models.UserData
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.STATUS_AWAITING_WEIGHER
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.STATUS_DRAFT
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.STATUS_PENDING_DRIVER
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.dispatchCollectionRef
import com.androidstrike.schoolprojects.thefreighterapp.utils.hideProgress
import com.androidstrike.schoolprojects.thefreighterapp.utils.showProgress
import com.androidstrike.schoolprojects.thefreighterapp.utils.toast
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.QuerySnapshot
import com.hbb20.CountryCodePicker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class CreateNewDispatch : Fragment() {

    private var _binding: FragmentCreateNewDispatchBinding? = null
    private val binding get() = _binding!!

    private val args: CreateNewDispatchArgs by navArgs()
    private lateinit var loggedInUser: UserData
    private lateinit var dispatchId: String

    private var externalWeighersList: MutableList<UserData> = mutableListOf()


    private lateinit var status: String
    private lateinit var packageType: String
    private var packageTypeOkay = false
    private lateinit var weight: String
    private var weightOkay = false

    //amount
    private lateinit var client: String

    //driver
    //weigher
    private lateinit var lastUpdater: String
    private lateinit var pickupAddress: String
    private var pickupAddressOkay = false
    private lateinit var pickupProvince: String
    private var pickupProvinceOkay = false
    private lateinit var pickupCountry: String
    private lateinit var pickupCountryCode: String
    private lateinit var pickupDate: String
    private var pickupDateOkay = false
    private lateinit var dropOffAddress: String
    private var dropOffAddressOkay = false
    private lateinit var dropOffProvince: String
    private var dropOffProvinceOkay = false
    private lateinit var dropOffCountry: String
    private lateinit var dropOffCountryCode: String

    private val calendar = Calendar.getInstance()


    private var externalWeighersAdapter: FirestoreRecyclerAdapter<UserData, ChooseExternalWeighersAdapter>? =
        null

    private val TAG = "CreateNewDispatch"


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateNewDispatchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loggedInUser = args.loggedUser

        //get the arguments (parameters) passed passed to this class to know if the user is here to create a new dispatch or edit a draft
        if (args.dispatchId.isNotEmpty()) { //is here to edit draft
            dispatchId = args.dispatchId
            fetchDispatchDetails(dispatchId)
        } else { //is here to create a new dispatch
            dispatchId = UUID.randomUUID().toString()
            val dispatch = Dispatch(
                dispatchId = dispatchId
            )
            loadView(dispatch, resources.getString(R.string.add_dispatch))

        }


    }

    private fun loadView(dispatch: Dispatch, request: String) {

        with(binding) {

            //populate the dropdown with the various types of packages
            val packageTypes = resources.getStringArray(R.array.package_types)
            val packageTypesAdapter =
                ArrayAdapter(requireContext(), R.layout.drop_down_item, packageTypes)
            newDispatchChoosePackageType.setAdapter(packageTypesAdapter)

            if (request == resources.getString(R.string.complete_draft)){
                //populate the view with the draft dispatch
                newDispatchChoosePackageType.setText(dispatch.packageType)
                dispatchPackageWeight.setText(dispatch.weight)
                dispatchPickupAddress.setText(dispatch.pickupAddress)
                dispatchPickupProvince.setText(dispatch.pickupProvince)
                dispatchDropOffAddress.setText(dispatch.dropOffAddress)
                dispatchDropOffProvince.setText(dispatch.dropOffProvince)
                dispatchDropOffCountry.setCountryForNameCode(dispatch.dropOffCountryCode)
                dispatchPickupDate.setText(dispatch.pickupDate)

            }


            dispatchPickupDate.setOnFocusChangeListener { v, hasFocus ->
                val packagePickUpDateLayout = v as TextInputEditText
                pickupDate = packagePickUpDateLayout.text.toString().trim()
                if (hasFocus) {
                    showDatePicker(v)
                } else {
                    if (pickupDate.isEmpty()
                    ) {
                        textInputLayoutDispatchPickupDate.error =
                            resources.getString(R.string.invalid_date)
                    } else {
                        textInputLayoutDispatchPickupDate.error = null

                        pickupDateOkay = true
                    }
                }
            }

            dispatchPickupAddress.setOnFocusChangeListener { v, hasFocus ->
                val packagePickUpAddressLayout = v as TextInputEditText
                pickupAddress = packagePickUpAddressLayout.text.toString().trim()
                if (!hasFocus) {
                    if (pickupAddress.isEmpty()
                    ) {
                        textInputLayoutDispatchPickupAddress.error =
                            resources.getString(R.string.invalid_address_line)
                    } else {
                        textInputLayoutDispatchPickupAddress.error = null

                        pickupAddressOkay = true
                    }
                }
            }

            dispatchPickupProvince.setOnFocusChangeListener { v, hasFocus ->
                val packagePickUpProvinceLayout = v as TextInputEditText
                pickupProvince = packagePickUpProvinceLayout.text.toString().trim()
                if (!hasFocus) {
                    if (pickupProvince.isEmpty()
                    ) {
                        textInputLayoutDispatchPickupProvince.error =
                            resources.getString(R.string.invalid_province)
                    } else {
                        textInputLayoutDispatchPickupProvince.error = null

                        pickupProvinceOkay = true
                    }
                }
            }

            dispatchDropOffAddress.setOnFocusChangeListener { v, hasFocus ->
                val packageDropOffAddressLayout = v as TextInputEditText
                dropOffAddress = packageDropOffAddressLayout.text.toString().trim()
                if (!hasFocus) {
                    if (dropOffAddress.isEmpty()
                    ) {
                        textInputLayoutDispatchDropOffAddress.error =
                            resources.getString(R.string.invalid_address_line)
                    } else {
                        textInputLayoutDispatchDropOffAddress.error = null

                        dropOffAddressOkay = true
                    }
                }
            }

            dispatchDropOffProvince.setOnFocusChangeListener { v, hasFocus ->
                val packageDropOffProvinceLayout = v as TextInputEditText
                dropOffProvince = packageDropOffProvinceLayout.text.toString().trim()
                if (!hasFocus) {
                    if (dropOffProvince.isEmpty()
                    ) {
                        textInputLayoutDispatchDropOffProvince.error =
                            resources.getString(R.string.invalid_province)
                    } else {
                        textInputLayoutDispatchDropOffProvince.error = null

                        dropOffProvinceOkay = true
                    }
                }
            }



            pickupCountry = dispatchPickupCountry.defaultCountryName
            pickupCountryCode = dispatchPickupCountry.defaultCountryNameCode
            dropOffCountry = dispatchDropOffCountry.defaultCountryName
            dropOffCountry = dispatchDropOffCountry.defaultCountryName



            dispatchPickupCountry.setOnCountryChangeListener {
                pickupCountry = dispatchPickupCountry.selectedCountryName
                // Handle selected country code and name
            }
            dispatchDropOffCountry.setOnCountryChangeListener {
                dropOffCountry = dispatchDropOffCountry.selectedCountryName
                // Handle selected country code and name
            }

            newDispatchSubmitBtn.setOnClickListener {

                packageType = newDispatchChoosePackageType.text.toString().trim()
                weight = dispatchPackageWeight.text.toString().trim()
                status = if (weight.isNotEmpty()) {
                    STATUS_PENDING_DRIVER
                } else {
                    STATUS_AWAITING_WEIGHER
                }
                client = loggedInUser.userId
                lastUpdater = client
                pickupAddress = dispatchPickupAddress.text.toString().trim()
                pickupProvince = dispatchPickupProvince.text.toString().trim()
                pickupCountry = dispatchPickupCountry.selectedCountryName
                pickupCountryCode = dispatchPickupCountry.selectedCountryNameCode
                dropOffAddress = dispatchDropOffAddress.text.toString().trim()
                dropOffProvince = dispatchDropOffProvince.text.toString().trim()
                dropOffCountry = dispatchDropOffCountry.selectedCountryName
                dropOffCountryCode = dispatchDropOffCountry.selectedCountryNameCode
                pickupDate = dispatchPickupDate.text.toString().trim()


                if (packageType.isNotEmpty() && weight.isNotEmpty() && pickupAddress.isNotEmpty() && pickupProvince.isNotEmpty() && pickupDate.isNotEmpty() && dropOffAddress.isNotEmpty() && dropOffProvince.isNotEmpty()) {

                    val newDispatch = Dispatch(
                        packageType = packageType,
                        weight = weight,
                        status = status,
                        client = client,
                        lastUpdater = lastUpdater,
                        pickupAddress = pickupAddress,
                        pickupProvince = pickupProvince,
                        pickupCountry = pickupCountry,
                        pickupCountryCode = pickupCountryCode,
                        dropOffAddress = dropOffAddress,
                        dropOffProvince = dropOffProvince,
                        dropOffCountry = dropOffCountry,
                        dropOffCountryCode = dropOffCountryCode,
                        pickupDate = pickupDate,
                        dispatchId = dispatchId,
                        dateCreated = System.currentTimeMillis().toString(),
                        statusChangeTime = System.currentTimeMillis().toString()
                    )
                    //create a new dispatch in cloud if all conditions are met
                    createNewDispatch(newDispatch)
                } else {
                    requireContext().toast(resources.getString(R.string.missing_fields))
                }
            }

            textDispatchPackageWeightContractors.setOnClickListener {//user clicks to contract a weigher
                //prompt to fill out other details first
                packageType = newDispatchChoosePackageType.text.toString().trim()
                weight = dispatchPackageWeight.text.toString().trim()
                client = loggedInUser.userId
                lastUpdater = client
                pickupAddress = dispatchPickupAddress.text.toString().trim()
                pickupProvince = dispatchPickupProvince.text.toString().trim()
                pickupCountry = dispatchPickupCountry.selectedCountryName
                pickupCountryCode = dispatchPickupCountry.selectedCountryNameCode
                dropOffAddress = dispatchDropOffAddress.text.toString().trim()
                dropOffProvince = dispatchDropOffProvince.text.toString().trim()
                dropOffCountry = dispatchDropOffCountry.selectedCountryName
                dropOffCountryCode = dispatchDropOffCountry.selectedCountryNameCode
                pickupDate = dispatchPickupDate.text.toString().trim()


                if (packageType.isNotEmpty() && pickupAddress.isNotEmpty() && pickupProvince.isNotEmpty() && pickupDate.isNotEmpty() && dropOffAddress.isNotEmpty() && dropOffProvince.isNotEmpty()) {

                    val newId = dispatchId.ifEmpty { System.currentTimeMillis().toString() }
                    val newDispatch = Dispatch(
                        packageType = packageType,
                        client = client,
                        lastUpdater = lastUpdater,
                        pickupAddress = pickupAddress,
                        pickupProvince = pickupProvince,
                        pickupCountry = pickupCountry,
                        pickupCountryCode = pickupCountryCode,
                        dropOffAddress = dropOffAddress,
                        dropOffProvince = dropOffProvince,
                        dropOffCountry = dropOffCountry,
                        dropOffCountryCode = dropOffCountryCode,
                        pickupDate = pickupDate,
                        dispatchId = dispatchId,
                        dateCreated = System.currentTimeMillis().toString(),
                        statusChangeTime = System.currentTimeMillis().toString()
                    )
                    //show screen to chose weigher
                    launchExternalContractorsDialog(newDispatch)
                } else {
                    requireContext().toast(resources.getString(R.string.fill_details_before_weighing))
                }
            }

            newDispatchCancelBtn.setOnClickListener {
                val navBackToDispatchLanding =
                    CreateNewDispatchDirections.actionCreateNewDispatchToDispatchScreenLanding()
                findNavController().navigate(navBackToDispatchLanding)
            }

            newDispatchSaveBtn.setOnClickListener {
                //get the filled in details
                packageType = newDispatchChoosePackageType.text.toString().trim()
                weight = dispatchPackageWeight.text.toString().trim()
                status = STATUS_DRAFT
                client = loggedInUser.userId
                lastUpdater = client
                pickupAddress = dispatchPickupAddress.text.toString().trim()
                pickupProvince = dispatchPickupProvince.text.toString().trim()
                pickupCountry = dispatchPickupCountry.selectedCountryName
                pickupCountryCode = dispatchPickupCountry.selectedCountryNameCode
                dropOffAddress = dispatchDropOffAddress.text.toString().trim()
                dropOffProvince = dispatchDropOffProvince.text.toString().trim()
                dropOffCountry = dispatchDropOffCountry.selectedCountryName
                dropOffCountryCode = dispatchDropOffCountry.selectedCountryNameCode
                pickupDate = dispatchPickupDate.text.toString().trim()
                val newDispatchDraft = Dispatch(
                    status = status,
                    client = client,
                    lastUpdater = lastUpdater,
                    packageType = packageType,
                    weight = weight,
                    pickupAddress = pickupAddress,
                    pickupProvince = pickupProvince,
                    pickupCountry = pickupCountry,
                    pickupCountryCode = pickupCountryCode,
                    dropOffAddress = dropOffAddress,
                    dropOffProvince = dropOffProvince,
                    dropOffCountry = dropOffCountry,
                    dropOffCountryCode = dropOffCountryCode,
                    pickupDate = pickupDate,
                    dispatchId = dispatchId,
                    dateCreated = System.currentTimeMillis().toString(),
                    statusChangeTime = System.currentTimeMillis().toString()

                )
                createNewDispatch(newDispatchDraft)
            }

        }

    }

    private fun fetchDispatchDetails(dispatchId: String) {

        requireContext().showProgress()
        var draftDispatch = Dispatch()

        CoroutineScope(Dispatchers.IO).launch {
            val dispatchRef = dispatchCollectionRef.document(dispatchId)
                .addSnapshotListener { value, error ->
                    if (error != null) {
                        requireContext().toast(error.message.toString())
                        return@addSnapshotListener
                    }
                    if (value != null && value.exists()) {
                        draftDispatch = value.toObject(Dispatch::class.java)!!
                        //call the function to set up the ui
                        hideProgress()
                        loadView(draftDispatch, resources.getString(R.string.complete_draft))

                    }

                }
        }
    }

    private fun launchExternalContractorsDialog(dispatch: Dispatch) {
        //load list of weighers from the cloud db
        CoroutineScope(Dispatchers.IO).launch {
            Common.userCollectionRef.whereEqualTo("role", resources.getString(R.string.weigher))
                .get()
                .addOnSuccessListener { querySnapshot: QuerySnapshot ->

                    for (document in querySnapshot.documents) {
                        val item = document.toObject(UserData::class.java)
                        if (item != null) {
                            externalWeighersList.add(item)
                        }
                    }
                    launchAssignStaffDialog(dispatch)

                }
        }
    }

    private fun launchAssignStaffDialog(withWeigherDispatch: Dispatch) {

        val builder =
            layoutInflater.inflate(R.layout.dialog_choose_external_weigher_layout, null)

        val tilWeighingDate =
            builder.findViewById<TextInputLayout>(R.id.text_input_layout_choose_external_weigher_date)
        val etWeighingDate =
            builder.findViewById<TextInputEditText>(R.id.choose_external_weigher_date)
        val rvExternalWeighers = builder.findViewById<RecyclerView>(R.id.rv_external_weighers)
        val btnCancelExternalWeighers =
            builder.findViewById<Button>(R.id.choose_external_weigher_cancel_btn)
        val btnSubmitExternalWeighers =
            builder.findViewById<Button>(R.id.choose_external_weigher_submit_btn)


        val layoutManager = LinearLayoutManager(requireContext())
        rvExternalWeighers.layoutManager = layoutManager
        rvExternalWeighers.addItemDecoration(
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

        var selectedWeigher: String = ""
        var weighingDate: String


        val externalWeighers =
            Common.userCollectionRef.whereEqualTo("role", resources.getString(R.string.weigher))
        val options = FirestoreRecyclerOptions.Builder<UserData>()
            .setQuery(externalWeighers, UserData::class.java).build()
        try {
            externalWeighersAdapter = object :
                FirestoreRecyclerAdapter<UserData, ChooseExternalWeighersAdapter>(options) {
                override fun onCreateViewHolder(
                    parent: ViewGroup,
                    viewType: Int
                ): ChooseExternalWeighersAdapter {
                    val itemView = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_external_weighers_layout, parent, false)
                    return ChooseExternalWeighersAdapter(itemView)
                }

                override fun onBindViewHolder(
                    holder: ChooseExternalWeighersAdapter,
                    position: Int,
                    model: UserData
                ) {


                    holder.externalWeigherName.text = model.fullName
                    holder.externalWeigherPrice.text =
                        resources.getString(R.string.weighing_cost, model.weigherCost)

                    holder.itemView.setOnClickListener {
                        it.setBackgroundColor(resources.getColor(R.color.primary_faded))
                        holder.externalWeigherItemImage.setImageResource(R.drawable.ic_selected)
                        selectedWeigher = model.userId
                    }

                }

            }

        } catch (e: Exception) {
            requireActivity().toast(e.message.toString())
        }
        externalWeighersAdapter?.startListening()
        rvExternalWeighers.adapter = externalWeighersAdapter

        btnSubmitExternalWeighers.setOnClickListener {
            //change the status and update the existing dispatch
            weighingDate = etWeighingDate.text.toString()
            if (weighingDate.isEmpty()) {
                tilWeighingDate.error = resources.getString(R.string.invalid_weighing_date)
            } else {
                val status = STATUS_AWAITING_WEIGHER
                withWeigherDispatch.weighingDate = weighingDate
                withWeigherDispatch.weigher = selectedWeigher
                withWeigherDispatch.status = status


                Log.d(TAG, "launchAssignStaffDialog: $withWeigherDispatch")
                createNewDispatch(withWeigherDispatch)
                dialog.dismiss()
                //updateDispatch(dispatchId, selectedWeigher, weighingDate, status, dialog)
            }
        }

        etWeighingDate.setOnFocusChangeListener { v, hasFocus ->
            val packageWeighingDateLayout = v as TextInputEditText
            weighingDate = packageWeighingDateLayout.text.toString().trim()
            if (hasFocus) {
                showDatePicker(v)
            } else {
                if (pickupDate.isEmpty()
                ) {
                    tilWeighingDate.error =
                        resources.getString(R.string.invalid_weighing_date)
                } else {
                    tilWeighingDate.error = null
                }
            }
        }


        btnCancelExternalWeighers.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun updateDispatch(
        dispatchId: String,
        selectedWeigher: String,
        weighingDate: String,
        status: String,
        dialog: AlertDialog
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val dispatchRef = dispatchCollectionRef.document(dispatchId)

            val updates = hashMapOf<String, Any>(
                "status" to status,
                "weigher" to selectedWeigher,
                "weighingDate" to weighingDate,
            )

            dispatchRef.update(updates)
                .addOnSuccessListener {
                    // Update successful
                    requireContext().toast(status)
                    dialog.dismiss()
                    val navBackToPendingDispatch =
                        CreateNewDispatchDirections.actionCreateNewDispatchToPendingDispatch()
                    findNavController().navigate(navBackToPendingDispatch)
                }
                .addOnFailureListener { e ->
                    // Handle error
                    requireContext().toast(e.message.toString())
                }


        }

    }


    private fun createNewDispatch(newDispatch: Dispatch) {
        requireContext().showProgress()
        //creates a new dispatch on the cloud
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Common.dispatchCollectionRef.document(newDispatch.dispatchId).set(newDispatch)
                    .await()
                hideProgress()
                val navBackToDispatch =
                    CreateNewDispatchDirections.actionCreateNewDispatchToDispatchScreenLanding()
                findNavController().navigate(navBackToDispatch)

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    //activity?.toast(e.message.toString())
                    Log.d(TAG, "createNewDispatch: ${e.message.toString()}")
                }
            }
        }

    }

    private fun showDatePicker(view: View) {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog =
            DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
                // Update the selected date in the calendar instance
                calendar.set(selectedYear, selectedMonth, selectedDay)

                // Perform any desired action with the selected date
                // For example, update a TextView with the selected date
                val formattedDate =
                    SimpleDateFormat(Common.DATE_FORMAT, Locale.getDefault()).format(calendar.time)
                val bookAppointmentDate = view as TextInputEditText
                bookAppointmentDate.setText(formattedDate)
            }, year, month, day)

        //set the calendar to only allow selection of future dates
        datePickerDialog.datePicker.minDate = calendar.timeInMillis
        datePickerDialog.show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}

