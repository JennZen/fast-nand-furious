package com.example.fastandfury;

import android.util.Log;

import com.google.gson.Gson;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class SimpleDataSender {
    private static final String TAG = "SimpleDataSender";
    private final String URI_SENT_REPORT = "https://febd658e146b.ngrok-free.app/report-road/sent-report";
    // Добавьте этот метод в SimpleDataSender
    public void sendPhotoData(PhotoData photoData, String apiUrl) {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                Gson gson = new Gson();
                String json = gson.toJson(photoData);

                // Вариант 1:
                RequestBody body = new FormBody.Builder()
                        .add("data", json)
                        .build();

                // Или Вариант 2:
                // MediaType JSON = MediaType.get("application/json; charset=utf-8");
                // RequestBody body = RequestBody.create(json, JSON);

                // Или Вариант 3:
                // RequestBody body = new FormBody.Builder()
                //     .add("data", json)
                //     .build();

                Request request = new Request.Builder()
                        .url(apiUrl)
                        .post(body)
                        .build();

                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    Log.d(TAG, "Photo sent successfully");
                } else {
                    Log.e(TAG, "Failed to send photo: " + response.code());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error sending photo: " + e.getMessage());
            }
        }).start();
    }

    public void sendRoadData(RoadData roadData, String apiUrl) {
        new Thread(() -> {
            try {
                String jsonData = String.format(Locale.US,
                        "{\"roadName\":\"%s\",\"placeId\":\"%s\",\"congestionLevel\":\"%s\"," +
                                "\"deviation\":%.2f,\"rating\":%.1f,\"photoCount\":%d," +
                                "\"latitude\":%.6f,\"longitude\":%.6f}",
                        roadData.getStreetName(),
                        roadData.getPlaceId(),
                        roadData.getCongestionLevel(),
                        roadData.getDeviation(),
                        roadData.getRating(),
                        roadData.getPhotoCount(),
                        roadData.getLatitude(),  // ДОБАВЛЯЕМ КООРДИНАТЫ
                        roadData.getLongitude()  // ДОБАВЛЯЕМ КООРДИНАТЫ
                );

                Log.d(TAG, "Проверка данных: " +
                        "streetName=" + roadData.getStreetName() +
                        ", placeId=" + roadData.getPlaceId() +
                        ", congestionLevel=" + roadData.getCongestionLevel() +
                        ", deviation=" + roadData.getDeviation() +
                        ", rating=" + roadData.getRating() +
                        ", photoCount=" + roadData.getPhotoCount() +
                        ", latitude=" + roadData.getLatitude() +
                        ", longitude=" + roadData.getLongitude());

                Log.d(TAG, "📡 Отправка JSON: " + jsonData);

                // ИСПОЛЬЗУЕМ ПЕРЕДАННЫЙ URL, а не константу
                URL url = new URL(apiUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonData.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "✅ Код ответа: " + responseCode);

                connection.disconnect();

            } catch (Exception e) {
                Log.e(TAG, "❌ Ошибка отправки: " + e.getMessage());
            }
        }).start();
    }
}
