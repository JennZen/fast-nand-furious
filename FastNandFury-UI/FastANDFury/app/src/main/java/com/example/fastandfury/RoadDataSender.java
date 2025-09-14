package com.example.fastandfury;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;

import org.chromium.net.CronetException;
import org.chromium.net.UrlRequest;
import org.chromium.net.UrlResponseInfo;

import java.nio.ByteBuffer;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class RoadDataSender {
    private final org.chromium.net.CronetEngine cronetEngine;
    private final Executor executor;
    private final Gson gson;

    public RoadDataSender(Context context) {
        this.cronetEngine = new org.chromium.net.CronetEngine.Builder(context)
                .enableHttp2(true)
                .enableQuic(true)
                .enableBrotli(true)
                .build();
        this.executor = Executors.newSingleThreadExecutor();
        this.gson = new Gson();
    }

    public void sendRoadData(RoadData roadData, String apiUrl) {
        try {
            // Преобразуем данные в JSON
            RoadDataRequest requestPayload = new RoadDataRequest(
                    roadData.getStreetName(),
                    roadData.getPlaceId(),
                    roadData.getCongestionLevel(),
                    roadData.getDeviation(),
                    roadData.getRating(),
                    roadData.getPhotoCount()
            );

            String jsonData = gson.toJson(requestPayload);
            byte[] postData = jsonData.getBytes("UTF-8");

            // Создаем запрос
            UrlRequest.Builder requestBuilder = cronetEngine.newUrlRequestBuilder(
                    apiUrl,
                    new RoadDataCallback(),
                    executor
            );

            // Устанавливаем метод POST и заголовки
            requestBuilder.setHttpMethod("POST");
            requestBuilder.addHeader("Content-Type", "application/json");
            requestBuilder.addHeader("Accept", "application/json");

            // Создаем и запускаем запрос
            UrlRequest request = requestBuilder.build();

            // Начинаем запрос
            request.start();

            Log.d("RoadDataSender", "Запрос запущен: " + jsonData);

        } catch (Exception e) {
            Log.e("RoadDataSender", "Ошибка отправки данных: " + e.toString());
        }
    }

    // Класс для сериализации в JSON
    public static class RoadDataRequest {
        public String roadName;
        public String placeId;
        public String congestionLevel;
        public double deviation;
        public double rating;
        public int photo;

        public RoadDataRequest(String roadName, String placeId, String congestionLevel,
                               double deviation, double rating, int photo) {
            this.roadName = roadName;
            this.placeId = placeId;
            this.congestionLevel = congestionLevel;
            this.deviation = deviation;
            this.rating = rating;
            this.photo = photo;
        }
    }

    // Callback для обработки ответа
    public class RoadDataCallback extends UrlRequest.Callback {
        private static final String TAG = "RoadDataCallback";
        private final StringBuilder responseBuilder = new StringBuilder();

        @Override
        public void onRedirectReceived(UrlRequest request, UrlResponseInfo info, String newLocationUrl) {
            Log.d(TAG, "Redirect received: " + newLocationUrl);
            request.followRedirect();
        }

        @Override
        public void onResponseStarted(UrlRequest request, UrlResponseInfo info) {
            Log.d(TAG, "Response started. Status: " + info.getHttpStatusCode());
            // Начинаем читать ответ
            request.read(ByteBuffer.allocateDirect(1024));
        }

        @Override
        public void onReadCompleted(UrlRequest request, UrlResponseInfo info, ByteBuffer byteBuffer) {
            Log.d(TAG, "Read completed");

            // Читаем данные из buffer
            byteBuffer.flip();
            byte[] bytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(bytes);
            responseBuilder.append(new String(bytes));

            byteBuffer.clear();
            request.read(byteBuffer);
        }

        @Override
        public void onSucceeded(UrlRequest request, UrlResponseInfo info) {
            String response = responseBuilder.toString();
            Log.d(TAG, "Request succeeded. Status: " + info.getHttpStatusCode() +
                    " Response: " + response);
        }

        @Override
        public void onFailed(UrlRequest request, UrlResponseInfo info, CronetException error) {
            Log.e(TAG, "Request failed: " + error.getMessage());
        }

        @Override
        public void onCanceled(UrlRequest request, UrlResponseInfo info) {
            Log.d(TAG, "Request canceled");
        }
    }
}