package com.udacity.project4.locationreminders.savereminder

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.geofence.GeofenceRepository
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.GeofenceUtils
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.util.jar.Manifest

class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    private lateinit var geofencingClient: GeofencingClient
    private val RUNNINGQORLATER = android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.Q

    companion object{
        private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 5
        private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 10
        private const val LOCATION_PERMISSION_INDEX = 1
        private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1
        private const val REQUEST_TURN_DEVICE_LOCATION_ON = 19
    }

    //set up geofence pending intent
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        //intent.action = ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel

        geofencingClient = LocationServices.getGeofencingClient(requireContext())

        return binding.root
    }

    //if user has not granted permission, ask again
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            checkDeviceLocationSettingsAndStartGeofence()
        }
    }
    /**
     * Permission is denied if:
     * - The grantResults array is empty
     * - The grantResults array’s value at the LOCATION_PERMISSION_INDEX has a PERMISSION_DENIED
     * - The request code equals REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE and the
     * BACKGROUND_LOCATION_PERMISSION_INDEX is denied.
     */
    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Timber.d("onRequestPermissionResult")

        if (
                grantResults.isEmpty() ||
                grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
                (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
                        grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] ==
                        PackageManager.PERMISSION_DENIED))
        {
            // Display a snackbar explaining that the user needs location permissions in order to
            // trigger the reminders
            Snackbar.make(
                    binding.root,
                    R.string.permission_denied_explanation,
                    Snackbar.LENGTH_INDEFINITE
            )
                    .setAction(R.string.settings) {
                        startActivity(Intent().apply {
                            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    }.show()
        } else
            checkDeviceLocationSettingsAndStartGeofence()
    }
    private fun checkDeviceLocationSettingsAndStartGeofence() {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder =
                LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireContext())
        val locationSettingsResponseTask = settingsClient.checkLocationSettings(builder.build())

        //check if location services is turned off to prevent infinite loop
        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    //prompt user to turn on location
                    exception.startResolutionForResult(
                            requireActivity(),
                            REQUEST_TURN_DEVICE_LOCATION_ON
                    )
                } catch (exception: IntentSender.SendIntentException) {
                    Timber.e("Error getting location settings:  ${exception.message}")
                }
            }else{
                // location alert snackbar
                Snackbar.make(binding.root,
                        R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettingsAndStartGeofence()
                }.show()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener { response ->
            if (response.isSuccessful) {
                createReminder().let { item ->
                _viewModel.validateAndSaveReminder(item)
                }
            }
        }
    }

    //set up geofence
    private fun buildGeofence(reminderDataItem: ReminderDataItem): Geofence? {
        return reminderDataItem.latitude?.let {latitude ->
            reminderDataItem.longitude?.let { longitude ->
                Geofence.Builder()
                        .setRequestId(reminderDataItem.id)
                        .setCircularRegion(
                                latitude,
                                longitude,
                                GeofenceUtils.GeofenceConstants.GEOFENCE_RADIUS_IN_METERS
                        )
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                        .setExpirationDuration(Geofence.NEVER_EXPIRE) // will exist until the user removes it.
                        .build()
            }
        }
    }

    //Create Reminder from gotten variables
    private fun createReminder(): ReminderDataItem {
        val title = _viewModel.reminderTitle.value
        val description = _viewModel.reminderDescription.value
        val location = _viewModel.reminderSelectedLocationStr.value
        val latitude = _viewModel.latitude.value
        val longitude = _viewModel.longitude.value

        return ReminderDataItem(title, description, location, latitude, longitude)

    }

    //add geofence
    private fun addGeofence(geofence: Geofence) {
        val geofencingRequest = GeofenceRepository.buildGeofenceRequest(geofence)

        //permission check
        if (ContextCompat.checkSelfPermission(requireContext(),
                        android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
                    .addOnSuccessListener { Timber.d("Geofence, id: ${geofence.requestId} added") }
                    .addOnFailureListener { e->
                        if (e.message != null) {
                            Timber.e("Geofence addition failed ${e.message}")
                        }
                    }
        }
    }

    //remove geofence
    private fun removeGeofences() {
        if (!foregroundAndBackgroundLocationPermissionApproved()) return

        geofencingClient.removeGeofences(geofencePendingIntent)?.run {
            addOnSuccessListener {
                Timber.d("Geofence removed successfully")
                Toast.makeText(requireContext(), R.string.geofences_removed, Toast.LENGTH_SHORT)
                        .show()
            }
            addOnFailureListener {
                Timber.d("Geofence could not be removed")
            }
        }
    }

    @TargetApi(29)
    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(requireContext(),
                        android.Manifest.permission.ACCESS_FINE_LOCATION)
                )
        val backgroundPermissionApproved =
                if (RUNNINGQORLATER) {
                    PackageManager.PERMISSION_GRANTED ==
                            ActivityCompat.checkSelfPermission(requireContext(),
                            android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                } else {
                    true
                }
        return foregroundLocationApproved && backgroundPermissionApproved
    }

    //Permissions for android versions >= 10
    @TargetApi(29)
    private fun requestForegroundAndBackgroundLocationPermission() {
        if (foregroundAndBackgroundLocationPermissionApproved()) {
            createReminder().let { reminder ->
                _viewModel.validateAndSaveReminder(reminder)
                buildGeofence(reminder)?.let {
                    addGeofence(it)
                }
            }
        }

        var permissionsArray = arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION)

        val resultCode = when {
            RUNNINGQORLATER -> {
                permissionsArray += android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }
            else -> REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }

        ActivityCompat.requestPermissions(
                requireActivity(),
                permissionsArray,
                resultCode
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location
//            findNavController(this).navigate(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
            _viewModel.navigationCommand.value =
                    NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {
            requestForegroundAndBackgroundLocationPermission()

            //entry point for geofence API interaction
            geofencingClient = LocationServices.getGeofencingClient(requireContext())


            _viewModel.eventHasSelectedLocation.observe(viewLifecycleOwner, Observer { hasLocation ->
                if (hasLocation) {
                    binding.selectLocation.visibility = View.GONE
                }
            })
        }
    }



//    override fun onResume() {
//        super.onResume()
//        if (_viewModel.hasLocationData.value!!) {
//            binding.selectLocation.visibility = View.GONE
//            _viewModel.doneGetLocationDataValue()
//        }
//    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }
}
