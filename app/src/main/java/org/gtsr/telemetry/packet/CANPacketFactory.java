package org.gtsr.telemetry.packet;

import android.util.Log;

import java.util.Arrays;

import static org.gtsr.telemetry.TelemetryService.TAG;

public class CANPacketFactory {
    public static CANPacket parsePacket(int len, byte[] packetData) {

        if (len > 4) {
            try {
                short canId = (short)(((int)packetData[0] & 0xFF) + (((int)packetData[1]) << 8));
                short canLength = (short)packetData[2];
                return new CANPacket(canId, canLength, Arrays.copyOfRange(packetData, 3, 3 + canLength));
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
}
