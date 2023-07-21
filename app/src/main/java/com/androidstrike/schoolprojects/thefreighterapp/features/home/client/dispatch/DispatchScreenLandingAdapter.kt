package com.androidstrike.schoolprojects.thefreighterapp.features.home.client.dispatch

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.androidstrike.schoolprojects.thefreighterapp.features.home.client.dispatch.concluded.ConcludedDispatch
import com.androidstrike.schoolprojects.thefreighterapp.features.home.client.dispatch.pending.PendingDispatch

class DispatchScreenLandingAdapter (var context: FragmentActivity?,
                                    fm: FragmentManager,
                                    private var totalTabs: Int
) : FragmentPagerAdapter(fm) {
    override fun getCount(): Int {
        return totalTabs
    }

    val TAG = "ManagerOrdersLandingPagerAdapter"

    //when each tab is selected, define the fragment to be implemented
    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> {
                PendingDispatch()
            }
            1 -> {
                ConcludedDispatch()
            }
            else -> {
                getItem(position)

            }
        }
    }
}