package org.gtsr.telemetry.packet;

import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;

public class PacketReceiver {
    private static final String TAG = PacketReceiver.class.getName();
    private enum State {
        AWAITING_HEADER,
        RECEIVING_LENGTH,
        RECEIVING_MESSAGE
    }

    private final Byte[] expectedHeader;

    private State state;
    private Queue<Byte> header;
    private int length;
    private List<Byte> message;
    private ReceiveCallback writer;

    public PacketReceiver(@NonNull ReceiveCallback writer, @NonNull Byte[] expectedHeader) {
        if (expectedHeader.length == 0) {
            throw new IllegalArgumentException("Expected header cannot be empty!");
        }
        state = State.AWAITING_HEADER;
        header = new ArrayDeque<>(expectedHeader.length);
        length = 0;
        message = new ArrayList<>();
        this.writer = writer;
        this.expectedHeader = expectedHeader;
    }

    public void receiveByte(byte payload) {
        switch (state) {
            case AWAITING_HEADER:
                receiveHeaderByte(payload);
                break;
            case RECEIVING_LENGTH:
                receiveLength(payload);
                break;
            case RECEIVING_MESSAGE:
                receiveMessageByte(payload);
                break;
        }
    }

    private void receiveHeaderByte(byte payload) {
        if (header.size() == expectedHeader.length) {
            header.remove();
        }
        header.add(payload);
        Log.d(TAG, "Current header: " + Arrays.toString(header.toArray()));
        if (Arrays.equals(expectedHeader, header.toArray())) {
            Log.d(TAG, "Header matched");
            state = State.RECEIVING_LENGTH;
            header.clear();
        }
    }

    private void receiveLength(byte payload) {
        length = payload;
        state = State.RECEIVING_MESSAGE;
    }

    private void receiveMessageByte(byte payload) {
        message.add(payload);
        if (message.size() == length) {
            byte[] rawMessage = new byte[message.size()];
            for (int i = 0; i < message.size(); i++) {
                rawMessage[i] = message.get(i);
            }
            Log.d(TAG, "received message: " + Arrays.toString(rawMessage));
            writer.receiveMessage(rawMessage);
            state = State.AWAITING_HEADER;
            message = new ArrayList<>();
        }
    }

    public interface ReceiveCallback {
        void receiveMessage(byte[] message);
    }

}
