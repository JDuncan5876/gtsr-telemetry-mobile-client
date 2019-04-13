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
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;

        if (isCharging) {
            Log.d(TAG, "Received power! Starting service...");
            TelemetryService.startService(context);
            TelemetryService.getInstance().deviceAttemptConnection();
        }
    }
}
