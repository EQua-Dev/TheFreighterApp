package com.androidstrike.schoolprojects.thefreighterapp.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

object Common {


    const val USER_REF = "Freighter App Users"


    const val DATE_FORMAT = "dd MMM, yyyy"
    const val MIN_PASSWORD = 8

    val auth = FirebaseAuth.getInstance()
    val userCollectionRef = Firebase.firestore.collection(USER_REF)


}