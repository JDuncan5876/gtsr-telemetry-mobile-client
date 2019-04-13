package org.gtsr.telemetry;

import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class TelemetryServer {
    public static final String TAG = TelemetryServer.class.getName();
    public static final String SERVER_NAME = "solarracing.me";
    public static final int SERVER_PORT = 6001;

    private Socket socket;
    private ReceiveCallback receiveCallback;

    public TelemetryServer(ReceiveCallback receiveCallback) {
        this.receiveCallback = receiveCallback;
    }

    private void receive(Socket socket) {
        if (socket == null) {
            return;
        }
        while (socket.isConnected()) {
            try {
                int value = socket.getInputStream().read();
                if (value == -1) {
                    Log.d(TAG, "Disconnected from server");
                    if (socket.isConnected()) {
                        socket.close();
                    }
                    return;
                }
                receiveCallback.receiveByte((byte)value);
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
        }
    }

    public synchronized void open() {
        if (socket != null && socket.isConnected()) {
            return;
        }
        try {
            socket = new Socket(InetAddress.getByName(SERVER_NAME), SERVER_PORT);
        } catch (IOException e) {
            Log.e(TAG, "Error connecting to server: " + e.toString());
            socket = null;
            return;
        }
        new Thread(() -> receive(socket)).start();
    }

    public synchronized void close() {
        if (socket == null || !socket.isConnected()) {
            return;
        }
        try {
            socket.close();
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
    }

    public synchronized boolean write(byte[] data) {
        if (socket == null || !socket.isConnected()) {
            open();
            if (socket == null || !socket.isConnected()) {
                return false;
            }
        }
        try {
            socket.getOutputStream().write(data);
            return true;
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            return false;
        }
    }

    public interface ReceiveCallback {
        void receiveByte(byte payload);
    }
}
