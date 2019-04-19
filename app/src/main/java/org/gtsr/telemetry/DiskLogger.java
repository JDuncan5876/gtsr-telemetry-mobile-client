package org.gtsr.telemetry;

import android.content.Context;
import android.util.Log;

import org.gtsr.telemetry.packet.CANPacket;
import org.gtsr.telemetry.packet.TimestampedCANPacket;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DiskLogger {

    private static final String TAG = DiskLogger.class.getName();
    private static final String filename = "gtsr_log";

    private FileOutputStream out;
    private Context ctx;

    public DiskLogger(Context ctx) {
        this.ctx = ctx;
        try {
            out = ctx.openFileOutput(filename, Context.MODE_PRIVATE);
        } catch (IOException e) {
            Log.e(TAG, "Error opening log file: " + e.toString());
        }
    }

    public synchronized void write(CANPacket packet) throws IOException {
        out.write(ByteBuffer.allocate(12 + packet.getDataLen())
                .putShort(packet.getCanId())
                .putLong(new Date().getTime())
                .putShort(packet.getDataLen())
                .put(packet.getData())
                .array());
    }

    public synchronized List<TimestampedCANPacket> read() throws IOException {
        List<TimestampedCANPacket> packets = new ArrayList<>();
        FileInputStream in = ctx.openFileInput(filename);
        byte[] header = new byte[12];
        while (in.available() > 0) {
            int nread = in.read(header);
            if (nread != 12) {
                Log.e(TAG, "Error reading CAN packet header from flash");
                return packets;
            }
            ByteBuffer headerBuffer = ByteBuffer.wrap(header);
            short canId = headerBuffer.getShort();
            Date timestamp = new Date(headerBuffer.getLong());
            short dataLen = headerBuffer.getShort();
            byte[] data = new byte[dataLen];
            nread = in.read(data);
            if (nread != dataLen) {
                Log.e(TAG, "Error reading CAN data from flash");
                return packets;
            }
            packets.add(new TimestampedCANPacket(canId, dataLen, data, timestamp));
        }
        in.close();
        return packets;
    }

    public void close() {
        try {
            out.close();
        } catch (IOException e) {
            Log.e(TAG, "Error closing log file: " + e.toString());
        }
    }

}
