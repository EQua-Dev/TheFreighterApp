package com.androidstrike.schoolprojects.thefreighterapp.features.auth

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.androidstrike.schoolprojects.thefreighterapp.R
import com.androidstrike.schoolprojects.thefreighterapp.databinding.FragmentSignInBinding
import com.androidstrike.schoolprojects.thefreighterapp.models.UserData
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.auth
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.userCollectionRef
import com.androidstrike.schoolprojects.thefreighterapp.utils.enable
import com.androidstrike.schoolprojects.thefreighterapp.utils.hideProgress
import com.androidstrike.schoolprojects.thefreighterapp.utils.showProgress
import com.androidstrike.schoolprojects.thefreighterapp.utils.toast
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SignIn : Fragment() {

    //this line connects the design file to this class file, making this file to be able to access the views
    private var _binding: FragmentSignInBinding? = null
    private val binding get() = _binding!!

    //this line imports the argument (parameters) passed into this class such as the user role
    private val args: SignInArgs by navArgs()

    private lateinit var email: String
    private lateinit var password: String

    lateinit var loggedInUser: UserData

    lateinit var role: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentSignInBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        role = args.role

        loggedInUser = UserData()

        //navigates to the register screen when the text is clicked
        binding.accountLogInCreateAccount.setOnClickListener {
            val navToRegister = SignInDirections.actionSignInToRegister(role)
            findNavController().navigate(navToRegister)
        }

        //navigates to the register screen when the text is clicked
        binding.accountLogInForgotPasswordPrompt.setOnClickListener {
            val navToResetPassword = SignInDirections.actionSignInToResetPassword(role)
            findNavController().navigate(navToResetPassword)
        }

        with(binding){

            //the sign in button is disabled at first and enabled when the fields have been filled
            accountLogInBtnLogin.enable(false)

            //watch the password text field  for when characters  will be entered
            signInPassword.addTextChangedListener {
                email = signInEmail.text.toString().trim()
                password = it.toString().trim()
                binding.accountLogInBtnLogin.apply {
                    enable(email.isNotEmpty() && password.isNotEmpty())
                    setOnClickListener {
                        //call the login function if the fields are not empty
                        login(email, password)
                    }
                }
            }

        }

    }

    private fun login(email: String, password: String) {

        requireContext().showProgress()

        //firebase functionality to sign the user in with the entered email and password
        email.let { auth.signInWithEmailAndPassword(it, password) }
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            withContext(Dispatchers.Main) {
//                                pbLoading.visible(false)
                                hideProgress()
                                getUser()
                            }


                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                hideProgress()
                                requireActivity().toast(e.message.toString())
                            }
                        }
                    }
                } else {
                    //pbLoading.visible(false)
                    hideProgress()
                    activity?.toast(it.exception?.message.toString())
                }
            }

    }

    private fun getUser() {
        CoroutineScope(Dispatchers.IO).launch {
            userCollectionRef.whereEqualTo("userId", auth.uid.toString())
                .get()
                .addOnSuccessListener { querySnapshot: QuerySnapshot ->
                    //get the details of the signed in user
                    for (document in querySnapshot.documents) {
                        val item = document.toObject(UserData::class.java)
                        if (item?.userId == auth.uid.toString())
                            loggedInUser = item
                    }
                    //take the user to the phone verification screen
                    val navToPhoneVerification =
                        SignInDirections.actionSignInToPhoneVerification(role = role, loggedInUser.phoneNumber)
                    findNavController().navigate(navToPhoneVerification)

                }
        }

    }


    override fun onDestroyView() {
        super.onDestroyView()
    }
}