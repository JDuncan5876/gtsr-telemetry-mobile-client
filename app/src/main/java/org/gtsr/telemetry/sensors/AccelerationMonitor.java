package org.gtsr.telemetry.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import org.gtsr.telemetry.CANPublisher;
import org.gtsr.telemetry.packet.CANPacket;
import org.gtsr.telemetry.serial.TelemetrySerial;

public class AccelerationMonitor implements SensorEventListener {

    private CANPublisher publisher;
    private TelemetrySerial serial;
    private SensorManager manager;
    private Sensor accelerometer;

    public AccelerationMonitor(Context context, CANPublisher publisher, TelemetrySerial serial) {
        this.publisher = publisher;
        this.serial = serial;

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
        CANPacket[] packets = new CANPacket[2];
        packets[0] = new CANPacket((short)0x629, x, y);
        packets[1] = new CANPacket((short)0x62a, z, mag);
        for (CANPacket packet : packets) {
            publisher.publishCANPacket(packet);
            serial.send(packet.marshalSerial());
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int val) {

    }

}
