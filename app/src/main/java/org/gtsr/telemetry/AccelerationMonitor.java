package org.gtsr.telemetry;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import org.gtsr.telemetry.packet.CANPacket;

public class AccelerationMonitor implements SensorEventListener {

    private CANPublisher publisher;
    private SensorManager manager;
    private Sensor accelerometer;

    public AccelerationMonitor(Context context, CANPublisher publisher) {
        this.publisher = publisher;

        this.manager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
        this.accelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    public void startUpdates() {
        if (accelerometer != null) {
            manager.registerListener(this, accelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    public void stopUpdates() {
        manager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0] / 9.81f; // Report values in Gs, not m/s^2
        float y = event.values[1] / 9.81f;
        float z = (event.values[2] - 9.81f) / 9.81f;
        float mag = (float)Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));
        publisher.publishCANPacket(new CANPacket(
                (short)0x629, x, y));
        publisher.publishCANPacket(new CANPacket(
                (short)0x62a, z, mag));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int val) {

    }

}
