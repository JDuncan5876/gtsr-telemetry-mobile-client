package org.gtsr.telemetry.packet;

import java.util.Arrays;

public class SerialCANPacket extends SerialPacket {
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
        return canId + ", " + dataLen + " " + Arrays.toString(data);
    }
}
