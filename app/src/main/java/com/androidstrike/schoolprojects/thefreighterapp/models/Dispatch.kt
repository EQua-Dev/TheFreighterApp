package com.androidstrike.schoolprojects.thefreighterapp.models

data class Dispatch(
    var status: String = "",
    val packageType: String = "",
    val weight: String = "",
    val amount: String = "",
    val client: String = "",
    val driver: String = "",
    var weigher: String = "",
    var weighingDate: String = "",
    val lastUpdater: String = "",
    val pickupAddress: String = "",
    val pickupProvince: String = "",
    val pickupCountry: String = "",
    val pickupDate: String = "",
    val dropOffAddress: String = "",
    val dropOffProvince: String = "",
    val dropOffCountry: String = "",
    val transitLocation: String = "",
    val pickerName: String = "",
    val pickerNumber: String = "",
    val negotiationRound: String = "0",
    val negotiationPrice1: String = "",
    val negotiationPrice2: String = "",
    val negotiationPrice3: String = "",
    val dateCreated: String = "",
    val dateWeighed: String = "",
    val datePickedUp: String = "",
    val dateDelivered: String = "",
    val dispatchId: String = ""

)
