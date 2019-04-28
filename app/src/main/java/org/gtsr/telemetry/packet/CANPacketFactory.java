package org.gtsr.telemetry.packet;

import java.util.Arrays;

public class CANPacketFactory {
    public static CANPacket parsePacket(byte[] message) {
        short canId = (short)(((int)message[0] & 0xFF) + (((int)message[1]) << 8));
        return new CANPacket(canId, (short)(message.length - 2),
                Arrays.copyOfRange(message, 2, message.length));
    }
}
