package com.androidstrike.schoolprojects.thefreighterapp.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

object Common {


    const val USER_REF = "Freighter App Users"
    const val DISPATCH_REF = "Freighter App Dispatches"


    const val DATE_FORMAT = "EEEE, dd MMM, yyyy"
    const val DATE_FORMAT_SHORT = "EEE, dd MMM, yyyy"
    const val NOT_AVAILABLE = "N/A"
    const val MIN_PASSWORD = 8

    const val STATUS_DRAFT = "draft" //the client has not completed the dispatch request
    const val STATUS_PENDING_WEIGHER = "pending weigher" //waiting for the weighers to indicate interest
    const val STATUS_AWAITING_WEIGHER = "awaiting weigher" //waiting for the weigher to weigh and update the dispatch weight
    const val STATUS_PENDING_DRIVER = "pending driver" //waiting for drivers to indicate interest in the dispatch
    const val STATUS_NEGOTIATING_PRICE = "negotiating price" //driver and client are negotiating price
    const val STATUS_AWAITING_DRIVER = "awaiting driver" //waiting for driver to pickup package
    const val STATUS_IN_TRANSIT = "in transit" //driver is on his way to deliver the package
    const val STATUS_DELIVERED = "delivered" //driver has delivered the package and has been confirmed by the client
    const val STATUS_RATED = "rated" //client has rated the service
    const val STATUS_CANCELLED = "rated"

    val auth = FirebaseAuth.getInstance()
    val userCollectionRef = Firebase.firestore.collection(USER_REF)
    val dispatchCollectionRef = Firebase.firestore.collection(DISPATCH_REF)

    //N1 => user suggests different price from the initial driver price
    //N2 => driver counters the user price with another
    //N2 => user counters the driver updated price (if driver rejects, deal is off)


}