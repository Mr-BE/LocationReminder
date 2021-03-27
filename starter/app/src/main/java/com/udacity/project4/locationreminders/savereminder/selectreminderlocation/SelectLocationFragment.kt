package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.navigation.fragment.NavHostFragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {
    private val PERMISSION_ID: Int = 19
    private val ZOOM: Float = 15f
    private val TAG = SelectLocationFragment::class.java.simpleName

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var saveButton: Button


    private val REQUEST_LOCATION_PERMISSION = 1

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
                DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        initMapFragment()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        //set up save button
        saveButton = binding.locationSaveButton
        saveButton.visibility = View.GONE


        //observe location selected changes
        _viewModel.eventHasSelectedLocation.observe(viewLifecycleOwner, Observer { hasSelectedLocation ->
            if (hasSelectedLocation) {
                saveButton.visibility = View.VISIBLE
                saveButton.setOnClickListener {
                    when (it.id) {
                        R.id.locationSaveButton -> onLocationSelected()
                    }
                }
            }
        })


        return binding.root
    }

    private fun initMapFragment() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap?) {

        if (googleMap != null) {
            map = googleMap
        }
        //add marker
        setMapClick(map)

        //show location
        enableMyLocation()
    }

    private fun setMapClick(map: GoogleMap?) {

        if (map != null) {
            map.setOnMapClickListener { latLng ->
                //add marker
                map.addMarker(MarkerOptions()
                        .position(latLng)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)))

                _viewModel.longitude.value = latLng.longitude
                _viewModel.latitude.value = latLng.latitude
                _viewModel.reminderSelectedLocationStr.value = "${latLng.longitude}, ${latLng.latitude}"
                _viewModel.locationSelected()
            }
        } else {
            Log.e(TAG, "Null Google Map value")
            Toast.makeText(requireContext(), "No Network Connection Available", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_ID) {
            if (grantResults.isNotEmpty() &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation()
            } else {
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
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        //empty LatLng var
        var latLng: LatLng

        //check location permission
        if (isPermissionGranted()) {
            map.isMyLocationEnabled = true

            //monitor location changes
            fusedLocationProviderClient.lastLocation
                    .addOnSuccessListener { location ->
                        location.let {
                            //assign got location details to LatLng var
                            latLng = LatLng(location.latitude, location.longitude)

                            //move map camera and set marker on gotten location
                            map.let {
                                it.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, ZOOM))
                                it.addMarker(MarkerOptions().position(latLng).title("Last Location"))
                            }
                            Toast.makeText(requireContext(), "Please select a location", Toast.LENGTH_LONG).show()
                        }
                    }
            getUserLastLocation()
        } else {
            ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_LOCATION_PERMISSION
            )
        }
    }

    private fun getUserLastLocation() {
        if (isLocationEnabled()) {
            //check android version compatibility
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                checkPermissionAndGetLocation()
        }
    }

    @SuppressLint("MissingPermission")
    private fun checkPermissionAndGetLocation() {
        var latLng: LatLng

        if (isForegroundPermissionApproved()) {
            requestPermissions()
        } else {
            //monitor location changes
            fusedLocationProviderClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        location?.let {
                            latLng = LatLng(location.latitude, location.longitude)

                            map.let {
                                it.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, ZOOM))
                                it.addMarker(MarkerOptions().position(latLng).title("Marker in user's last location"))
                            }
                            Toast.makeText(requireContext(), "Please select a location", Toast.LENGTH_LONG).show()

                        }
                    }
        }
    }

    private fun isForegroundPermissionApproved(): Boolean {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            map.isMyLocationEnabled = true
            return true
        }
        return false
    }

    private fun onLocationSelected() {
        //         When the user confirms on the selected location,
        //         send back the selected location details to the view model
        //         and navigate back to the previous fragment to save the reminder and add the geofence

        NavHostFragment.findNavController(this).popBackStack()
//        NavigationCommand.Back
        _viewModel.locationSelected()
    }

    private fun requestPermissions() {
        requestPermissions(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION),
                PERMISSION_ID)
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        //  Change the map type based on the user's selection.
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }


    //user permission granted
    private fun isPermissionGranted(): Boolean {
        return ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    //check user location enabled
    private fun isLocationEnabled(): Boolean {
        var locationEnabled = false
        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager

        locationManager?.let { manager ->
            locationEnabled = manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    || manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        }
        return locationEnabled
    }

}
