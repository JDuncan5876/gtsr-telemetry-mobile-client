package org.gtsr.telemetry;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import static org.gtsr.telemetry.TelemetryService.TAG;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("org.gtsr.telemetry.ALARM")) {
            Log.d(TAG, "Received alarm!");

            if (!TelemetryService.isRunning()) {
                Log.d(TAG, "Service not running. Starting telemetry service...");
                ContextCompat.startForegroundService(context, new Intent(context, TelemetryService.class));
            }
        }
    }
}
