package com.androidstrike.schoolprojects.thefreighterapp.models

data class Dispatch(
    var status: String = "",
    val packageType: String = "",
    val weight: String = "",
    val amount: String = "",
    val client: String = "",
    val driver: String = "",
    var weigher: String = "",
    val statusChangeTime: String = "",
    var weighingDate: String = "",
    val lastUpdater: String = "",
    val pickupAddress: String = "",
    val pickupProvince: String = "",
    val pickupCountry: String = "",
    val pickupCountryCode: String = "",
    val pickupDate: String = "",
    val dropOffAddress: String = "",
    val dropOffProvince: String = "",
    val dropOffCountry: String = "",
    val dropOffCountryCode: String = "",
    val transitLocation: String = "",
    val pickerName: String = "",
    val pickerNumber: String = "",
    var interestedDrivers: Map<String, String> = mapOf(),
    val negotiationRound: String = "",
    val negotiationPrice0: String = "",
    val negotiationPrice1: String = "",
    val negotiationPrice2: String = "",
    val negotiationPrice3: String = "",
    val dateCreated: String = "",
    val dateWeighed: String = "",
    val datePickedUp: String = "",
    val dateDelivered: String = "",
    val dispatchId: String = ""
)
