package com.agenticbank.utils

import android.content.Context
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

fun Double.toFormattedCurrency(currency: String = "LKR"): String {
    return try {
        val nf = NumberFormat.getNumberInstance(Locale.US)
        nf.minimumFractionDigits = 2
        nf.maximumFractionDigits = 2
        "$currency ${nf.format(this)}"
    } catch (e: Exception) {
        "$currency $this"
    }
}

fun String.toFormattedDate(): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val date = inputFormat.parse(this)
        outputFormat.format(date ?: return this)
    } catch (e: Exception) {
        this.take(10) // just return date part
    }
}

fun String.toShortDate(): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
        val date = inputFormat.parse(this)
        outputFormat.format(date ?: return this)
    } catch (e: Exception) {
        this.take(10)
    }
}

fun String.maskCardNumber(): String {
    return if (length >= 4) "**** **** **** ${takeLast(4)}" else this
}

fun View.showSnackbar(message: String, duration: Int = Snackbar.LENGTH_SHORT) {
    Snackbar.make(this, message, duration).show()
}

fun Context.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun View.visible() { visibility = View.VISIBLE }
fun View.gone() { visibility = View.GONE }
fun View.invisible() { visibility = View.INVISIBLE }
