package com.androidstrike.schoolprojects.thefreighterapp.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class UserData(
    val userId: String = "",
    val fullName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val dateOfBirth: String = "",
    val countryOfResidence: String = "",
    val dateJoined: String = "",
    val role: String = "",
    val vehiclePlateNumber: String = "",
    val driverLicenseNumber: String = "",
    val coverageLocation1: String = "",
    val coverageLocation2: String = "",
    val driverContactPersonName: String = "",
    val driverContactPersonPhoneNumber: String = "",
    val driverContactPersonAddress: String = "",
    val driverContactPersonCity: String = "",
    val driverContactPersonCountry: String = "",
    val weigherCost: String = ""
): Parcelable
