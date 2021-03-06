package org.gtsr.telemetry;

import org.gtsr.telemetry.packet.CANPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class CANPublisher {
    private static final int N_EXECUTOR_THREADS = 20;

    public interface ReceiveCallback {
        void receivePacket(CANPacket packet);
    }

    private List<ReceiveCallback> callbacks;
    private Executor executor;

    public CANPublisher() {
        callbacks = new ArrayList<>();
        executor = Executors.newFixedThreadPool(N_EXECUTOR_THREADS);
    }

    public synchronized void registerReceiveCallback(ReceiveCallback callback) {
        callbacks.add(callback);
    }

    public synchronized void publishCANPacket(CANPacket packet) {
        callbacks.forEach(callback -> executor.execute(() -> callback.receivePacket(packet)));
    }
}
