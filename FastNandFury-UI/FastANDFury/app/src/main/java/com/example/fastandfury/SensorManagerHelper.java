package com.example.fastandfury;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Синглтон, слушает сенсоры и считает текущую "шумность/рывки" дороги
 * как ст. отклонение модуля линейного ускорения за скользящее окно.
 */
public class SensorManagerHelper implements SensorEventListener {

    private static volatile SensorManagerHelper INSTANCE;

    public static SensorManagerHelper getInstance(Context ctx) {
        if (INSTANCE == null) {
            synchronized (SensorManagerHelper.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SensorManagerHelper(ctx.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    private final SensorManager sensorManager;
    private final Sensor linearAccel;     // предпочтительно
    private final Sensor accel;           // fallback

    // Для low-pass фильтра гравитации, если нет TYPE_LINEAR_ACCELERATION
    private final float[] gravity = new float[3];
    private boolean gravityInited = false;

    // Скользящее окно значений модуля ускорения
    private final double[] ring = new double[128];
    private int ringCount = 0;
    private int ringIndex = 0;

    private volatile double currentStdDev = 0.0;

    private SensorManagerHelper(Context ctx) {
        sensorManager = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
        linearAccel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // Регистрируемся
        if (linearAccel != null) {
            sensorManager.registerListener(this, linearAccel, SensorManager.SENSOR_DELAY_GAME);
        } else if (accel != null) {
            sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    public double getCurrentDeviation() {
        return currentStdDev;
    }

    public void stop() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float ax, ay, az;

        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            ax = event.values[0];
            ay = event.values[1];
            az = event.values[2];
        } else {
            // Вычитаем гравитацию из акселерометра
            if (!gravityInited) {
                System.arraycopy(event.values, 0, gravity, 0, 3);
                gravityInited = true;
            }
            // Простой low-pass
            final float alpha = 0.8f;
            gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
            gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
            gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

            ax = event.values[0] - gravity[0];
            ay = event.values[1] - gravity[1];
            az = event.values[2] - gravity[2];
        }

        double mag = Math.sqrt(ax * ax + ay * ay + az * az);

        // Кладём в кольцевой буфер
        ring[ringIndex] = mag;
        ringIndex = (ringIndex + 1) % ring.length;
        if (ringCount < ring.length) ringCount++;

        // Пересчёт ст. отклонения по текущему окну (достаточно быстро для 128)
        if (ringCount > 1) {
            double mean = 0.0;
            for (int i = 0; i < ringCount; i++) mean += ring[i];
            mean /= ringCount;

            double var = 0.0;
            for (int i = 0; i < ringCount; i++) {
                double d = ring[i] - mean;
                var += d * d;
            }
            var /= (ringCount - 1);
            currentStdDev = Math.sqrt(var);
        } else {
            currentStdDev = 0.0;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { /* no-op */ }
}
