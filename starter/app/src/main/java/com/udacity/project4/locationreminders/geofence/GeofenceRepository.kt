package com.udacity.project4.locationreminders.geofence

import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest

object GeofenceRepository {

    fun buildGeofenceRequest (geofence: Geofence): GeofencingRequest {
        return GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()
    }
}