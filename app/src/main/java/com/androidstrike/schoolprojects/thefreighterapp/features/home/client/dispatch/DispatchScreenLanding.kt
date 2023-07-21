package com.androidstrike.schoolprojects.thefreighterapp.features.home.client.dispatch

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.findNavController
import com.androidstrike.schoolprojects.thefreighterapp.R
import com.androidstrike.schoolprojects.thefreighterapp.databinding.FragmentDispatchScreenLandingBinding
import com.androidstrike.schoolprojects.thefreighterapp.features.home.client.ClientHome
import com.androidstrike.schoolprojects.thefreighterapp.features.home.client.ClientHomeDirections
import com.androidstrike.schoolprojects.thefreighterapp.models.Dispatch
import com.androidstrike.schoolprojects.thefreighterapp.models.UserData
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.STATUS_AWAITING_WEIGHER
import com.androidstrike.schoolprojects.thefreighterapp.utils.Common.STATUS_PENDING_DRIVER
import com.androidstrike.schoolprojects.thefreighterapp.utils.hideProgress
import com.androidstrike.schoolprojects.thefreighterapp.utils.toast
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.hbb20.CountryCodePicker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DispatchScreenLanding : Fragment() {

    private var _binding: FragmentDispatchScreenLandingBinding? = null
    private val binding get() = _binding!!



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentDispatchScreenLandingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        with(binding) {
            //set the title to be displayed on each tab

            val tabTitles = resources.getStringArray(R.array.dispatch_tab_titles)
            for (title in tabTitles) {
                val tab = dispatchBaseTabLayout.newTab()
                tab.text = title
                dispatchBaseTabLayout.addTab(tab)
            }
            dispatchBaseTabLayout.tabGravity = TabLayout.GRAVITY_FILL

            val adapter = DispatchScreenLandingAdapter(
                activity,
                childFragmentManager,
                dispatchBaseTabLayout.tabCount
            )
            dispatchViewPager.adapter = adapter
            dispatchViewPager.addOnPageChangeListener(
                TabLayout.TabLayoutOnPageChangeListener(
                    dispatchBaseTabLayout
                )
            )

            //define the functionality of the tab layout
            dispatchBaseTabLayout.addOnTabSelectedListener(object :
                TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    dispatchViewPager.currentItem = tab.position
                    dispatchBaseTabLayout.setSelectedTabIndicatorColor(resources.getColor(R.color.primary))
                    dispatchBaseTabLayout.setTabTextColors(
                        Color.BLACK,
                        resources.getColor(R.color.primary)
                    )
                }

                override fun onTabUnselected(tab: TabLayout.Tab) {
                    dispatchBaseTabLayout.setTabTextColors(Color.WHITE, Color.BLACK)
                }

                override fun onTabReselected(tab: TabLayout.Tab) {}
            })

            fabAddDispatch.setOnClickListener {
                getUser()
            }

        }


    }

    private fun getUser(){
        var loggedUser = UserData()

        CoroutineScope(Dispatchers.IO).launch {
            Common.userCollectionRef.document(Common.auth.uid.toString())
//            Common.userCollectionRef.document("ElE9dfN1rXVaJ0MD8IsJv046BnV2")
                .addSnapshotListener { value, error ->
                    if (error != null) {
                        requireContext().toast(error.message.toString())
                        return@addSnapshotListener
                    }
                    if (value != null && value.exists()) {
                        loggedUser = value.toObject(UserData::class.java)!!
                        //binding.fabAddDispatch.setOnClickListener {
                            val navToNewDispatch = DispatchScreenLandingDirections.actionDispatchScreenLandingToCreateNewDispatch(loggedUser)
                            findNavController().navigate(navToNewDispatch)
                        //}


                    }
                }
//
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}