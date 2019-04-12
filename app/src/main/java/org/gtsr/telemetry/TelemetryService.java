/*
    Modified from example code at https://github.com/felHR85/UsbSerial
 */
package org.gtsr.telemetry;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;

import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.gtsr.telemetry.libs.Constants;
import org.gtsr.telemetry.packet.CANPacket;
import org.gtsr.telemetry.packet.CANPacketFactory;

public class TelemetryService extends IntentService {
    private static final String TELEM_PACKET_BROADCAST_ACTION =
            "org.gtsr.telemetry.BROADCAST_PACKET";

    public static final String TAG = "GTSRTelemetryService";
    private static final int BAUD_RATE = 256000;        // TODO: make this a preference

    private BroadcastReceiver broadcastReceiver;

    private TelemetrySerial serial;

    private int msgNum = 0;

    private static boolean isRunning = false;

    private static TelemetryService telemService = null;

    public TelemetryService() {
        super(TelemetryService.class.getSimpleName());
        serial = new TelemetrySerial(this, BAUD_RATE, TelemetryService.this::receiveLine);
    }
    /*
     * Lifecycle
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Starting service.");
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().equals(Constants.INTENT_ACTION_GRANT_USB)) {
                    Boolean granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
                    serial.connect(granted);
                }
            }
        };
        serial.init();
        registerReceiver(broadcastReceiver, new IntentFilter(Constants.INTENT_ACTION_GRANT_USB));
        isRunning = true;

        try {
            if (telemService == null) {
                telemService = this;
            } else {
                throw new Exception("Telemetry service already created!");
            }
        } catch(Exception e) {
            Log.e(TAG, "Error in telemetry service onCreate()!");
            Log.e(TAG, e.getLocalizedMessage());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Stopping service.");
        serial.cleanup();
        unregisterReceiver(broadcastReceiver);
        isRunning = false;
        telemService = null;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if(intent.getAction().equals("android.hardware.usb.action.USB_DEVICE_ATTACHED")){
            serial.deviceAttached();
        }
    }

    /* MUST READ about services
     * http://developer.android.com/guide/components/services.html
     * http://developer.android.com/guide/components/bound-services.html
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        prepareNotification();
        startForeground(1, createNotification("Connecting..."));

        return Service.START_STICKY;
    }

    private Notification createNotification(String subtitle) {
        return new NotificationCompat.Builder(TelemetryService.this, getString(R.string.app_name))
                .setContentTitle("GTSR Telemetry")
                .setContentText(subtitle)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build();
    }

    private void prepareNotification() {
        // Have to add channel to notification manager
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = getString(R.string.app_name);

        NotificationChannel notificationChannel = new NotificationChannel(channelId, channelId, NotificationManager.IMPORTANCE_DEFAULT);
        notificationChannel.setDescription(channelId);
        notificationChannel.setSound(null, null);
        manager.createNotificationChannel(notificationChannel);
    }

    private void updateNotification(String text) {
        Notification notification = createNotification(text);

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(1, notification);
    }

    public void receiveLine(byte[] data, int length) {
        CANPacket p = CANPacketFactory.parsePacket(length, data);

        if (p != null) {
            msgNum++;
            if (msgNum % 1000 == 0) {
                Log.d(TAG, "Got packet #" + msgNum + ": " + p.toString());
                updateNotification("Message: "+msgNum);
            }
            //Toast.makeText(TelemetryService.this, "Got packet!", Toast.LENGTH_SHORT).show();
            Intent broadIntent = new Intent();
            broadIntent.setAction(TELEM_PACKET_BROADCAST_ACTION);
            broadIntent.putExtra("data", p.toString());
            LocalBroadcastManager.getInstance(TelemetryService.this).sendBroadcast(broadIntent);
        }
    }

    public static void startService(Context c) {
        if (!TelemetryService.isRunning()) {
            Log.d(TAG, "Service not running. Starting telemetry service...");
            ContextCompat.startForegroundService(c, new Intent(c, TelemetryService.class));
        }
    }

    public static boolean isRunning() {
        return isRunning;
    }
}