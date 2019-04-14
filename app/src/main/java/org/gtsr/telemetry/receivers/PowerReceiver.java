package org.gtsr.telemetry.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.util.Log;

import org.gtsr.telemetry.TelemetryService;

import static org.gtsr.telemetry.TelemetryService.TAG;

public class PowerReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_POWER_CONNECTED)) {
            Log.d(TAG, "Received power! Starting service...");
            TelemetryService.startService(context);
            TelemetryService.getInstance().deviceAttemptConnection();
        } else if (intent.getAction().equals(Intent.ACTION_POWER_DISCONNECTED)) {
            Log.d(TAG, "Power lost. Stopping service...");
            TelemetryService.stopService();
        }
    }
}
