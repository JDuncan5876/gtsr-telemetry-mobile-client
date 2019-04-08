/*
    Modified from example code at https://github.com/felHR85/UsbSerial
 */
package org.gtsr.telemetry;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import android.os.IBinder;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class TelemetryService extends IntentService implements ServiceConnection, SerialListener {

    private enum Connected { False, Pending, True }

    public static final String TAG = "GTSRTelemetryService";
    private static final int BAUD_RATE = 256000; // BaudRate. Change this value if you need

    private static final String TELEM_PACKET_BROADCAST_ACTION = "org.gtsr.telemetry.BROADCAST_PACKET";

    public static final String INTENT_ACTION_GRANT_USB = BuildConfig.APPLICATION_ID + ".GRANT_USB";

    private int deviceId, portNum, baudRate;
    private String newline = "\r\n";

    private SerialSocket socket;
    private SerialService service;
    private boolean initialStart = true;
    private Connected connected = Connected.False;
    private BroadcastReceiver broadcastReceiver;

    private ReadThread readThread;
    private LogThread logThread;

    private boolean serviceConnected = false;

    private Notification serviceNotification;

    public TelemetryService() {
        super(TelemetryService.class.getSimpleName());
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
                if(intent.getAction().equals(INTENT_ACTION_GRANT_USB)) {
                    Boolean granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
                    connect(granted);
                }
            }
        };
        registerReceiver(broadcastReceiver, new IntentFilter(INTENT_ACTION_GRANT_USB));

        if (service == null) {
            bindService(new Intent(this, SerialService.class), this, Context.BIND_AUTO_CREATE);
            serviceConnected = true;
        }
        if(initialStart && service != null) {
            initialStart = false;
            connect();
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Stopping service.");
        if (connected != Connected.False)
            disconnect();

        if (service != null) {
            unbindService(this);
        }
        unregisterReceiver(broadcastReceiver);
        super.onDestroy();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if(intent.getAction().equals("android.hardware.usb.action.USB_DEVICE_ATTACHED")){
            if(service != null) {
                initialStart = false;
                connect();
            }
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
    public void onServiceConnected(ComponentName name, IBinder binder) {
        service = ((SerialService.SerialBinder) binder).getService();
        if(initialStart) {
            initialStart = false;
            connect();
        }
        serviceConnected = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        prepareNotification();
        startForeground(1, createNotification("Connecting..."));

        return Service.START_STICKY;
    }

    class TelemBroadcastReceiver extends BroadcastReceiver {
        ArrayList<SerialPacket> packetQueue;
        @Override
        public void onReceive(Context c, Intent i) {

        }
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

    private class LogThread extends Thread {
        BroadcastReceiver recv;
        @Override
        public void run() {

            Looper.prepare();
            Toast.makeText(TelemetryService.this, "Starting log thread!", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Starting logging thread.");
            //recv = new TelemBroadcastReceiver();
            //IntentFilter filt = new IntentFilter(TELEM_PACKET_BROADCAST_ACTION);
            //filt.addAction();
        }
    }
    private enum ReceiverState {
        IDLE,
        RECEIVING_PACKET
    };

    private class SerialPacket {

    }

    private class SerialCANPacket extends SerialPacket {
        public short canId;
        public short dataLen;
        public String[] data;

        public SerialCANPacket(short canId, short dataLen, String[] data) {
            this.canId = canId;
            this.dataLen = dataLen;
            this.data = data;
        }

        public SerialCANPacket(short canId, short dataLen) {
            this.canId = canId;
            this.dataLen = dataLen;
        }

        @Override
        public String toString() {
            return canId + ", " + dataLen + Arrays.toString(data);
        }
    }

    private class ReadThread extends Thread {
        private AtomicBoolean keep = new AtomicBoolean(true);
        private int PACKET_BUF_LEN = 30;

        private byte[] packetData = new byte[PACKET_BUF_LEN];
        private int packetPtr = 0;

        ByteBuffer byteBuf;



        @Override
        public void run() {

        }

        public void setKeep(boolean keep){
            this.keep.set(keep);
        }
    }

    /*
     * Serial + UI
     */
    private void connect() {
        connect(null);
    }

    private void connect(Boolean permissionGranted) {
        UsbDevice device = null;
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        for(UsbDevice v : usbManager.getDeviceList().values())
            //if(v.getDeviceId() == deviceId)
            device = v;
        if(device == null) {
            status("connection failed: device not found");
            return;
        }
        UsbSerialDriver driver = UsbSerialProber.getDefaultProber().probeDevice(device);
        if(driver == null) {
            // driver = CustomProber.getCustomProber().probeDevice(device);
        }
        if(driver == null) {
            status("connection failed: no driver for device");
            return;
        }
        if(driver.getPorts().size() < portNum) {
            status("connection failed: not enough ports at device");
            return;
        }
        UsbSerialPort usbSerialPort = driver.getPorts().get(portNum);
        UsbDeviceConnection usbConnection = usbManager.openDevice(driver.getDevice());
        if(usbConnection == null && permissionGranted == null) {
            if (!usbManager.hasPermission(driver.getDevice())) {
                PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(INTENT_ACTION_GRANT_USB), 0);
                usbManager.requestPermission(driver.getDevice(), usbPermissionIntent);
                return;
            }
        }
        if(usbConnection == null) {
            if (!usbManager.hasPermission(driver.getDevice()))
                status("connection failed: permission denied");
            else
                status("connection failed: open failed");
            return;
        }

        connected = Connected.Pending;
        try {
            socket = new SerialSocket();
            service.connect(this, "Connected");
            socket.connect(this, service, usbConnection, usbSerialPort, BAUD_RATE);
            // usb connect is not asynchronous. connect-success and connect-error are returned immediately from socket.connect
            // for consistency to bluetooth/bluetooth-LE app use same SerialListener and SerialService classes
            onSerialConnect();
        } catch (Exception e) {
            onSerialConnectError(e);
        }
    }

    private void disconnect() {
        connected = Connected.False;
        if (service != null) {
            service.disconnect();
        }
        if (socket != null) {
            socket.disconnect();
        }
        socket = null;
    }


    private void send(String str) {
        if(connected != Connected.True) {
            Toast.makeText(this, "not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            byte[] data = (str + newline).getBytes();
            socket.write(data);
        } catch (Exception e) {
            onSerialIoError(e);
        }
    }

    private int PACKET_BUF_LEN = 100;

    private byte[] packetData = new byte[PACKET_BUF_LEN];
    private int packetPtr = 0;

    ByteBuffer byteBuf;

    private SerialPacket parsePacket(int len) {
        //Log.d(TAG, "Parsing packet with length: " + len);
        if (byteBuf == null) {
            byteBuf = ByteBuffer.wrap(packetData);
        }
        String[] splitStr = new String(packetData).substring(0,len-1).split(",");
        if (splitStr.length > 2) {
            //Log.d(TAG, Arrays.toString(splitStr));
            try {
                short canLength = Integer.valueOf(splitStr[1]).shortValue();
                SerialCANPacket packet = new SerialCANPacket(Integer.valueOf(splitStr[0]).shortValue(), canLength, Arrays.copyOfRange(splitStr, 2, canLength));
                return packet;
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid number format!");
                Log.e(TAG, e.getLocalizedMessage());
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Invalid argument!");
                Log.e(TAG, e.getLocalizedMessage());
            }
        }
        return null;
    }

    private static String toASCII(int value) {
        int length = 4;
        StringBuilder builder = new StringBuilder(length);
        for (int i = length - 1; i >= 0; i--) {
            builder.append((char) ((value >> (8 * i)) & 0xFF));
        }
        return builder.toString();
    }

    int msgNum = 0;
    private void receive(byte[] data) {

    }

    private void status(String str) {
        Log.d(TAG, str);
    }

    /*
     * SerialListener
     */
    @Override
    public void onSerialConnect() {
        status("connected");
        connected = Connected.True;
    }

    @Override
    public void onSerialConnectError(Exception e) {
        status("connection failed: " + e.getMessage());
        disconnect();
    }

    @Override
    public void onSerialRead(byte[] data) {
        //Log.d("GTSR Telem", "Received bytes!");
        for (int i = 0; i < data.length; i++) {
            //Log.d(TAG, "Byte: " + data[i]);
            packetData[packetPtr] = data[i];

            if (data[i] == '\n') {
                SerialPacket p = parsePacket(packetPtr + 1);
                packetPtr = 0;

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
            } else {
                packetPtr = (packetPtr + 1) % PACKET_BUF_LEN;
            }
        }
    }

    @Override
    public void onSerialIoError(Exception e) {
        status("connection lost: " + e.getMessage());
        disconnect();
    }
}