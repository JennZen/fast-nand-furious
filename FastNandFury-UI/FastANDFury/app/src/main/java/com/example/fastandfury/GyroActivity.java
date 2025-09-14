package com.example.fastandfury;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class GyroActivity extends AppCompatActivity implements SensorEventListener {


    private static double currentDeviation = 0;
    private static List<Double> deviationHistory = new ArrayList<>();

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private TextView gyroText;
    private View tiltCircle;
    private ImageButton backButton;

    private float lastZ = 0;
    private float filteredZ = 0;
    private float alpha = 0.8f;

    private int potholesCount = 0;
    private final float POTHOLES_THRESHOLD = 2.0f;
    private long lastPotholeTime = 0;
    private final long POTHOLES_COOLDOWN = 1000;

    private Handler handler = new Handler(Looper.getMainLooper());
    private final int UPDATE_INTERVAL = 100; // Обновляем UI каждые 100ms

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gyro);

        gyroText = findViewById(R.id.gyroText);
        tiltCircle = findViewById(R.id.tiltCircle);
        backButton = findViewById(R.id.backButton);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        if (accelerometer == null) {
            Toast.makeText(this, "Акселерометр не найден", Toast.LENGTH_LONG).show();
        }

        backButton.setOnClickListener(v -> finish());

        // Запускаем обновление UI
        handler.post(updateUI);
    }

    // Runnable для безопасного обновления UI
    private Runnable updateUI = new Runnable() {
        @Override
        public void run() {
            updateUI();
            handler.postDelayed(this, UPDATE_INTERVAL);
        }
    };


    public static double getCurrentDeviation() {
        return currentDeviation;
    }

    // Метод для получения истории отклонений
    public static List<Double> getDeviationHistory() {
        return new ArrayList<>(deviationHistory);
    }




    public static Intent createIntent(Context context, double deviation) {
        Intent intent = new Intent(context, MapsActivity.class);
        intent.putExtra("last_deviation", deviation);
        return intent;
    }
    private void updateUI() {
        String text = String.format(
                "Акселерометр\nZ: %.2f G\n" +
                        "Фильтрованный: %.2f G\n" +
                        "Обнаружено ям: %d\n" +
                        "Порог: %.2f G",
                lastZ, filteredZ, potholesCount, POTHOLES_THRESHOLD
        );
        gyroText.setText(text);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        handler.removeCallbacks(updateUI);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float z = event.values[2];
            lastZ = z;

            // 1. ФИЛЬТРУЕМ медленные движения
            filteredZ = alpha * filteredZ + (1 - alpha) * z;


            // 2. Вычисляем ВНЕЗАПНОЕ ускорение
            float suddenAcceleration = Math.abs(z - filteredZ);

            currentDeviation = suddenAcceleration;
            deviationHistory.add((double) suddenAcceleration);

            if (deviationHistory.size() > 100) {
                deviationHistory.remove(0);
            }

            // 3. Детектим ямы
            long currentTime = System.currentTimeMillis();
            if (suddenAcceleration > POTHOLES_THRESHOLD &&
                    (currentTime - lastPotholeTime) > POTHOLES_COOLDOWN) {

                potholesCount++;
                lastPotholeTime = currentTime;

                // Показываем Toast в UI потоке
                handler.post(() -> {
                    Toast.makeText(GyroActivity.this, "Яма обнаружена! 🕳️", Toast.LENGTH_SHORT).show();
                });
            }

            // 4. Двигаем шарик в UI потоке
            float ballMovement = suddenAcceleration * 50;
            handler.post(() -> {
                tiltCircle.setTranslationY(ballMovement);
            });
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Не нужно
    }

    // Кнопка сброса счётчика ям
    public void onResetCounter(View view) {
        potholesCount = 0;
        Toast.makeText(this, "Счётчик ям сброшен!", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateUI);
    }
}
