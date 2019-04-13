package org.gtsr.telemetry.receivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.gtsr.telemetry.TelemetrySerial;
import org.gtsr.telemetry.TelemetryService;

import java.util.Calendar;

import static org.gtsr.telemetry.TelemetryService.TAG;

/*
    This is meant to fire periodically to ensure that the service is running
 */
public class AlarmReceiver extends BroadcastReceiver {

    public static void registerAlarm(Context context) {
        Log.d(TAG, "Registering alarm.");
        // Set alarms
        AlarmManager mgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent alarmIntent = new Intent(context, AlarmReceiver.class);
        alarmIntent.setAction("org.gtsr.telemetry.ALARM");

        PendingIntent alarmPendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, 0);

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        // Start at 00:00:00.000
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        // TODO: make interval a preference
        mgr.setInexactRepeating(AlarmManager.RTC, cal.getTimeInMillis(), 60 * 1000, alarmPendingIntent);
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("org.gtsr.telemetry.ALARM")) {
            Log.d(TAG, "Received alarm!");

            TelemetryService.startService(context);
            TelemetryService.getInstance().deviceAttemptConnection();
        }
    }
}
