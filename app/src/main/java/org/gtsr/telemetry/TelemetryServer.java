package org.gtsr.telemetry;

import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.Executors;

public class TelemetryServer {
    public static final String TAG = TelemetryServer.class.getName();
    public static final String SERVER_NAME = "solarracing.me";
    public static final int SERVER_PORT = 6001;

    private Socket socket;
    private ReceiveCallback receiveCallback;
    private boolean connected;

    public TelemetryServer(ReceiveCallback receiveCallback) {
        this.receiveCallback = receiveCallback;
        this.connected = false;
    }

    public boolean isConnected() {
        return this.connected;
    }

    private void receive() {
        while (connected) {
            try {
                int value = socket.getInputStream().read();
                if (value == -1) {
                    Log.d(TAG, "Disconnected from server");
                    connected = false;
                    return;
                }
                receiveCallback.receiveByte((byte)value);
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
        }
    }

    public void open() {
        synchronized (this) {
            if (connected) {
                return;
            }
            try {
                socket = new Socket(InetAddress.getByName(SERVER_NAME), SERVER_PORT);
                connected = true;
            } catch (IOException e) {
                Log.e(TAG, "Error connecting to server: " + e.toString());
                socket = null;
                connected = false;
                return;
            }
        }
        Executors.newSingleThreadExecutor().execute(this::receive);
    }

    public void close() {
        synchronized (this) {
            if (!connected) {
                return;
            }
            try {
                connected = false;
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
        }
    }

    public void write(byte[] data) {
        if (!connected) {
            return;
        }
        try {
            socket.getOutputStream().write(data);
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
    }

    public interface ReceiveCallback {
        void receiveByte(byte payload);
    }
}
