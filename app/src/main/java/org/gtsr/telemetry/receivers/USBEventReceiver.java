package org.gtsr.telemetry.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.util.Log;

import org.gtsr.telemetry.TelemetryService;

public class USBEventReceiver extends BroadcastReceiver {
    private final String TAG = "GTSRTelemetryService.USBEventReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()) {
            case UsbManager.ACTION_USB_DEVICE_ATTACHED:
                Log.d(TAG, "USB device attached, starting telemetry service.");
                TelemetryService.startService(context);
                TelemetryService.getInstance().deviceAttached();
                break;
            case UsbManager.ACTION_USB_DEVICE_DETACHED:
                Log.d(TAG, "USB device detached, stopping telemetry service...");
                TelemetryService.stopService();
        }
    }
}
