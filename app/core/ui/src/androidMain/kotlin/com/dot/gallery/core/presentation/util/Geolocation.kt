package com.dot.gallery.core.presentation.util

import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.text.isDigitsOnly



@Composable
fun rememberGeocoder(): Geocoder? {
    val geocoder = Geocoder(LocalContext.current)
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && Geocoder.isPresent())
        geocoder else null
}
