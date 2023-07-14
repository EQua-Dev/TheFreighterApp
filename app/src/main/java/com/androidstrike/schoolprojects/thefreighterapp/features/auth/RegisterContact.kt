package com.androidstrike.schoolprojects.thefreighterapp.features.auth

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.androidstrike.schoolprojects.thefreighterapp.R
import com.androidstrike.schoolprojects.thefreighterapp.databinding.FragmentRegisterContactBinding
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common
import com.androidstrike.schoolprojects.thefreighterapp.utils.hideProgress
import com.androidstrike.schoolprojects.thefreighterapp.utils.showProgress
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RegisterContact : Fragment() {

    private var _binding: FragmentRegisterContactBinding? = null
    private val binding get() = _binding!!

    private val args: RegisterContactArgs by navArgs()

    private lateinit var role: String
    private lateinit var uid: String


    private lateinit var driverContactPersonName: String
    private lateinit var driverContactPersonPhoneNumber: String
    private lateinit var driverContactPersonAddress: String
    private lateinit var driverContactPersonCity: String
    private lateinit var driverContactPersonCountry: String
    private lateinit var driverContactPersonCountryCode: String

    private var driverContactPersonNameOkay = false
    private var driverContactPersonPhoneNumberOkay = false
    private var driverContactPersonAddressOkay = false
    private var driverContactPersonCityOkay = false

    @Suppress("PrivatePropertyName")
    private val TAG = "RegisterContact"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentRegisterContactBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        role = args.role
        uid = args.uid
        with(binding){

            driverContactPersonCountryCode = registerContactCountryCodePicker.defaultCountryCodeWithPlus
            driverContactPersonCountry = contactAddressCountry.defaultCountryName

            registerContactName.setOnFocusChangeListener { v, hasFocus ->
                val contactNameLayout = v as TextInputEditText
                driverContactPersonName = contactNameLayout.text.toString().trim()
                if (!hasFocus) {
                    if (driverContactPersonName.isEmpty()) {
                        textInputLayoutRegisterContactName.error =
                            resources.getString(R.string.invalid_full_name)
                    } else {
                        textInputLayoutRegisterContactName.error = null
                        driverContactPersonNameOkay = true
                    }
                }
            }
            registerContactAddressLine.setOnFocusChangeListener { v, hasFocus ->
                val contactAddressLineLayout = v as TextInputEditText
                driverContactPersonAddress = contactAddressLineLayout.text.toString().trim()
                if (!hasFocus) {
                    if (driverContactPersonAddress.isEmpty()) {
                        textInputLayoutRegisterContactAddressLine.error =
                            resources.getString(R.string.invalid_address_line)
                    } else {
                        textInputLayoutRegisterContactAddressLine.error = null
                        driverContactPersonAddressOkay = true
                    }
                }
            }
            registerContactCity.setOnFocusChangeListener { v, hasFocus ->
                val contactCityLayout = v as TextInputEditText
                driverContactPersonCity = contactCityLayout.text.toString().trim()
                if (!hasFocus) {
                    if (driverContactPersonCity.isEmpty()) {
                        textInputLayoutRegisterContactCity.error =
                            resources.getString(R.string.invalid_city)
                    } else {
                        textInputLayoutRegisterContactCity.error = null
                        driverContactPersonCityOkay = true
                    }
                }
            }
            registerContactPhoneNumber.setOnFocusChangeListener { v, hasFocus ->
                val contactPhoneNumberLayout = v as TextInputEditText
                driverContactPersonPhoneNumber = contactPhoneNumberLayout.text.toString().trim()
                if (!hasFocus) {
                    if (driverContactPersonPhoneNumber.isEmpty() || driverContactPersonPhoneNumber.length < 9){
                        textInputLayoutRegisterContactPhoneNumber.error = resources.getString(R.string.invalid_phone_number)
                    }else{
                        textInputLayoutRegisterContactPhoneNumber.error = null
                        driverContactPersonPhoneNumberOkay = true
                    }
                }
            }
            contactAddressCountry.setOnCountryChangeListener {
                driverContactPersonCountryCode = contactAddressCountry.selectedCountryCodeWithPlus
                driverContactPersonCountry = contactAddressCountry.selectedCountryName
                // Handle selected country code and name
                contactAddressCountry.setNumberAutoFormattingEnabled(true)
            }

            userRegisterContactBtn.setOnClickListener {
                driverContactPersonName = registerContactName.text.toString().trim()
                driverContactPersonPhoneNumber = "$driverContactPersonCountryCode${registerContactPhoneNumber.text.toString().trim()}"
                driverContactPersonAddress = registerContactAddressLine.text.toString().trim()
                driverContactPersonCity = registerContactCity.text.toString().trim()
                
                if (driverContactPersonNameOkay && driverContactPersonPhoneNumberOkay && driverContactPersonAddressOkay && driverContactPersonCityOkay){
                    addDriverContact(driverContactPersonCountry,
                            driverContactPersonName,
                            driverContactPersonPhoneNumber,
                            driverContactPersonAddress,
                            driverContactPersonCity)
                }
            }

        }

    }

    private fun addDriverContact(
        driverContactPersonCountry: String?,
        driverContactPersonName: String,
        driverContactPersonPhoneNumber: String,
        driverContactPersonAddress: String,
        driverContactPersonCity: String
    ) {
        requireContext().showProgress()

        CoroutineScope(Dispatchers.IO).launch {
            val userCollectionRef =
                Common.userCollectionRef.document(uid)

            val updates = hashMapOf<String, Any>(
                "driverContactPersonCountry" to driverContactPersonCountry!!,
                "driverContactPersonName" to driverContactPersonName,
                "driverContactPersonPhoneNumber" to driverContactPersonPhoneNumber,
                "driverContactPersonAddress" to driverContactPersonAddress,
                "driverContactPersonCity" to driverContactPersonCity,
            )

            userCollectionRef.update(updates).addOnSuccessListener {
                hideProgress()
                val navToHome = RegisterContactDirections.actionRegisterContactToClientHome(role)
                findNavController().navigate(navToHome)
            }

        }
        Log.d(TAG, "addDriverContact: " +
                "$driverContactPersonCountry\n" +
                "$driverContactPersonName\n" +
                "$driverContactPersonPhoneNumber\n" +
                "$driverContactPersonAddress\n" +
                driverContactPersonCity
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}