package com.androidstrike.schoolprojects.thefreighterapp.features.auth

import android.os.Bundle
import android.util.Log
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.androidstrike.schoolprojects.thefreighterapp.R
import com.androidstrike.schoolprojects.thefreighterapp.databinding.FragmentRegisterVehicleBinding
import com.androidstrike.schoolprojects.thefreighterapp.models.UserData
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.userCollectionRef
import com.androidstrike.schoolprojects.thefreighterapp.utils.hideProgress
import com.androidstrike.schoolprojects.thefreighterapp.utils.showProgress
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RegisterVehicle : Fragment() {

    private var _binding: FragmentRegisterVehicleBinding? = null
    private val binding get() = _binding!!


    private val args: RegisterVehicleArgs by navArgs()

    private lateinit var role: String
    private lateinit var uid: String


    private lateinit var vehiclePlateNumber: String
    private lateinit var driverLicenseNumber: String
    private lateinit var coverageLocation1: String
    private lateinit var coverageLocation2: String

    private var vehiclePlateNumberOkay = false
    private var driverLicenseNumberOkay = false

    private val TAG = "RegisterVehicle"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentRegisterVehicleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        role = args.role
        uid = args.uid
        with(binding){

            //watch the textfield and know when the user has clicked it
            //then assign whatever has been typed to a variable and perform validation if the user has clicked out
            registerVehiclePlateNumber.setOnFocusChangeListener { v, hasFocus ->
                val vehiclePlateNumberLayout = v as TextInputEditText
                vehiclePlateNumber = vehiclePlateNumberLayout.text.toString().trim()
                if (!hasFocus) {
                    if (vehiclePlateNumber.isEmpty()) {
                        textInputLayoutRegisterVehiclePlateNumber.error =
                            resources.getString(R.string.invalid_vehicle_plate_number)
                    } else {
                        textInputLayoutRegisterVehiclePlateNumber.error = null
                        vehiclePlateNumberOkay = true
                    }
                }
            }
            registerDriverLicenseNumber.setOnFocusChangeListener { v, hasFocus ->
                val driverLicenseNumberLayout = v as TextInputEditText
                driverLicenseNumber = driverLicenseNumberLayout.text.toString().trim()
                if (!hasFocus) {
                    if (driverLicenseNumber.isEmpty()) {
                        textInputLayoutRegisterDriverLicenseNumber.error =
                            resources.getString(R.string.invalid_driver_license)
                    } else {
                        textInputLayoutRegisterDriverLicenseNumber.error = null
                        driverLicenseNumberOkay = true
                    }
                }
            }

            coverageLocation1 = coverageCountryOne.defaultCountryName
            coverageLocation2 = coverageCountryTwo.defaultCountryName



            coverageCountryOne.setOnCountryChangeListener {
                coverageLocation1 = coverageCountryOne.selectedCountryName

            }
            coverageCountryTwo.setOnCountryChangeListener {
                coverageLocation2 = coverageCountryTwo.selectedCountryName
                }
            userRegisterVehicleBtn.setOnClickListener {
                vehiclePlateNumber = registerVehiclePlateNumber.text.toString().trim()
                driverLicenseNumber = registerDriverLicenseNumber.text.toString().trim()
                Log.d(TAG, "onViewCreated: $vehiclePlateNumberOkay $driverLicenseNumberOkay")

                if (vehiclePlateNumberOkay && driverLicenseNumberOkay){
                    addToDriverInfo(vehiclePlateNumber, driverLicenseNumber, coverageLocation1, coverageLocation2)
                }
            }
        }
    }

    private fun addToDriverInfo(
        vehiclePlateNumber: String,
        driverLicenseNumber: String,
        coverageLocation1: String?,
        coverageLocation2: String?
    ) {
        requireContext().showProgress()

        CoroutineScope(Dispatchers.IO).launch {
            val userCollectionRef =
                userCollectionRef.document(uid)

            val updates = hashMapOf<String, Any>(
                "vehiclePlateNumber" to vehiclePlateNumber,
                "driverLicenseNumber" to driverLicenseNumber,
                "coverageLocation1" to coverageLocation1!!,
                "coverageLocation2" to coverageLocation2!!,
            )

            userCollectionRef.update(updates).addOnSuccessListener {
                hideProgress()
                val navToContactPersonReg = RegisterVehicleDirections.actionRegisterVehicleToRegisterContact(role, uid)
                findNavController().navigate(navToContactPersonReg)
            }

        }
        Log.d(TAG, "addToDriverInfo: $vehiclePlateNumber, $driverLicenseNumber, $coverageLocation1, $coverageLocation2")

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}