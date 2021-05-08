package com.antony.fusedlocationusingkotlinflow

import android.Manifest
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.antony.fusedlocationusingkotlinflow.databinding.ActivityMainBinding
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private var mapFragment: SupportMapFragment? = null
    private var gMap: GoogleMap? = null
    private var locationJob: Job? = null
    private val viewModel by viewModels<LocationViewModel>() {
        LocationViewModelFactory(
            LocationServices
                .getFusedLocationProviderClient(applicationContext)
        )
    }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        setUpMap()
        checkLocationPermission()
    }

    private fun setUpMap() {
        mapFragment = supportFragmentManager.findFragmentById(
            R.id.map
        ) as? SupportMapFragment
        mapFragment?.getMapAsync { googleMap ->
            gMap = googleMap
        }
    }

    private fun checkLocationPermission() {
        locationPermission.launch(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    private fun startLocationUpdates() {
        locationJob?.cancel()
        locationJob = lifecycleScope.launchWhenStarted {
            viewModel.locations
                .filterNotNull()
                .distinctUntilChanged()
                .collect { location ->
                    Timber.e("Latitude : ${location.latitude}, Longitude : ${location.longitude}")
                    gMap?.clear()
                    val latLng = LatLng(
                        location.latitude,
                        location.longitude
                    )
                    gMap?.addMarker(
                        MarkerOptions()
                            .title("You are here")
                            .position(latLng)
                    )
                    gMap?.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(latLng, 14f)
                    )
                }
        }
    }

    override fun onPause() {
        super.onPause()
        locationJob?.cancel()
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startLocationUpdates()
        }
    }

    private val locationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                // Permission granted
                checkLocationSettings()
            } else {
                // Permission denied
                Snackbar.make(
                    binding.root,
                    "Please allow location permission",
                    Snackbar.LENGTH_SHORT
                ).also { snackBar ->
                    snackBar.setAction("Allow") {
                        checkLocationPermission()
                    }
                    snackBar.show()
                }
            }
        }

    private val locationSettingsRequest =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
            if (it.resultCode == RESULT_OK) {
                startLocationUpdates()
            } else {
                Snackbar.make(
                    binding.root,
                    "Please turn on location service",
                    Snackbar.LENGTH_SHORT
                ).also { snackBar ->
                    snackBar.setAction("Turn On") {
                        checkLocationSettings()
                    }
                    snackBar.show()
                }
            }
        }

    private fun checkLocationSettings() {
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())
        task.addOnSuccessListener {
            // All location settings are satisfied. The client can initialize
            // location requests here.
            // ...
            startLocationUpdates()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    locationSettingsRequest.launch(
                        IntentSenderRequest.Builder(
                            exception.resolution.intentSender
                        ).build()
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }
}