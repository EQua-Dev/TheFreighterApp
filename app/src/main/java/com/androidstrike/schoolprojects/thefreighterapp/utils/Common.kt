package com.androidstrike.schoolprojects.thefreighterapp.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

object Common {


    const val USER_REF = "Freighter App Users"
    const val DISPATCH_REF = "Freighter App Dispatches"


    const val DATE_FORMAT = "dd MMM, yyyy"
    const val MIN_PASSWORD = 8

    const val STATUS_DRAFT = "draft"
    const val STATUS_PENDING_WEIGHER = "pending weigher"
    const val STATUS_AWAITING_WEIGHER = "awaiting weigher"
    const val STATUS_PENDING_DRIVER = "pending driver"
    const val STATUS_NEGOTIATING_PRICE = "negotiating price"
    const val STATUS_AWAITING_DRIVER = "awaiting driver"
    const val STATUS_IN_TRANSIT = "in transit"
    const val STATUS_DELIVERED = "delivered"
    const val STATUS_RATED = "rated"
    const val STATUS_CANCELLED = "rated"

    val auth = FirebaseAuth.getInstance()
    val userCollectionRef = Firebase.firestore.collection(USER_REF)
    val dispatchCollectionRef = Firebase.firestore.collection(DISPATCH_REF)


}