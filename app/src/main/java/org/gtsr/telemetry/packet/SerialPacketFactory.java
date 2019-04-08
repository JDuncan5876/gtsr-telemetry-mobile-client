package org.gtsr.telemetry.packet;

import android.util.Log;

import java.util.Arrays;

import static org.gtsr.telemetry.TelemetryService.TAG;

public class SerialPacketFactory {
    public static SerialPacket parsePacket(int len, byte[] packetData) {
        String[] splitStr = new String(packetData).substring(0,len-1).split(",");
        if (splitStr.length > 2) {
            try {
                short canLength = Integer.valueOf(splitStr[1]).shortValue();
                return new SerialCANPacket(
                        Integer.valueOf(splitStr[0]).shortValue(), canLength,
                        Arrays.copyOfRange(splitStr, 1, canLength+1));
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
