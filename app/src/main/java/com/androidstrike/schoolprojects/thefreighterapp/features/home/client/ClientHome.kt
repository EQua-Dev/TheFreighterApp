@file:Suppress("DEPRECATION")

package com.androidstrike.schoolprojects.thefreighterapp.features.home.client

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavHost
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI.setupWithNavController
import androidx.navigation.ui.setupActionBarWithNavController
import com.androidstrike.schoolprojects.thefreighterapp.R
import com.androidstrike.schoolprojects.thefreighterapp.databinding.FragmentClientHomeBinding
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.auth

class ClientHome : Fragment() {

    private var _binding: FragmentClientHomeBinding? = null
    private val binding get() = _binding!!

    private val args: ClientHomeArgs by navArgs()

    private lateinit var role: String

    private lateinit var navController: NavController


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentClientHomeBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as AppCompatActivity).setSupportActionBar(binding.managerCustomToolBar)
        role = args.role

        val navHostFragment = childFragmentManager.findFragmentById(R.id.home_nav_host_fragment) as NavHost

        navController = navHostFragment.navController
        setupWithNavController(binding.navView,navController)

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_dispatch, R.id.navigation_wallet, R.id.navigation_profile
            )
        )
        (activity as AppCompatActivity).setupActionBarWithNavController(navController, appBarConfiguration)
    }


    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_logout, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.logout -> {
                auth.signOut()
                val navToStart = ClientHomeDirections.actionClientHomeToIntroScreen()
                findNavController().navigate(navToStart)
            }
        }
        return super.onOptionsItemSelected(item)
    }




    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}