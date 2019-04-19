package org.gtsr.telemetry.packet;

import java.util.Date;

public class TimestampedCANPacket extends CANPacket {

    private Date timestamp;

    public TimestampedCANPacket(CANPacket packet, Date timestamp) {
        super(packet.getCanId(), packet.getDataLen(), packet.getData());
        this.timestamp = timestamp;
    }

    public TimestampedCANPacket(CANPacket packet) {
        this(packet, new Date());
    }

    public TimestampedCANPacket(short canId, short dataLen, byte[] data, Date timestamp) {
        super(canId, dataLen, data);
        this.timestamp = timestamp;
    }

    public Date getTimestamp() {
        return this.timestamp;
    }

}
