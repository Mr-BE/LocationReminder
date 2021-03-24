package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
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


//        TODO: zoom to the user location after taking his permission
//        TODO: add style to the map
//        TODO: put a marker to location that the user selected


//        TODO: call this function after the user confirms on the selected location
        onLocationSelected()

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

//        val latitude = 4.986773547640699
//        val longitude = 8.348942111167954
//        val zoomLevel = 15f
//
//        val drillLatLng = LatLng(latitude, longitude)
//
//        map.moveCamera(CameraUpdateFactory.newLatLngZoom(drillLatLng, zoomLevel))

        //show location
        enableMyLocation()
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
        }else{
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
        }else{
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
        //        TODO: When the user confirms on the selected location,
        //         send back the selected location details to the view model
        //         and navigate back to the previous fragment to save the reminder and add the geofence
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
