package org.gtsr.telemetry.serial;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.IBinder;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import org.gtsr.telemetry.packet.PacketReceiver;
import org.gtsr.telemetry.libs.Constants;
import org.gtsr.telemetry.libs.SerialListener;
import org.gtsr.telemetry.libs.SerialService;
import org.gtsr.telemetry.libs.SerialSocket;

import static org.gtsr.telemetry.TelemetryService.TAG;

public class TelemetrySerial implements SerialListener, ServiceConnection {

    private int deviceId, portNum, baudRate;

    public enum Connected { False, Pending, True }

    private Connected connected = Connected.False;

    public Connected isConnected() {
        return connected;
    }

    private SerialSocket socket;
    private SerialService service;

    private Context context;

    private boolean initialStart = true;
    private boolean serviceConnected = false;

    private PacketReceiver receiver;

    public TelemetrySerial(Context context, int baudRate, PacketReceiver.ReceiveCallback receiveCallback) {
        this.context = context;
        this.baudRate = baudRate;
        this.receiver = new PacketReceiver(receiveCallback, new Byte[]{'G', 'T'});
    }

    public void init() {
        if (service == null) {
            context.bindService(new Intent(context, SerialService.class), this,
                    Context.BIND_AUTO_CREATE);
            serviceConnected = true;
        }
        if(isConnected() == TelemetrySerial.Connected.False && service != null) {
            connect();
        }
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
        for (byte b : data) {
            receiver.receiveByte(b);
        }
    }

    @Override
    public void onSerialIoError(Exception e) {
        status("connection lost: " + e.getMessage());
        disconnect();
    }

    /*
     * Serial + UI
     */
    public void connect() {
        connect(null);
    }

    public void connect(Boolean permissionGranted) {
        UsbDevice device = null;
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
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
                PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(context,
                        0, new Intent(Constants.INTENT_ACTION_GRANT_USB), 0);
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
            socket.connect(context, service, usbConnection, usbSerialPort, baudRate);
            // usb connect is not asynchronous. connect-success and connect-error are returned immediately from socket.connect
            // for consistency to bluetooth/bluetooth-LE app use same SerialListener and SerialService classes
            onSerialConnect();
        } catch (Exception e) {
            onSerialConnectError(e);
        }
    }

    public void disconnect() {
        connected = Connected.False;
        if (service != null) {
            service.disconnect();
        }
        if (socket != null) {
            socket.disconnect();
        }
        socket = null;
    }

    public void send(byte[] data) {
        if(connected != TelemetrySerial.Connected.True) {
            status("not connected");
            return;
        }
        try {
            byte[] header = {'G', 'T', (byte)data.length};
            socket.write(header);
            socket.write(data);
        } catch (Exception e) {
            onSerialIoError(e);
        }
    }

    public void deviceAttached() {
        if(service != null) {
            initialStart = false;
            connect();
        }
    }

    public void cleanup() {
        if (isConnected() != TelemetrySerial.Connected.False) {
            disconnect();
        }
        if (service != null) {
            context.unbindService(this);
        }
    }

    private void status(String str) {
        Log.d(TAG, str);
    }
}