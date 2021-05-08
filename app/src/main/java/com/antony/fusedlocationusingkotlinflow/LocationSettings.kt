package com.antony.fusedlocationusingkotlinflow

import com.google.android.gms.location.LocationRequest

val locationRequest = LocationRequest.create().apply {
    interval = 10000
    fastestInterval = 5000
    priority = LocationRequest.PRIORITY_HIGH_ACCURACY
}