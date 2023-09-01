/*
 * Copyright (c) 2022.
 * Richard Uzor
 * For School Project
 *
 */

package com.androidstrike.schoolprojects.thefreighterapp.utils

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.Toast
import androidx.core.graphics.drawable.toDrawable
import com.google.android.material.snackbar.Snackbar
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import android.telephony.PhoneNumberUtils
import com.androidstrike.schoolprojects.thefreighterapp.R
import java.security.MessageDigest


/**
 * this is a file that contains definitions of various ui operations that could be used in multiple places
 * the functions in here define the operations and allow simplified form passing only the required parameter
 * Created by Richard Uzor  on 15/12/2022
 */


//toast function
fun Context.toast(message: String) =
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

var progressDialog: Dialog? = null

fun Context.showProgress() {
    hideProgress()
    progressDialog = this.showProgressDialog()
}

fun hideProgress() {
    progressDialog?.let { if (it.isShowing) it.cancel() }
}


//snack bar function
fun View.snackbar(message: String, action: (() -> Unit)? = null) {
    val snackbar = Snackbar.make(this, message, Snackbar.LENGTH_LONG)
    action?.let {
        snackbar.setAction("Retry") {
            it()
        }
    }
    snackbar.show()
}
fun View.doneSnackBar(message: String, action: (() -> Unit)? = null) {
    val snackBar = Snackbar.make(this, message, Snackbar.LENGTH_LONG)
    action?.let {
        snackBar.setAction("Done") {
            it()
        }
    }
    snackBar.show()
}


//common function to handle progress bar visibility
fun View.visible(isVisible: Boolean) {
    visibility = if (isVisible) View.VISIBLE else View.GONE
}


//common function to handle all intent activity launches
fun <A : Activity> Activity.startNewActivity(activity: Class<A>) {
    Intent(this, activity).also {
        it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(it)
    }
}

//common function to handle enabling the views (buttons)
fun View.enable(enabled: Boolean) {
    isEnabled = enabled
    isClickable = enabled
    alpha = if (enabled) 1f else 0.5f
}

fun Context.showProgressDialog(): Dialog {
    val progressDialog = Dialog(this)

    progressDialog.let {
        it.show()
        it.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        it.setContentView(R.layout.loading_progress_dialog)
        it.setCancelable(false)
        it.setCanceledOnTouchOutside(false)
        return it
    }
}


fun formatPhoneNumber(phoneNumber: String): String {
    // Remove any non-digit characters from the phone number
    //val digitsOnly = PhoneNumberUtils.digitsOnly(phoneNumber)

    // Format the phone number into E.164 format
    val formattedNumber = PhoneNumberUtils.formatNumberToE164(phoneNumber, "NG")

    return formattedNumber ?: phoneNumber
}

//function to change milliseconds to date format
fun getDate(milliSeconds: Long?, dateFormat: String?): String {
    // Create a DateFormatter object for displaying date in specified format.
    val formatter = SimpleDateFormat(dateFormat)

    // Create a calendar object that will convert the date and time value in milliseconds to date.
    val calendar: Calendar? = Calendar.getInstance()
    calendar?.timeInMillis = milliSeconds!!
    return formatter.format(calendar?.time!!)
}

fun convertISODateToMonthAndYear(isoDate: String?): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    dateFormat.timeZone = TimeZone.getTimeZone("UTC")
    val date = dateFormat.parse(isoDate!!)
    val monthAndYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    return monthAndYearFormat.format(date!!)
}

fun convertISODateToMonthYearAndTime(isoDate: String?): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    dateFormat.timeZone = TimeZone.getTimeZone("UTC")
    val date = dateFormat.parse(isoDate!!)
    val monthAndYearFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
    val hourAndMinuteFormat = SimpleDateFormat("HH:mm a", Locale.getDefault())
    val monthDate = monthAndYearFormat.format(date!!)
    val hourDate = hourAndMinuteFormat.format(date)
    return "$monthDate by $hourDate"
}

fun convertTimeStringToMilliseconds(timeString: String): Long {
    val parts = timeString.split(", ") // Split the string into parts: ["14 minutes", "0 seconds"]
    val minutesString = parts[0].replace(" minutes", "") // Extract the minutes part: "14"
    val secondsString = parts[1].replace(" seconds", "") // Extract the seconds part: "0"

    val minutes = minutesString.toLong() // Convert minutes to a long value
    val seconds = secondsString.toLong() // Convert seconds to a long value

    // Calculate the total duration in milliseconds
    val totalMilliseconds = (minutes * 60 + seconds) * 1000

    return totalMilliseconds
}
//
//fun convertMillisecondsToMinutes(timeInMillis: Long): String {
//    return "${timeInMillis / 60000} ${R.string.minutes}"
//}


fun convertMillisToISODate(milliseconds: Long?){
    val date = Date(milliseconds!!)
    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    dateFormat.timeZone = TimeZone.getTimeZone("UTC")
    val iso8601 = dateFormat.format(date)

}

fun isUpToTenMinutes(specificTimeMillis: Long): Boolean {
    val currentTimeMillis = System.currentTimeMillis()
    val tenMinutesInMillis = 2 * 60 * 1000 // 10 minutes in milliseconds

    return currentTimeMillis - specificTimeMillis <= tenMinutesInMillis
}


fun calculateAverage(numbers: List<Int>): Double {
    val sum = numbers.sum()
    val count = numbers.size
    return if (count > 0) {
        sum.toDouble() / count
    } else {
        0.0 // or you can handle the case when the list is empty
    }
}

fun formatTime(minutes: Double): String {
    val totalSeconds = (minutes * 60).toInt()
    val formattedMinutes = totalSeconds / 60
    val formattedSeconds = totalSeconds % 60

    //return String.format("%d minutes, %d seconds", formattedMinutes, formattedSeconds)
    return String.format("%d minutes", formattedMinutes)
}

fun formatPrice(price: Double): String {
    val nigeriaLocale = Locale("en", "NG") // Use Nigeria locale
    val currencyFormat = NumberFormat.getCurrencyInstance(nigeriaLocale)
    currencyFormat.currency = Currency.getInstance("NGN") // Set currency to NGN (Naira)

    return currencyFormat.format(price)
}



fun getMonthFromISODate(isoDate: String?): Int{
    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
    val date = dateFormat.parse(isoDate!!)
    val calendar = Calendar.getInstance()
    calendar.time = date!!
    return calendar.get(Calendar.MONTH)
}

fun convertISODateToMillis(isoDate: String): Long {
    val dateFormat = SimpleDateFormat("dd MMMM, yyyy")
    dateFormat.timeZone = TimeZone.getTimeZone("UTC")
    val date = dateFormat.parse(isoDate)
    return date!!.time

}

fun isTimeWithin24hours(givenTimeInMillis: Long): Boolean{
    // Get the current time in milliseconds

// Get the time at the start of the day (midnight)
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val startOfDayMillis = calendar.timeInMillis

// Get the time at the end of the day (23:59:59.999)
    calendar.apply {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }
    val endOfDayMillis = calendar.timeInMillis

// Check if the given time falls within the range of the current day's time
    val isWithinDayRange = givenTimeInMillis in startOfDayMillis..endOfDayMillis

    return isWithinDayRange
}

fun isAbove18(dateOfBirth: Date): Boolean {
    val calendar = Calendar.getInstance()
    val today = calendar.time

    calendar.add(Calendar.YEAR, -18)
    val minimumAgeDate = calendar.time

    return dateOfBirth.before(minimumAgeDate)
}

fun getDateFromString(dateString: String, format: String): Date? {
    val dateFormat = SimpleDateFormat(format, Locale.getDefault())
    return try {
        dateFormat.parse(dateString)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun isInvalidDate(dateString: String, format: String): Boolean {
    val dateFormat = SimpleDateFormat(format, Locale.getDefault())
    dateFormat.isLenient = false

    return try {
        dateFormat.parse(dateString)
        false // Parsing successful, valid date
    } catch (e: Exception) {
        true // Parsing failed, invalid date
    }
}



fun isPasswordValid(password: String): Boolean {
    val pattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$".toRegex()
    return !pattern.matches(password)
}
fun hashString(input: String): String {
    val messageDigest = MessageDigest.getInstance("MD5")
    val bytes = messageDigest.digest(input.toByteArray())
    val stringBuilder = StringBuilder()

    for (byte in bytes) {
        // Convert each byte to a hex string
        stringBuilder.append(String.format("%02x", byte))
    }

    return stringBuilder.toString()
}

