package com.antony.fusedlocationusingkotlinflow

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class LocationViewModel(
    fusedLocation: FusedLocationProviderClient
) : ViewModel() {

    private val locationDataSource by lazy {
        LocationDataSource(
            fusedLocation
        )
    }

    val locations: Flow<Location?> =
        locationDataSource.locationsSource.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            null
        )
}

class LocationViewModelFactory(private val fusedLocation: FusedLocationProviderClient) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LocationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LocationViewModel(fusedLocation) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
