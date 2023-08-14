package com.androidstrike.schoolprojects.thefreighterapp.features.home.client.profile

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.androidstrike.schoolprojects.thefreighterapp.R
import com.androidstrike.schoolprojects.thefreighterapp.databinding.FragmentProfileBinding
import com.androidstrike.schoolprojects.thefreighterapp.models.UserData
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.auth
import com.androidstrike.schoolprojects.thefreighterapp.utils.getDate
import com.androidstrike.schoolprojects.thefreighterapp.utils.toast
import com.androidstrike.schoolprojects.thefreighterapp.utils.visible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
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


        val user = getUser(auth.uid!!)

        with(binding){
            when(user!!.role){
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

                profileIconText.text = user.fullName.substring(0,1)
                profileNameText.text = user.fullName
                profileDateJoinedText.text = resources.getString(R.string.date_joined, getDate(user.dateJoined.toLong(), "dd, MMM, yyyy"))
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


                    profileIconText.text = user.fullName.substring(0,1)
                    profileNameText.text = user.fullName
                    profileDateJoinedText.text = resources.getString(R.string.date_joined, getDate(user.dateJoined.toLong(), "dd, MMM, yyyy"))
                    profileUserEmail.text = user.email
                    profileUserPhoneNumber.text = user.phoneNumber
                    profileUserDateOfBirth.text = user.dateOfBirth
                    profileUserCountryOfResidence.text = user.countryOfResidence
                    profileUserWeighingCost.text = resources.getString(R.string.money_text, user.weigherCost)
                }
                driverRole -> {
                    profileUserVehiclePlateNumber.text = user.vehiclePlateNumber
                    profileUserDriverLicenseNumber.text = user.driverLicenseNumber
                    profileUserCoverageCountry1.text = user.coverageLocation1
                    profileUserCoverageCountry2.text = user.coverageLocation2
                    profileUserDriverContactPersonName.text = user.driverContactPersonName
                    profileUserDriverContactPersonPhoneNumber.text = user.driverContactPersonPhoneNumber
                    profileUserDriverContactPersonAddress.text = resources.getString(R.string.dispatch_detail_address, user.driverContactPersonAddress, user.driverContactPersonCity, user.driverContactPersonCountry)

                    layoutWeighingCost.visible(false)

                    profileIconText.text = user.fullName.substring(0,1)
                    profileNameText.text = user.fullName
                    profileDateJoinedText.text = resources.getString(R.string.date_joined, getDate(user.dateJoined.toLong(), "dd, MMM, yyyy"))
                    profileUserEmail.text = user.email
                    profileUserPhoneNumber.text = user.phoneNumber
                    profileUserDateOfBirth.text = user.dateOfBirth
                    profileUserCountryOfResidence.text = user.countryOfResidence
                }
            }

        }
//        vehicle_plate_number_title
//        driver_license_number_title
//        layout_coverage_location1
//        layout_coverage_location2
//        layout_contact_person_name
//        layout_contact_person_phone_number
//        layout_contact_person_address
//        layout_weighing_cost
//        profile_icon_text
//        profile_name_text
//        profile_date_joined_text
//        profile_user_email
//        profile_user_phone_number
//        tv_profile_edit_phone_number
//        profile_user_date_of_birth
//        profile_user_country_of_residence
//        tv_profile_edit_country_of_residence
//        profile_user_vehicle_plate_number
//        profile_user_driver_license_number
//        profile_user_coverage_country1
//        tv_profile_edit_coverage_country1
//        profile_user_coverage_country2
//        tv_profile_edit_coverage_country2
//        profile_user_driver_contact_person_name
//        tv_profile_edit_driver_contact_person_name
//        profile_user_driver_contact_person_phone_number
//        tv_profile_edit_driver_contact_person_phone_number
//        profile_user_driver_contact_person_address
//        tv_profile_edit_driver_contact_person_address
//        profile_user_weighing_cost
//        tv_profile_edit_weighing
//
//
//        fullName
//        email
//        phoneNumber
//        dateOfBirth
//        countryOfResidence
//        dateJoined
//        role
//        vehiclePlateNumber
//        driverLicenseNumber
//        coverageLocation1
//        coverageLocation2
//        driverContactPersonName
//        driverContactPersonPhoneNumber
//        driverContactPersonAddress
//        driverContactPersonCity
//        driverContactPersonCountry
//        weigherCost



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