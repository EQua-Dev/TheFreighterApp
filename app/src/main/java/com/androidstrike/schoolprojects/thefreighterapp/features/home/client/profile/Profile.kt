package com.androidstrike.schoolprojects.thefreighterapp.features.home.client.profile

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import com.androidstrike.schoolprojects.thefreighterapp.R
import com.androidstrike.schoolprojects.thefreighterapp.databinding.FragmentProfileBinding
import com.androidstrike.schoolprojects.thefreighterapp.models.UserData
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.auth
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.userCollectionRef
import com.androidstrike.schoolprojects.thefreighterapp.utils.enable
import com.androidstrike.schoolprojects.thefreighterapp.utils.getDate
import com.androidstrike.schoolprojects.thefreighterapp.utils.hideProgress
import com.androidstrike.schoolprojects.thefreighterapp.utils.showProgress
import com.androidstrike.schoolprojects.thefreighterapp.utils.toast
import com.androidstrike.schoolprojects.thefreighterapp.utils.visible
import com.google.android.material.textfield.TextInputEditText
import com.hbb20.CountryCodePicker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await

class Profile : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var clientRole: String
    private lateinit var driverRole: String
    private lateinit var weigherRole: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        clientRole = resources.getString(R.string.client)
        driverRole = resources.getString(R.string.driver)
        weigherRole = resources.getString(R.string.weigher)


        loadView()

    }

    private fun launchEditPhoneNumberDialog() {

        val builder =
            layoutInflater.inflate(R.layout.edit_phone_number_dialog, null)

        val updatePhoneCountryCodePicker =
            builder.findViewById<CountryCodePicker>(R.id.update_phone_number_country_code_picker)
        val updatePhoneEditText =
            builder.findViewById<TextInputEditText>(R.id.update_customer_phone)
        val cancelUpdateBtn = builder.findViewById<Button>(R.id.btn_cancel)
        val updateBtn = builder.findViewById<Button>(R.id.btn_update)


        val dialog = AlertDialog.Builder(requireContext())
            .setView(builder)
            .setCancelable(false)
            .create()

        var updatedCountryCode = updatePhoneCountryCodePicker.defaultCountryCodeWithPlus



        updatePhoneCountryCodePicker.setOnCountryChangeListener {
            updatedCountryCode = updatePhoneCountryCodePicker.selectedCountryCodeWithPlus
            // Handle selected country code and name
            updatePhoneCountryCodePicker.setNumberAutoFormattingEnabled(true)
        }
        updatePhoneEditText.addTextChangedListener { phoneNumber ->
            val newPhoneNumber = phoneNumber.toString().trim()
            updateBtn.apply {
                enable(newPhoneNumber.isNotEmpty())
                setOnClickListener {
                    val userUpdatedPhoneNumber = "$updatedCountryCode$newPhoneNumber"
                    requireContext().showProgress()
                    CoroutineScope(Dispatchers.IO).launch {
                        userCollectionRef.document(auth.uid!!)
                            .update("phoneNumber", userUpdatedPhoneNumber).addOnSuccessListener {
                                hideProgress()
                            requireContext().toast("Phone number updated")
                            loadView()
                            dialog.dismiss()
                        }.addOnFailureListener { e ->
                                hideProgress()
                            requireContext().toast(e.message.toString())
                        }
                    }
                }
            }
        }

        cancelUpdateBtn.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun loadView() {
        val user = getUser(auth.uid!!)

        with(binding) {
            when (user!!.role) {
                clientRole -> {

                    vehiclePlateNumberTitle.visible(false)
                    profileUserVehiclePlateNumber.visible(false)
                    driverLicenseNumberTitle.visible(false)
                    profileUserDriverLicenseNumber.visible(false)
                    layoutCoverageLocation1.visible(false)
                    layoutCoverageLocation2.visible(false)
                    layoutContactPersonName.visible(false)
                    layoutContactPersonPhoneNumber.visible(false)
                    layoutContactPersonAddress.visible(false)
                    layoutWeighingCost.visible(false)

                    profileIconText.text = user.fullName.substring(0, 1)
                    profileNameText.text = user.fullName
                    profileDateJoinedText.text = resources.getString(
                        R.string.date_joined,
                        getDate(user.dateJoined.toLong(), "dd, MMM, yyyy")
                    )
                    profileUserEmail.text = user.email
                    profileUserPhoneNumber.text = user.phoneNumber
                    profileUserDateOfBirth.text = user.dateOfBirth
                    profileUserCountryOfResidence.text = user.countryOfResidence
                }

                weigherRole -> {
                    vehiclePlateNumberTitle.visible(false)
                    profileUserVehiclePlateNumber.visible(false)
                    driverLicenseNumberTitle.visible(false)
                    profileUserDriverLicenseNumber.visible(false)
                    layoutCoverageLocation1.visible(false)
                    layoutCoverageLocation2.visible(false)
                    layoutContactPersonName.visible(false)
                    layoutContactPersonPhoneNumber.visible(false)
                    layoutContactPersonAddress.visible(false)


                    profileIconText.text = user.fullName.substring(0, 1)
                    profileNameText.text = user.fullName
                    profileDateJoinedText.text = resources.getString(
                        R.string.date_joined,
                        getDate(user.dateJoined.toLong(), "dd, MMM, yyyy")
                    )
                    profileUserEmail.text = user.email
                    profileUserPhoneNumber.text = user.phoneNumber
                    profileUserDateOfBirth.text = user.dateOfBirth
                    profileUserCountryOfResidence.text = user.countryOfResidence
                    profileUserWeighingCost.text =
                        resources.getString(R.string.money_text, user.weigherCost)
                }

                driverRole -> {
                    profileUserVehiclePlateNumber.text = user.vehiclePlateNumber
                    profileUserDriverLicenseNumber.text = user.driverLicenseNumber
                    profileUserCoverageCountry1.text = user.coverageLocation1
                    profileUserCoverageCountry2.text = user.coverageLocation2
                    profileUserDriverContactPersonName.text = user.driverContactPersonName
                    profileUserDriverContactPersonPhoneNumber.text =
                        user.driverContactPersonPhoneNumber
                    profileUserDriverContactPersonAddress.text = resources.getString(
                        R.string.dispatch_detail_address,
                        user.driverContactPersonAddress,
                        user.driverContactPersonCity,
                        user.driverContactPersonCountry
                    )

                    layoutWeighingCost.visible(false)

                    profileIconText.text = user.fullName.substring(0, 1)
                    profileNameText.text = user.fullName
                    profileDateJoinedText.text = resources.getString(
                        R.string.date_joined,
                        getDate(user.dateJoined.toLong(), "dd, MMM, yyyy")
                    )
                    profileUserEmail.text = user.email
                    profileUserPhoneNumber.text = user.phoneNumber
                    profileUserDateOfBirth.text = user.dateOfBirth
                    profileUserCountryOfResidence.text = user.countryOfResidence
                }
            }

            tvProfileEditPhoneNumber.visible(true)
            tvProfileEditPhoneNumber.setOnClickListener {
                launchEditPhoneNumberDialog()
            }
        }


    }


    private fun getUser(user: String): UserData? {
//        requireContext().showProgress()
        val deferred = CoroutineScope(Dispatchers.IO).async {
            try {
                val snapshot = Common.userCollectionRef.document(user).get().await()
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

        //hideProgress()

        return runBlocking { deferred.await() }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}