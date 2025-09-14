package com.example.fastandfury;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class CameraActivity extends AppCompatActivity {

    private ImageButton backButton;
    private ImageButton captureButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        // Инициализация кнопок
        backButton = findViewById(R.id.back_button);
        captureButton = findViewById(R.id.capture_button);

        // Обработчик кнопки "Назад"
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Закрываем активность и возвращаемся на главную
            }
        });

        // Обработчик кнопки "Снять фото"
        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhoto();
            }
        });
    }

    private void takePhoto() {
        // Здесь будет реальная логика съемки фото
        // Пока просто заглушка
        Toast.makeText(this, "Photo taken! Returning to main screen...", Toast.LENGTH_SHORT).show();

        // Возвращаемся на главный экран после съемки
        finish();
    }

    @Override
    public void onBackPressed() {
        // Обработка системной кнопки "Назад"
        super.onBackPressed();
        finish(); // Закрываем активность
    }
}
