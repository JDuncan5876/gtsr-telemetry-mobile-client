package org.gtsr.telemetry.threads;

import android.content.Context;
import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

public class LoggingThread extends TelemReceiverThread {
    private final String TAG = "LogThread";
    AtomicBoolean keepAlive = new AtomicBoolean(true);

    public LoggingThread(Context context) {
        super(context);
    }

    @Override
    public void run() {
        while (keepAlive.get()) {
            Log.d(TAG, "Queue length: " + queue.size());

            while(queue.poll() != null) {

            }
            try {
                sleep(100);
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrupted!", e);
            }
        }
    }

    public void setKeepAlive(boolean alive) {
        keepAlive.set(alive);
    }
}
