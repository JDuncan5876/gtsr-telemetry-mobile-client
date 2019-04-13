package org.gtsr.telemetry.threads;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import org.gtsr.telemetry.TelemetryService;
import org.gtsr.telemetry.packet.CANPacket;

import java.util.concurrent.ConcurrentLinkedQueue;


public abstract class TelemReceiverThread extends Thread {
    private BroadcastReceiver threadReceiver;

    protected ConcurrentLinkedQueue<CANPacket> queue = new ConcurrentLinkedQueue<>();

    public TelemReceiverThread(Context c) {
        threadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(TelemetryService.TELEM_PACKET_BROADCAST_ACTION)) {
                    queue.offer((CANPacket)intent.getSerializableExtra(CANPacket.class.getSimpleName()));
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TelemetryService.TELEM_PACKET_BROADCAST_ACTION);
        LocalBroadcastManager.getInstance(c).registerReceiver(threadReceiver, intentFilter);
    }
}
