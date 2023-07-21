package com.androidstrike.schoolprojects.thefreighterapp.features.home.client.dispatch

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.androidstrike.schoolprojects.thefreighterapp.R
import com.androidstrike.schoolprojects.thefreighterapp.databinding.FragmentCreateNewDispatchBinding
import com.androidstrike.schoolprojects.thefreighterapp.models.Dispatch
import com.androidstrike.schoolprojects.thefreighterapp.models.UserData
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.STATUS_AWAITING_WEIGHER
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.STATUS_DRAFT
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.STATUS_PENDING_DRIVER
import com.androidstrike.schoolprojects.thefreighterapp.utils.hideProgress
import com.androidstrike.schoolprojects.thefreighterapp.utils.showProgress
import com.androidstrike.schoolprojects.thefreighterapp.utils.toast
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CreateNewDispatch : Fragment() {

    private var _binding: FragmentCreateNewDispatchBinding? = null
    private val binding get() = _binding!!

    private val args: CreateNewDispatchArgs by navArgs()
    private lateinit var loggedInUser: UserData

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
    private lateinit var pickupDate: String
    private var pickupDateOkay = false
    private lateinit var dropOffAddress: String
    private var dropOffAddressOkay = false
    private lateinit var dropOffProvince: String
    private var dropOffProvinceOkay = false
    private lateinit var dropOffCountry: String

    private val calendar = Calendar.getInstance()

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

        loggedInUser = args.loggedInUser

        with(binding) {


            val packageTypes = resources.getStringArray(R.array.package_types)
            val packageTypesAdapter =
                ArrayAdapter(requireContext(), R.layout.drop_down_item, packageTypes)
            newDispatchChoosePackageType.setAdapter(packageTypesAdapter)

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
                dropOffAddress = dispatchDropOffAddress.text.toString().trim()
                dropOffProvince = dispatchDropOffProvince.text.toString().trim()
                dropOffCountry = dispatchDropOffCountry.selectedCountryName
                pickupDate = dispatchPickupDate.text.toString().trim()


                if (packageTypeOkay && weightOkay && pickupAddressOkay && pickupProvinceOkay && pickupDateOkay && dropOffAddressOkay && dropOffProvinceOkay) {

                    val newDispatch = Dispatch(
                        packageType = packageType,
                        weight = weight,
                        status = status,
                        client = client,
                        lastUpdater = lastUpdater,
                        pickupAddress = pickupAddress,
                        pickupProvince = pickupProvince,
                        pickupCountry = pickupCountry,
                        dropOffAddress = dropOffAddress,
                        dropOffProvince = dropOffProvince,
                        dropOffCountry = dropOffCountry,
                        pickupDate = pickupDate,
                        dispatchId = System.currentTimeMillis().toString(),
                        dateCreated = System.currentTimeMillis().toString()
                    )

                    createNewDispatch(newDispatch)
                } else {
                    requireContext().toast(resources.getString(R.string.missing_fields))
                }
            }

            textDispatchPackageWeightContractors.setOnClickListener {
                //prompt to fill out other details first
                launchExternalContractorsDialog()
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
                dropOffAddress = dispatchDropOffAddress.text.toString().trim()
                dropOffProvince = dispatchDropOffProvince.text.toString().trim()
                dropOffCountry = dispatchDropOffCountry.selectedCountryName
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
                    dropOffAddress = dropOffAddress,
                    dropOffProvince = dropOffProvince,
                    dropOffCountry = dropOffCountry,
                    pickupDate = pickupDate,
                    dispatchId = System.currentTimeMillis().toString(),
                    dateCreated = System.currentTimeMillis().toString()
                    )
                createNewDispatch(newDispatchDraft)
            }

        }


    }

    private fun launchExternalContractorsDialog() {


    }


    private fun createNewDispatch(newDispatch: Dispatch) {
        requireContext().showProgress()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Common.dispatchCollectionRef.document(newDispatch.dispatchId).set(newDispatch)
                    .await()
                hideProgress()
                val navBackToDispatch = CreateNewDispatchDirections.actionCreateNewDispatchToDispatchScreenLanding()
                findNavController().navigate(navBackToDispatch)

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    activity?.toast(e.message.toString())
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