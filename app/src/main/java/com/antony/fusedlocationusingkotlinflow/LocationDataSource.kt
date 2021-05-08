package com.antony.fusedlocationusingkotlinflow

import android.annotation.SuppressLint
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber

class LocationDataSource(
    private val locationClient: FusedLocationProviderClient
) {
    @SuppressLint("MissingPermission")
    val locationsSource: Flow<Location> = callbackFlow<Location> {
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult?) {
                result ?: return
                try {
                    trySend(result.lastLocation)
                } catch (e: Exception) {
                    // Not Handled
                }
            }
        }
        Timber.e("Location request started")
        locationClient.requestLocationUpdates(
            locationRequest,
            callback,
            Looper.getMainLooper()
        ).addOnFailureListener { e ->
            close(e) // in case of exception, close the Flow
        }
        // clean up when Flow collection ends
        awaitClose {
            Timber.e("Location request removed")
            locationClient.removeLocationUpdates(callback)
        }
    }
}