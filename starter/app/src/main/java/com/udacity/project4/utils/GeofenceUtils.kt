package com.udacity.project4.utils

class GeofenceUtils {

    internal object GeofenceConstants {
        const val GEOFENCE_RADIUS_IN_METERS = 100f
        /**
         * Used to set an expiration time for a geofence. After this amount of time, Location services
         * stops tracking the geofence. For this sample, geofences expire after one hour.
         */
        val GEOFENCE_EXPIRATION_IN_MILLISECONDS: Long = java.util.concurrent.TimeUnit.HOURS.toMillis(1)
    }
}