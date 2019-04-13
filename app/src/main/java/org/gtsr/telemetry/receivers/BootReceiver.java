package org.gtsr.telemetry.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.gtsr.telemetry.TelemetryService;


public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            AlarmReceiver.registerAlarm(context);
            // Boot service
            TelemetryService.startService(context);
        }
    }
}
