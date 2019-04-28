package org.gtsr.telemetry;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import org.gtsr.telemetry.packet.CANPacket;
import org.gtsr.telemetry.packet.CANPacketFactory;
import org.gtsr.telemetry.packet.PacketReceiver;
import org.gtsr.telemetry.sensors.AccelerationMonitor;
import org.gtsr.telemetry.sensors.LocationTracker;
import org.gtsr.telemetry.serial.TelemetrySerial;

import java.io.IOException;

public class TelemetryService extends IntentService {
    public static final String TAG = "GTSRTelemetryService";
    private static final int BAUD_RATE = 256000;
    private int msgNum = 0;

    private static boolean isRunning = false;

    private static TelemetryService telemService = null;
    private static TelemetryServer server = null;

    private CANPublisher publisher;
    private TelemetrySerial serial;
    private LocationTracker tracker;
    private AccelerationMonitor accelerometer;
    private DiskLogger logger;
    public TelemetryService() {
        super(TelemetryService.class.getSimpleName());
    }

    private void init() {
        publisher = new CANPublisher();
        isRunning = true;

        serial = new TelemetrySerial(this, BAUD_RATE, this::receivePacket);
        serial.init();

        PacketReceiver receiver = new PacketReceiver(bytes -> {
            for (int i = 0; i < bytes.length; i += 7) {
                int numBytes = Math.min(7, bytes.length - i);
                byte[] canMessage = new byte[1 + numBytes];
                canMessage[0] = (byte)i;
                System.arraycopy(bytes, i, canMessage, 1, numBytes);
                CANPacket packet = new CANPacket((short)0x704, (short)canMessage.length, canMessage);
                serial.send(packet.marshalSerial());
            }
        }, new Byte[]{'G', 'T', 'S', 'R'});
        server = new TelemetryServer(receiver::receiveByte);
        publisher.registerReceiveCallback(packet -> server.write(packet.marshalTCP()));

        tracker = new LocationTracker(this, publisher, serial);
        tracker.startUpdates();

        accelerometer = new AccelerationMonitor(this, publisher);
        accelerometer.startUpdates();

        logger = new DiskLogger(this);
        publisher.registerReceiveCallback(packet -> {
            for (int i = 0; i < 3; i++) {
                try {
                    logger.write(packet);
                    return;
                } catch (IOException e) {
                    Log.e(TAG,"Error writing CAN packet to disk: " + e.toString());
                }
            }
        });
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
        isRunning = false;
        telemService = null;

        if (logger != null) {
            logger.close();
        }
        if (accelerometer != null) {
            accelerometer.stopUpdates();
        }
        if (tracker != null) {
            tracker.stopUpdates();
        }
        if (server != null) {
            server.close();
        }
        if (serial != null) {
            serial.cleanup();
        }

    }

    @Override
    protected void onHandleIntent(Intent intent) {

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
        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = getString(R.string.app_name);

        NotificationChannel notificationChannel = new NotificationChannel(channelId,
                channelId, NotificationManager.IMPORTANCE_DEFAULT);
        notificationChannel.setDescription(channelId);
        notificationChannel.setSound(null, null);
        manager.createNotificationChannel(notificationChannel);
    }

    private void updateNotification() {
        Notification notification = createNotification(msgNum + " messages received");

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(
                Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(100, notification);
    }

    public void receivePacket(byte[] data) {
        CANPacket p = CANPacketFactory.parsePacket(data);
        msgNum++;
        if (msgNum % 1000 == 0) {
            Log.d(TAG, "Got packet #" + msgNum + ": " + p.toString());
            updateNotification();
        }
        publisher.publishCANPacket(p);
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