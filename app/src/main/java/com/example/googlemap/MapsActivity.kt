package com.example.googlemap

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Point
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.googlemap.databinding.ActivityMapsBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.model.DirectionsResult
import com.google.maps.model.TravelMode


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapsBinding

    private lateinit var mapView: MapView
    private lateinit var googleMap: GoogleMap
    private var clickedMarker: Marker? = null
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private var lastKnownLocation: Location? = null
    private val defaultLocation = LatLng(-33.8523341, 151.2106085)
    private val DEFAULT_ZOOM = 15

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync(this)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @SuppressLint("MissingPermission")
    override fun onMapReady(gMap: GoogleMap) {
        this.googleMap = gMap
        googleMap.isMyLocationEnabled = true
        googleMap.uiSettings.isMyLocationButtonEnabled = true
        googleMap.setOnMapClickListener { marker ->
            if(clickedMarker == null) {
                clickedMarker = googleMap.addMarker(
                    MarkerOptions().position(
                        marker
                    )
                )!!

                // Move the camera to the starting point
                val startPoint =  LatLng(lastKnownLocation!!.latitude,lastKnownLocation!!.longitude) //LatLng(22.3124946,73.1481308)
                googleMap.moveCamera(
                    CameraUpdateFactory
                        .newLatLng(startPoint)
                )

                // Calculate and draw the route
                calculateAndDrawRoute(startPoint, marker)
                val zoomValue = calculateZoomLevel(startPoint, marker)
                googleMap.animateCamera(zoomValue)
            } else{
                clickedMarker?.remove()
                googleMap.moveCamera(CameraUpdateFactory.newLatLng(LatLng(lastKnownLocation!!.latitude,lastKnownLocation!!.longitude)))
                clickedMarker = null
            }
        }
        getDeviceLocation()
        // Move the camera to the starting point
        //val startPoint =  LatLng(lastKnownLocation!!.latitude,lastKnownLocation!!.longitude) //LatLng(22.3124946,73.1481308)
        //googleMap.moveCamera(CameraUpdateFactory.newLatLng(startPoint))

        // Calculate and draw the route
        //calculateAndDrawRoutecalculateAndDrawRoute(startPoint, LatLng(22.3032121,73.1722827))

    }

    private fun calculateZoomLevel(origin: LatLng, destination: LatLng, padding: Int = 100): CameraUpdate {
        val builder = LatLngBounds.Builder()
        builder.include(origin)
        builder.include(destination)

        val bounds = builder.build()
        val width = resources.displayMetrics.widthPixels
        val height = resources.displayMetrics.heightPixels
        val paddingInPixels = padding.coerceAtLeast(0)

        //val cameraUpdate: CameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, width, height, paddingInPixels)
        //val zoom = googleMap.cameraPosition.zoom // Get the current zoom level

        //return zoom
        return CameraUpdateFactory.newLatLngBounds(bounds, width, height, paddingInPixels)
    }

    @SuppressLint("MissingPermission")
    private fun getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            //if (locationPermissionGranted) {
                val locationResult = fusedLocationProviderClient?.lastLocation
                locationResult?.addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Set the map's camera position to the current location of the device.
                        lastKnownLocation = task.result
                        if (lastKnownLocation != null) {
                            googleMap.addMarker(
                                MarkerOptions().position(
                                    LatLng(lastKnownLocation!!.latitude,
                                        lastKnownLocation!!.longitude)
                                )
                            )!!
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                LatLng(lastKnownLocation!!.latitude,
                                    lastKnownLocation!!.longitude), DEFAULT_ZOOM.toFloat()))
                        }
                    } else {
                        //Log.d(TAG, "Current location is null. Using defaults.")
                        //Log.e(TAG, "Exception: %s", task.exception)
                        googleMap.moveCamera(CameraUpdateFactory
                            .newLatLngZoom(defaultLocation, DEFAULT_ZOOM.toFloat()))
                        googleMap.uiSettings.isMyLocationButtonEnabled = false
                    }
                }
            //}
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    private fun calculateAndDrawRoute(origin: LatLng, destination: LatLng) {

        val apiKey = getString(R.string.MAPS_API_KEY)
        val geoApiContext = GeoApiContext.Builder().apiKey(apiKey).build()

        // Request directions from Google Directions API
        val directionsResult: DirectionsResult = DirectionsApi.newRequest(geoApiContext)
            .mode(TravelMode.DRIVING)  // You can change the travel mode as needed
            .origin(com.google.maps.model.LatLng(origin.latitude, origin.longitude))
            .destination(com.google.maps.model.LatLng(destination.latitude, destination.longitude))
            .await()

        // Extract and draw the route on the map
        val route = directionsResult.routes[0].overviewPolyline.decodePath()
        val polylineOptions = PolylineOptions()
        polylineOptions.color(Color.RED)

        for (point in route) {
            polylineOptions.add(LatLng(point.lat, point.lng))
        }

        googleMap.addPolyline(polylineOptions)
        //addMarkersToMap(directionsResult)
        //googleMap.addMarker(MarkerOptions().position(origin).title("Origin"))
        //googleMap.addMarker(MarkerOptions().position(destination).title("Destination"))

        val latLngBounds = LatLngBounds.builder()
            .include(origin)
            .include(destination)
            .build()

        val points = Point()
        windowManager.defaultDisplay.getSize(points)
        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, points.x, 150, 30))
    }

    private fun addMarkersToMap(results: DirectionsResult) {
        googleMap.addMarker(
            MarkerOptions().position(
                LatLng(
                    results.routes[0].legs[0].startLocation.lat,
                    results.routes[0].legs[0].startLocation.lng
                )
            ).title(
                results.routes[0].legs[0].startAddress
            )
        )
        googleMap.addMarker(
            MarkerOptions().position(
                LatLng(
                    results.routes[0].legs[0].endLocation.lat,
                    results.routes[0].legs[0].endLocation.lng
                )
            ).title(
                results.routes[0].legs[0].startAddress
            ).snippet(getEndLocationTitle(results))
        )
    }

    private fun getEndLocationTitle(results: DirectionsResult): String? {
        return "Time :" + results.routes[0].legs[0].duration.humanReadable + " Distance :" + results.routes[0].legs[0].distance.humanReadable
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }
}