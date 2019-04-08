package org.gtsr.telemetry.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.gtsr.telemetry.TelemetryService;

import static org.gtsr.telemetry.TelemetryService.TAG;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("org.gtsr.telemetry.ALARM")) {
            Log.d(TAG, "Received alarm!");

            TelemetryService.startService(context);
        }
    }
}
