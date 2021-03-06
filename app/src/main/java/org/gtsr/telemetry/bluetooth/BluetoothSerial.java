package org.gtsr.telemetry.bluetooth;

import android.bluetooth.BluetoothGatt;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.gtsr.telemetry.bluetooth.adafruit_ble.BlePeripheral;
import org.gtsr.telemetry.bluetooth.adafruit_ble.BlePeripheralUart;
import org.gtsr.telemetry.bluetooth.adafruit_ble.BleScanner;
import org.gtsr.telemetry.bluetooth.adafruit_ble.UartPacket;
import org.gtsr.telemetry.bluetooth.adafruit_ble.UartPacketManager;
import org.gtsr.telemetry.bluetooth.adafruit_ble.UartPacketManagerBase;
import org.gtsr.telemetry.bluetooth.adafruit_ble.PeripheralModeManager;
import org.gtsr.telemetry.bluetooth.adafruit_ble.UartPeripheralService;

import java.util.ArrayList;
import java.util.List;

import static org.gtsr.telemetry.TelemetryService.TAG;

public class BluetoothSerial implements UartPacketManagerBase.Listener, BleScanner.BleScannerListener {
    private final String SERIAL_UUID = "C7:44:3A:E8:E5:94";

    private Context context;

    private UartPacketManager packetManager;
    BleScanner scanner;

    BlePeripheral serialDevice;
    List<BlePeripheralUart> uart;
    UartPeripheralService uartService;

    ReceiveCallback rxCallback;

    private Connected connected;

    private int PACKET_BUF_LEN = 100;

    private byte[] packetData = new byte[PACKET_BUF_LEN];
    private int packetPtr = 0;

    public BluetoothSerial(Context context, ReceiveCallback callback) {
        this.context = context;
        rxCallback = callback;

        packetManager = new UartPacketManager(context, this, true);

        uartService = PeripheralModeManager.getInstance().getUartPeripheralService();

        if (uartService == null) {
            uartService = new UartPeripheralService(context);
        }
        uartService.uartEnable(data -> packetManager.onRxDataReceived(data,
                null, BluetoothGatt.GATT_SUCCESS));

        scanner = BleScanner.getInstance();
        scanner.setListener(this);


        IntentFilter filter = new IntentFilter();
        filter.addAction(BlePeripheral.kBlePeripheral_OnConnecting);
        filter.addAction(BlePeripheral.kBlePeripheral_OnConnected);
        filter.addAction(BlePeripheral.kBlePeripheral_OnDisconnected);
        LocalBroadcastManager.getInstance(context).registerReceiver(mGattUpdateReceiver, filter);

        uart = new ArrayList<>();
        connected = Connected.FALSE;
    }

    public void init() {
        Log.d(TAG, "Initializing bluetooth handler");

        scanner.start();
    }

    public void close() {
        scanner.disconnectFromAll();
        scanner.stop();
    }

    public void connect() {
        if (serialDevice != null && serialDevice.isDisconnected()) {
            if (context != null) {
                Log.d(TAG, "Attempting connection...");
                serialDevice.connect(context);
            }
        } else {
            Log.d(TAG, "Force starting scan.");
            scanner.start();
        }
    }

    public Connected isConnected() {
        return connected;
    }

    @Override
    public void onScanPeripheralsUpdated(List<BlePeripheral> scanResults) {
        for (BlePeripheral p : scanResults) {
            if (p.getIdentifier().equals(SERIAL_UUID) && scanner.isScanning()) {
                Log.d(TAG, "Serial device found!");
                serialDevice = p;
                scanner.stop();
                p.connect(context);
                connected = Connected.PENDING;
            }
        }
    }

    @Override
    public void onScanPeripheralsFailed(int errorCode) {

    }

    @Override
    public void onScanStatusChanged(boolean isScanning) {

    }

    @Override
    public void onUartPacket(UartPacket packet) {
        if (packet.getMode() == UartPacket.TRANSFERMODE_RX) {

            for (byte b : packet.getData()) {
                packetData[packetPtr] = b;

                if (b == '\n') {
                    rxCallback.receiveLine(packetData, packetPtr + 1);
                    packetPtr = 0;
                } else {
                    packetPtr = (packetPtr + 1) % PACKET_BUF_LEN;
                }
            }
        }
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            final String identifier = intent.getStringExtra(BlePeripheral.kExtra_deviceAddress);
            final BlePeripheral blePeripheral = serialDevice;
            if (blePeripheral != null) {
                if (BlePeripheral.kBlePeripheral_OnConnected.equals(action)) {
                    blePeripheral.discoverServices(status -> {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            Log.d(TAG, "Successfully discovered services!");
                            if (!BlePeripheralUart.isUartInitialized(blePeripheral, uart)) {
                                Log.d(TAG, "UART not initialized! Initializing...");
                                BlePeripheralUart u = new BlePeripheralUart(blePeripheral);
                                uart.add(u);
                                u.uartEnable(packetManager, completionStatus -> {
                                    if (completionStatus == BluetoothGatt.GATT_SUCCESS) {
                                        Log.d(TAG, "UART enabled!");
                                    }
                                });

                                connected = Connected.TRUE;
                            }
                        } else {
                            Log.e(TAG, "Error discovering services.");
                            serialDevice.disconnect();
                        }
                        Log.d(TAG, "Discovered services! " + status);
                    });
                } else if (BlePeripheral.kBlePeripheral_OnDisconnected.equals(action)) {
                    connected = Connected.FALSE;
                } else if (BlePeripheral.kBlePeripheral_OnConnecting.equals(action)) {
                    connected = Connected.PENDING;
                } else {
                    Log.w(TAG, "ScannerViewModel mGattUpdateReceiver with null peripheral");
                }
            }
        }
    };

    public interface ReceiveCallback {
        void receiveLine(byte[] data, int length);
    }

    public enum Connected {
        FALSE,
        PENDING,
        TRUE
    }
}
