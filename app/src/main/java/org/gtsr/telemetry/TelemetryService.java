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

import org.gtsr.telemetry.bluetooth.BluetoothSerial;
import org.gtsr.telemetry.libs.Constants;
import org.gtsr.telemetry.packet.SerialPacket;
import org.gtsr.telemetry.packet.SerialPacketFactory;

import java.util.Arrays;

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

    private BluetoothSerial btHandler;
    public TelemetryService() {
        super(TelemetryService.class.getSimpleName());
        serial = new TelemetrySerial(this, BAUD_RATE, TelemetryService.this::receiveLine);
    }

    private void init() {
        if (broadcastReceiver == null) {
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if(intent.getAction().equals(Constants.INTENT_ACTION_GRANT_USB)) {
                        Boolean granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
                        serial.connect(granted);
                    }
                }
            };
            registerReceiver(broadcastReceiver, new IntentFilter(Constants.INTENT_ACTION_GRANT_USB));
        }

        serial.init();
        isRunning = true;

        btHandler = new BluetoothSerial(this, TelemetryService.this::receiveLine);
        btHandler.init();
    }

    /*
     * Lifecycle
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Starting service.");
        init();
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

        if (btHandler != null) {
            btHandler.close();
        }
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
        startForeground(100, createNotification("Connecting..."));

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

    private void updateNotification() {
        Notification notification = createNotification(msgNum + " messages received");

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(100, notification);
    }

    public void receiveLine(byte[] data, int length) {
        Log.d(TAG, "Packet: " + Arrays.toString(data));
        SerialPacket p = SerialPacketFactory.parsePacket(length, data);

        if (p != null) {
            msgNum++;
            if (msgNum % 20 == 0) {
                Log.d(TAG, "Got packet #" + msgNum + ": " + p.toString());
                updateNotification();
            }
            //Toast.makeText(TelemetryService.this, "Got packet!", Toast.LENGTH_SHORT).show();
            Intent broadIntent = new Intent();
            broadIntent.setAction(TELEM_PACKET_BROADCAST_ACTION);
            broadIntent.putExtra("data", p.toString());
            LocalBroadcastManager.getInstance(TelemetryService.this).sendBroadcast(broadIntent);
        }
    }

    public void deviceAttached() {
        if (serial != null) {
            serial.deviceAttached();
        } else {
            init();
        }
    }

    public void deviceAttemptConnection() {
        if (serial != null) {
            serial.connect();
        } else {
            init();
        }
    }

    public static void startService(Context c) {
        if (!TelemetryService.isRunning()) {
            Log.d(TAG, "Service not running. Starting telemetry service...");
            ContextCompat.startForegroundService(c, new Intent(c, TelemetryService.class));
        } else {
            Log.d(TAG, "Telemetry service already running!");
        }
    }

    public static void stopService() {
        if (telemService != null) {
            Log.d(TAG,"Stopping telemetry service.");
            telemService.stopSelf();
        }
    }

    public static TelemetryService getInstance() {
        return telemService;
    }

    public static boolean isRunning() {
        return isRunning;
    }

    public TelemetrySerial.Connected isSerialConnected() {
        return serial.isConnected();
    }
}