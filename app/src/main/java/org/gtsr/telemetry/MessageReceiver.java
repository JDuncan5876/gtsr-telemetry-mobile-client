package org.gtsr.telemetry;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;

public class MessageReceiver implements TelemetryServer.ReceiveCallback {
    private enum State {
        AWAITING_HEADER,
        RECEIVING_LENGTH,
        RECEIVING_MESSAGE
    }

    private static final Byte[] expectedHeader = {'G', 'T', 'S', 'R'};

    private State state;
    private Queue<Byte> header;
    private int length;
    private List<Byte> message;
    private MessageWriter writer;

    public MessageReceiver(MessageWriter writer) {
        state = State.AWAITING_HEADER;
        header = new ArrayDeque<>(4);
        length = 0;
        message = new ArrayList<>();
        this.writer = writer;
    }

    @Override
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
        if (header.size() == 4) {
            header.remove();
        }
        header.add(payload);
        if (Arrays.equals(expectedHeader, header.toArray())) {
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
            writer.writeMessage(rawMessage);
            state = State.AWAITING_HEADER;
            message = new ArrayList<>();
        }
    }

    public interface MessageWriter {
        void writeMessage(byte[] message);
    }

}
