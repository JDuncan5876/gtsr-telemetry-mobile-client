package org.gtsr.telemetry.packet;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;

public class CANPacket implements Serializable {
    private short canId;
    private short dataLen;
    private byte[] data;

    public CANPacket(short canId, short dataLen, byte[] data) {
        this.canId = canId;
        this.dataLen = dataLen;
        this.data = data;
    }

    public CANPacket(short canId, float low, float high) {
        this.canId = canId;
        this.dataLen = 8;
        this.data = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putFloat(low).putFloat(high).array();
    }

    private static byte[] byteArray(List<Byte> list) {
        byte[] bytes = new byte[list.size()];
        for (int i = 0; i < list.size(); i++) {
            bytes[i] = list.get(i);
        }
        return bytes;
    }

    public short getCanId() {
        return canId;
    }

    public short getDataLen() {
        return dataLen;
    }

    public byte[] getData() {
        return this.data.clone();
    }

    public byte[] marshalTCP() {
        byte[] message = new byte[12];
        message[0] = 'G';
        message[1] = 'T';
        message[2] = (byte) canId;
        message[3] = (byte) (canId >> 8);
        System.arraycopy(data, 0, message, 4, Math.min(8, data.length));
        return message;
    }

    public byte[] marshalSerial() {
        byte[] message = new byte[2 + data.length];
        message[0] = (byte) canId;
        message[1] = (byte) (canId >> 8);
        System.arraycopy(data, 0, message, 2, data.length);
        return message;
    }

    @Override
    public String toString() {
        return canId + ", " + dataLen + " " + Arrays.toString(data);
    }
}
