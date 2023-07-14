package com.androidstrike.schoolprojects.thefreighterapp.features.auth

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.androidstrike.schoolprojects.thefreighterapp.R
import com.androidstrike.schoolprojects.thefreighterapp.databinding.FragmentRegisterBinding
import com.androidstrike.schoolprojects.thefreighterapp.models.UserData
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.DATE_FORMAT
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.auth
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.userCollectionRef
import com.androidstrike.schoolprojects.thefreighterapp.utils.enable
import com.androidstrike.schoolprojects.thefreighterapp.utils.getDateFromString
import com.androidstrike.schoolprojects.thefreighterapp.utils.hideProgress
import com.androidstrike.schoolprojects.thefreighterapp.utils.isAbove18
import com.androidstrike.schoolprojects.thefreighterapp.utils.isInvalidDate
import com.androidstrike.schoolprojects.thefreighterapp.utils.isPasswordValid
import com.androidstrike.schoolprojects.thefreighterapp.utils.showProgress
import com.androidstrike.schoolprojects.thefreighterapp.utils.toast
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class Register : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val args: RegisterArgs by navArgs()
    private lateinit var role: String

    private lateinit var userFullName: String
    private lateinit var userEmail: String
    private lateinit var userDateOfBirth: String
    private lateinit var userPhoneNumber: String
    private lateinit var userPassword: String
    private lateinit var userConfirmPassword: String
    private lateinit var userCountry: String
    private lateinit var userCountryCode: String

    private var usernameOkay = false
    private var userEmailOkay = false
    private var userDateOfBirthOkay = false
    private var userPhoneNumberOkay = false
    private var userPasswordOkay = false
    private var userConfirmPasswordOkay = false

    private var clientUserData = UserData()

    private val calendar = Calendar.getInstance()

    @Suppress("PrivatePropertyName")
    private val TAG = "Register"


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        role = args.role
        with(binding) {

            registerUserName.setOnFocusChangeListener { v, hasFocus ->
                val userNameLayout = v as TextInputEditText
                userFullName = userNameLayout.text.toString().trim()
                if (!hasFocus) {
                    if (userFullName.isEmpty()) {
                        textInputLayoutRegisterUserName.error =
                            resources.getString(R.string.invalid_full_name)
                    } else {
                        textInputLayoutRegisterUserName.error = null
                        usernameOkay = true
                    }
                }
            }
            registerCustomerEmail.setOnFocusChangeListener { v, hasFocus ->
                val userEmailLayout = v as TextInputEditText
                userEmail = userEmailLayout.text.toString().trim()
                if (!hasFocus) {
                    if (userEmail.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(userEmail).matches()) {
                        textInputLayoutRegisterCustomerEmail.error =
                            resources.getString(R.string.invalid_email)
                    } else {
                        textInputLayoutRegisterUserName.error = null
                        userEmailOkay = true
                    }
                }
            }

            registerCustomerDateOfBirth.setOnFocusChangeListener { v, hasFocus ->
                val userDateOfBirthLayout = v as TextInputEditText
                userDateOfBirth = userDateOfBirthLayout.text.toString().trim()
                if (hasFocus) {
                    showDatePicker(v)
                }else{
                    if (userDateOfBirth.isEmpty() || isInvalidDate(userDateOfBirth, DATE_FORMAT)|| !isAbove18(
                            getDateFromString(
                                userDateOfBirth,
                                DATE_FORMAT
                            )!!)){
                        textInputLayoutRegisterCustomerDateOfBirth.error = resources.getString(R.string.underage)
                    }else{
                        textInputLayoutRegisterCustomerDateOfBirth.error = null

                        userDateOfBirthOkay = true
                    }
                }
            }


            registerCustomerPhone.setOnFocusChangeListener { v, hasFocus ->
                val userPhoneNumberLayout = v as TextInputEditText
                userPhoneNumber = userPhoneNumberLayout.text.toString().trim()
                if (!hasFocus) {
                    if (userPhoneNumber.isEmpty() || userPhoneNumber.length < 9){
                        textInputLayoutRegisterCustomerPhone.error = resources.getString(R.string.invalid_phone_number)
                    }else{
                        textInputLayoutRegisterCustomerPhone.error = null

                        userPhoneNumberOkay = true
                    }
                }
            }

            registerCustomerPassword.setOnFocusChangeListener { v, hasFocus ->
                val passwordLayout = v as TextInputEditText
                userPassword = passwordLayout.text.toString().trim()
                if (!hasFocus) {
                    if (!isPasswordValid(userPassword)) {
                        textInputLayoutRegisterCustomerPassword.error = resources.getString(R.string.invalid_password)
                    } else {
                        textInputLayoutRegisterCustomerPassword.error = null // Clear any previous error
                        userPasswordOkay = true

                    }
                }
            }

            registerCustomerConfirmPassword.setOnFocusChangeListener { v, hasFocus ->
                val confirmPasswordLayout = v as TextInputEditText
                userConfirmPassword = confirmPasswordLayout.text.toString().trim()

                if (!hasFocus) {
                    if (userConfirmPassword != userPassword) {
                        textInputLayoutRegisterCustomerConfirmPassword.error =
                            resources.getText(R.string.password_not_match) // Display an error message
                    } else {
                        binding.textInputLayoutRegisterCustomerConfirmPassword.error =
                            null // Clear any previous error
                        userConfirmPasswordOkay = true

                    }
                }
            }



            userCountryCode = countryCodePicker.defaultCountryCodeWithPlus
            userCountry = countryCodePicker.defaultCountryName



            countryCodePicker.setOnCountryChangeListener {
                userCountryCode = countryCodePicker.selectedCountryCodeWithPlus
                userCountry = countryCodePicker.selectedCountryName
                // Handle selected country code and name
                countryCodePicker.setNumberAutoFormattingEnabled(true)
            }
            userRegisterBtn.setOnClickListener {
                userFullName = registerUserName.text.toString().trim()
                userEmail = registerCustomerEmail.text.toString().trim()
                userDateOfBirth = registerCustomerDateOfBirth.text.toString().trim()
                userPhoneNumber = "$userCountryCode${registerCustomerPhone.text.toString().trim()}"
                userPassword = registerCustomerPassword.text.toString().trim()
                Log.d(TAG, "userFullName: $userFullName")
                if (usernameOkay && userEmailOkay && userPhoneNumberOkay && userDateOfBirthOkay && userPasswordOkay && userConfirmPasswordOkay){

                    clientUserData = UserData(
                        fullName = userFullName,
                        email = userEmail,
                        phoneNumber = userPhoneNumber,
                        dateOfBirth = userDateOfBirth,
                        countryOfResidence = userCountry,
                        dateJoined = System.currentTimeMillis().toString(),
                        role = role
                    )
                    createUser(clientUserData)
                }else{
                    requireContext().toast(resources.getString(R.string.missing_fields))
                }

            }
            userRegisterLoginPrompt.setOnClickListener {
                val navToSignIn = RegisterDirections.actionRegisterToSignIn(role)
                findNavController().navigate(navToSignIn)
            }
        }
    }

    private fun createUser(userData: UserData) {



        val mAuth = FirebaseAuth.getInstance()
        requireContext().showProgress()
        mAuth.createUserWithEmailAndPassword(userData.email, userPassword)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    val newUserId = mAuth.uid
                    val user = mAuth.currentUser
                    val newUser = UserData(
                        userId = newUserId.toString(),
                        fullName = userData.fullName,
                                email = userData.email,
                                phoneNumber = userData.phoneNumber,
                                dateOfBirth = userData.dateOfBirth,
                                countryOfResidence = userData.countryOfResidence,
                                dateJoined = userData.dateJoined,
                                role = role
                    )
                    user?.sendEmailVerification()
                        ?.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                requireContext().toast(resources.getString(R.string.email_sent))
                                launchVerifyEmailDialog(newUser)
                            }
                        }
                } else {
                    it.exception?.message?.let { message ->
                        hideProgress()
                        requireActivity().toast(message)
                    }
                }
            }

    }

    private fun launchVerifyEmailDialog(userData: UserData) {


        val builder =
            layoutInflater.inflate(R.layout.email_verification_dialog, null)

        val userEmail = builder.findViewById<TextView>(R.id.email_verification_email)

        val btnLinkClicked =
            builder.findViewById<Button>(R.id.email_verification_btn)

        userEmail.text = auth.currentUser?.email
        val dialog = AlertDialog.Builder(requireContext())
            .setView(builder)
            .setCancelable(false)
            .create()


        btnLinkClicked.setOnClickListener {
            auth.currentUser!!.reload().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    if (auth.currentUser!!.isEmailVerified) {
                        dialog.dismiss()
                        saveUser(userData)
                    } else {
                        hideProgress()
                        requireContext().toast(resources.getString(R.string.email_not_verified))
                    }
                } else {
                    requireContext().toast(task.exception.toString())
                }

            }

        }

        dialog.show()

    }

    private fun saveUser(userData: UserData) =
        CoroutineScope(Dispatchers.IO).launch {
            try {
                userCollectionRef.document(userData.userId).set(userData).await()
                hideProgress()
                when (role) {
                    resources.getString(R.string.client) -> {
                        val navToSignIn = RegisterDirections.actionRegisterToSignIn(role = role)
                        findNavController().navigate(navToSignIn)
                    }
                    resources.getString(R.string.driver) -> {
                        val navToVehicleReg = RegisterDirections.actionRegisterToRegisterVehicle(role = role, uid = userData.userId)
                        findNavController().navigate(navToVehicleReg)
                    }
                    else -> {
                        //show dialog to enter charge cost
                        Log.d(TAG, "saveUser: weigher")
                        withContext(Dispatchers.Main){
                            launchWeigherSetChargeDialog()
                        }
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    activity?.toast(e.message.toString())
                    Log.d(TAG, "saveUser: ${e.message.toString()}")
                }
            }


        }

    private fun launchWeigherSetChargeDialog() {
        Log.d(TAG, "launchWeigherSetChargeDialog: ")
        val builder =
            layoutInflater.inflate(R.layout.set_weigher_charge_dialog, null)

        val weigherCharge = builder.findViewById<TextInputEditText>(R.id.set_weigher_charge)

        val btnContinue =
            builder.findViewById<Button>(R.id.set_weigher_charge_btn)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(builder)
            .setCancelable(false)
            .create()

        btnContinue.enable(false)

        weigherCharge.addTextChangedListener {
            val weigherCost = it.toString().trim()
            btnContinue.apply {
                enable(weigherCost.isNotEmpty())
                setOnClickListener {
                    CoroutineScope(Dispatchers.IO).launch {
                        val userCollectionRef =
                            userCollectionRef.document(auth.uid!!)

                        val updates = hashMapOf<String, Any>(
                            "weigherCost" to weigherCost,
                        )

                        userCollectionRef.update(updates).addOnSuccessListener {
                            hideProgress()
                            dialog.dismiss()
                            val navToSignIn = RegisterDirections.actionRegisterToSignIn(role = role)
                            findNavController().navigate(navToSignIn)
                        }

                    }
                }
            }
        }

        dialog.show()

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
                    SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).format(calendar.time)
                val bookAppointmentDate = view as TextInputEditText
                bookAppointmentDate.setText(formattedDate)
            }, year, month, day)

        datePickerDialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}