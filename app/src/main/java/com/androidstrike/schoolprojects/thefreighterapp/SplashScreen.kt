package com.androidstrike.schoolprojects.thefreighterapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.androidstrike.schoolprojects.thefreighterapp.databinding.FragmentSplashScreenBinding
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.auth

class SplashScreen : Fragment() {
   
    private var _binding: FragmentSplashScreenBinding? = null
    private val binding get()= _binding!!
    
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentSplashScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        with(binding) {
            txtSplash.animate().setDuration(2000).alpha(1f).withEndAction {


                if (auth.currentUser != null) {
                    // User is logged in
                    // Perform necessary actions
                    val navToHome = SplashScreenDirections.actionSplashScreenToClientHome(resources.getString(R.string.client))
                    findNavController().navigate(navToHome)
                } else {
                    // User is not logged in
                    // Redirect to login screen or perform other actions
                    val navToSignIn = SplashScreenDirections.actionSplashScreenToIntroScreen()
                    findNavController().navigate(navToSignIn)
                }


            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}