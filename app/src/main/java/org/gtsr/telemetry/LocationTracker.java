package org.gtsr.telemetry;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.gtsr.telemetry.packet.CANPacket;

public class LocationTracker {

    private static final String TAG = LocationTracker.class.getName();

    private final LocationCallback locationCallback;

    private FusedLocationProviderClient client;
    private CANPublisher publisher;

    public LocationTracker(Context context, CANPublisher publisher) {
        this.publisher = publisher;
        client = LocationServices.getFusedLocationProviderClient(context);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                super.onLocationResult(result);
                if (result == null) {
                    Log.d(TAG, "null location result");
                    return;
                }
                Log.d(TAG, "Got location update");
                for (Location location : result.getLocations()) {
                    CANPacket packet = new CANPacket((short)0x621,
                            (float)location.getLatitude(), (float)location.getLongitude());
                    publisher.publishCANPacket(packet);
                }
            }
        };
    }

    public void startUpdates() {
        Log.d(TAG, "Starting location updates");
        try {
            client.getLastLocation().addOnSuccessListener(location -> {
                CANPacket packet = new CANPacket((short)0x621,
                        (float)location.getLatitude(), (float)location.getLongitude());
                publisher.publishCANPacket(packet);
            });
        } catch (SecurityException e) {
            Log.e(TAG, "shit");
        }
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(500);
        try {
            client.requestLocationUpdates(locationRequest, locationCallback, null);
        } catch (SecurityException e) {
            Log.e(TAG, "GPS permissions not provided!");
        }
    }

    public void stopUpdates() {
        Log.d(TAG, "Stopping location updates");
        client.removeLocationUpdates(locationCallback);
    }

}
