package com.example.fastandfury;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

//import com.example.fastandfury.dto.RoadRequestDto;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.PlaceLikelihood;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;


import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;
import com.example.fastandfury.SimpleDataSender;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String PREFS_ROAD_DATA = "RoadData";
    private static final String KEY_PREFIX_ROAD = "road_";
    private PlacesClient placesClient;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private static final int CAMERA_REQUEST_CODE = 101;
    private static final int GALLERY_REQUEST_CODE = 102;

    private static final int DATA_COLLECTION_INTERVAL = 5000;

    private final Handler dataCollectionHandler = new Handler();
    private boolean isCollecting = false;

    private final List<RoadData> roadDataList = new ArrayList<>();
    private FusedLocationProviderClient fusedLocationClient;

    private GoogleMap gMap;
    private EditText searchEditText;
    private ImageButton searchButton;
    private ImageButton backButton;
    private ImageButton myLocationButton;
    private ImageButton trafficButton;
    private ImageButton photoButton;
    private Button showDataButton;
    private Button btnClearData;
    private SimpleDataSender dataSender;
    private boolean isTrafficEnabled = false;
    private LatLng currentPhotoLocation;

    private final Runnable dataCollectionRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isCollecting) return;
            if (gMap != null) collectRoadData();
            dataCollectionHandler.postDelayed(this, DATA_COLLECTION_INTERVAL);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        initPlaces();
        // Инициализируем сенсоры (один раз)
        SensorManagerHelper.getInstance(this);


        // 1) СНАЧАЛА найдём все View
        searchEditText   = findViewById(R.id.searchEditText);
        searchButton     = findViewById(R.id.searchButton);
        backButton       = findViewById(R.id.backButton);
        trafficButton    = findViewById(R.id.trafficButton);
        myLocationButton = findViewById(R.id.myLocationButton);
        photoButton      = findViewById(R.id.photoButton);
        showDataButton   = findViewById(R.id.showDataButton);
        btnClearData     = findViewById(R.id.btnClearData); // может быть null, если кнопки нет в XML
        dataSender = new SimpleDataSender();
        // ← ДОБАВЬ ЭТУ СТРОЧКУ

        // 2) ТЕПЕРЬ можно работать с ними
        if (trafficButton != null) {
            trafficButton.setAlpha(isTrafficEnabled ? 1f : 0.5f);
        }

        // Карта
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        // Location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Обработчики
        if (searchButton != null) searchButton.setOnClickListener(v -> searchLocationImproved());
        if (backButton != null) backButton.setOnClickListener(v -> finish());
        if (myLocationButton != null) myLocationButton.setOnClickListener(v -> getMyLocation());
        if (trafficButton != null) trafficButton.setOnClickListener(v -> toggleTraffic());
        if (photoButton != null) photoButton.setOnClickListener(v -> showPhotoDialogSimple());
        if (showDataButton != null) showDataButton.setOnClickListener(v -> showCollectedDataDialog());

        // btnClearData может отсутствовать в разметке — учтём это
        if (btnClearData != null) {
            btnClearData.setOnClickListener(v -> {
                stopDataCollection();
                SharedPreferences prefs = getSharedPreferences(PREFS_ROAD_DATA, MODE_PRIVATE);
                prefs.edit().clear().apply();
                roadDataList.clear();
                Toast.makeText(this, "Все данные очищены!", Toast.LENGTH_SHORT).show();
            });
        }

        // Разрешения и старт сбора
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startDataCollection();
        } else {
            requestLocationPermission();
        }
    }
    private void initPlaces() {
        if (!Places.isInitialized()) {
            String apiKey = getApiKeyFromManifest();
            if (apiKey == null || apiKey.isEmpty()) {
                Toast.makeText(this, "Не найден API ключ для Places", Toast.LENGTH_LONG).show();
                return;
            }
            Places.initialize(getApplicationContext(), apiKey);
        }
        placesClient = Places.createClient(this);
    }

    private String getApiKeyFromManifest() {
        try {
            android.content.pm.ApplicationInfo ai =
                    getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            if (ai.metaData == null) return null;
            return ai.metaData.getString("com.google.android.geo.API_KEY", null);
        } catch (Exception e) {
            return null;
        }
    }

    private void toggleTraffic() {
        isTrafficEnabled = !isTrafficEnabled;

        if (gMap != null) gMap.setTrafficEnabled(isTrafficEnabled);
        if (trafficButton != null) trafficButton.setAlpha(isTrafficEnabled ? 1f : 0.5f);

        Toast.makeText(this,
                isTrafficEnabled ? "Пробки включены" : "Пробки выключены",
                Toast.LENGTH_SHORT).show();
    }

    private void startDataCollection() {
        if (isCollecting) return;
        isCollecting = true;
        dataCollectionHandler.postDelayed(dataCollectionRunnable, DATA_COLLECTION_INTERVAL);
    }

    private void stopDataCollection() {
        isCollecting = false;
        dataCollectionHandler.removeCallbacks(dataCollectionRunnable);
    }

    private void collectRoadData() {
        Log.d("MapsActivity", "🔍 collectRoadData вызван");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("MapsActivity", "❌ Нет разрешения на геолокацию");
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location == null) {
                Log.d("MapsActivity", "❌ Location is null");
                return;
            }

            LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
            double deviation = SensorManagerHelper.getInstance(this).getCurrentDeviation();

            Log.d("MapsActivity", "📊 Deviation: " + deviation);

            if (deviation > 2.0) {
                Log.d("MapsActivity", "🐛 Яма обнаружена! Отклонение: " + deviation);

                String streetName = "Неизвестная улица";
                try {
                    Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                    List<Address> addresses = geocoder.getFromLocation(
                            currentLocation.latitude, currentLocation.longitude, 1
                    );
                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        streetName = address.getThoroughfare();
                        if (streetName == null) streetName = address.getFeatureName();
                        if (streetName == null && address.getAddressLine(0) != null) {
                            streetName = address.getAddressLine(0);
                        }
                    }
                } catch (Exception ignored) { }

                // ОТПРАВЛЯЕМ ДАННЫЕ ТОЛЬКО ЕСЛИ НАШЛИ ЯМУ
                saveWithRealPlaceId(currentLocation, streetName, deviation);

                Toast.makeText(this, "🐛 Яма обнаружена! Отклонение: " + String.format(Locale.US, "%.2f", deviation),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void saveWithRealPlaceId(LatLng currentLocation, String streetName, double deviation) {
        if (placesClient == null ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            persistRoadData(currentLocation, streetName, deviation, null);
            return;
        }

        java.util.List<Place.Field> fields = java.util.Arrays.asList(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG
        );

        FindCurrentPlaceRequest request = FindCurrentPlaceRequest.newInstance(fields);

        placesClient.findCurrentPlace(request)
                .addOnSuccessListener(response -> {
                    String placeId = null;
                    if (response != null && response.getPlaceLikelihoods() != null
                            && !response.getPlaceLikelihoods().isEmpty()) {
                        PlaceLikelihood best = response.getPlaceLikelihoods().get(0);
                        if (best != null && best.getPlace() != null) {
                            placeId = best.getPlace().getId();
                        }
                    }
                    persistRoadData(currentLocation, streetName, deviation, placeId);
                })
                .addOnFailureListener(e -> {
                    persistRoadData(currentLocation, streetName, deviation, null);
                });
    }

    private void persistRoadData(LatLng currentLocation, String streetName,
                                 double deviation, String placeIdOrNull) {
        String placeId = (placeIdOrNull != null && !placeIdOrNull.isEmpty())
                ? placeIdOrNull
                : ("place_" + System.currentTimeMillis());

        String congestionLevel;
        if (deviation < 1.0) {
            congestionLevel = "свободно";
        } else if (deviation < 2.0) {
            congestionLevel = "умеренно";
        } else {
            congestionLevel = "плохая";
        }

        RoadData roadData = new RoadData(
                streetName,
                placeId,
                currentLocation.latitude,
                currentLocation.longitude,
                congestionLevel,
                deviation,
                4.5,
                0
        );

        saveRoadData(roadData);

        if (deviation > 0.5) {
            Log.d("MapsActivity", "🚨 Отправка данных...");

            if (dataSender != null) {
                String apiUrl = "https://febd658e146b.ngrok-free.app/report-road/sent-report";
                dataSender.sendRoadData(roadData, apiUrl); // ← ДОБАВЬТЕ URL ЗДЕСЬ
                Log.d("MapsActivity", "✅ Данные отправлены на сервер");
            }
        }
    }

    private void saveRoadData(RoadData roadData) {
        SharedPreferences prefs = getSharedPreferences(PREFS_ROAD_DATA, MODE_PRIVATE);
        String key = KEY_PREFIX_ROAD + System.currentTimeMillis();
        String json = new Gson().toJson(roadData);
        prefs.edit().putString(key, json).apply();
    }

    public List<RoadData> loadAllRoadData() {
        SharedPreferences prefs = getSharedPreferences(PREFS_ROAD_DATA, MODE_PRIVATE);
        Map<String, ?> all = prefs.getAll();

        List<RoadData> dataList = new ArrayList<>();
        Gson gson = new Gson();
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            if (entry.getKey().startsWith(KEY_PREFIX_ROAD)) {
                try {
                    dataList.add(gson.fromJson(String.valueOf(entry.getValue()), RoadData.class));
                } catch (Exception ignored) { }
            }
        }
        return dataList;
    }

    private void showCollectedDataDialog() {
        List<RoadData> allData = loadAllRoadData();
        StringBuilder sb = new StringBuilder();
        for (RoadData data : allData) {
            sb.append(data.getStreetName())
                    .append(" (")
                    .append(String.format(Locale.US, "%.3f", data.getDeviation()))
                    .append(")\n");
        }
        if (sb.length() == 0) sb.append("Данные ещё не собраны");

        new AlertDialog.Builder(this)
                .setTitle("Собранные данные (" + allData.size() + ")")
                .setMessage(sb.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    private void showPhotoDialogSimple() {
        new AlertDialog.Builder(this)
                .setTitle("Добавить фото")
                .setItems(new String[]{"Камера", "Галерея"}, (dialog, which) -> {
                    if (which == 0) takePhotoFromCameraSimple();
                    else choosePhotoFromGallerySimple();
                })
                .show();
    }

    private void takePhotoFromCameraSimple() {
        try {
            if (gMap != null) currentPhotoLocation = gMap.getCameraPosition().target;
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE);
            } else {
                Toast.makeText(this, "Камера не доступна", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка открытия камеры", Toast.LENGTH_SHORT).show();
        }
    }

    private void choosePhotoFromGallerySimple() {
        try {
            if (gMap != null) currentPhotoLocation = gMap.getCameraPosition().target;
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent, GALLERY_REQUEST_CODE);
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка открытия галереи", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || gMap == null || currentPhotoLocation == null) return;

        if (requestCode == CAMERA_REQUEST_CODE && data != null) {
            Bitmap imageBitmap = (Bitmap) (data.getExtras() != null ? data.getExtras().get("data") : null);
            addPhotoMarker(imageBitmap, currentPhotoLocation);
        } else if (requestCode == GALLERY_REQUEST_CODE && data != null) {
            try (InputStream inputStream = getContentResolver().openInputStream(data.getData())) {
                Bitmap imageBitmap = BitmapFactory.decodeStream(inputStream);
                addPhotoMarker(imageBitmap, currentPhotoLocation);
            } catch (Exception e) {
                Toast.makeText(this, "Ошибка загрузки фото", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void addPhotoMarker(Bitmap photo, LatLng location) {
        if (gMap == null || photo == null || location == null) return;
        Bitmap smallPhoto = Bitmap.createScaledBitmap(photo, 150, 150, false);
        BitmapDescriptor icon = BitmapDescriptorFactory.fromBitmap(smallPhoto);
        gMap.addMarker(new MarkerOptions()
                .position(location)
                .icon(icon)
                .title("Фото")
                .snippet("Добавлено: " + new Date()));
        Toast.makeText(this, "Фото добавлено!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.gMap = googleMap;
        try {
            gMap.getUiSettings().setZoomControlsEnabled(true);
            gMap.getUiSettings().setMapToolbarEnabled(true);

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                enableLocationFeatures();
            }

            LatLng mapMoldova = new LatLng(47.02278, 28.83528);
            gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mapMoldova, 10f));
            gMap.addMarker(new MarkerOptions().position(mapMoldova).title("Chișinău"));

            Toast.makeText(this, "Карта загружена!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка инициализации карты", Toast.LENGTH_LONG).show();
        }
    }

    private void enableLocationFeatures() {
        try {
            if (gMap != null) {
                gMap.setMyLocationEnabled(true);
                gMap.getUiSettings().setMyLocationButtonEnabled(true);
            }
        } catch (SecurityException e) {
            Toast.makeText(this, "Ошибка доступа к местоположению", Toast.LENGTH_SHORT).show();
        }
    }

    private void getMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED && gMap != null) {

            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    LatLng myLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    gMap.clear();
                    gMap.addMarker(new MarkerOptions().position(myLocation).title("Я здесь!"));
                    gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 15f));
                    Toast.makeText(this, "Местоположение обновлено!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Не могу получить местоположение", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            requestLocationPermission();
        }
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableLocationFeatures();
                startDataCollection();
                getMyLocation();
            } else {
                Toast.makeText(this, "Разрешение на геолокацию отклонено", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void searchLocationImproved() {
        String searchText = searchEditText.getText().toString().trim();
        if (searchText.isEmpty()) {
            Toast.makeText(this, "Введите адрес для поиска", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Geocoder geocoder = new Geocoder(this);
            List<Address> addresses = geocoder.getFromLocationName(searchText, 1);
            if (addresses != null && !addresses.isEmpty() && gMap != null) {
                Address address = addresses.get(0);
                LatLng location = new LatLng(address.getLatitude(), address.getLongitude());
                gMap.clear();
                gMap.addMarker(new MarkerOptions()
                        .position(location)
                        .title(searchText)
                        .snippet(address.getAddressLine(0)));
                gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f));
                Toast.makeText(this, "Найдено: " + address.getAddressLine(0), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Место не найдено", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(this, "Ошибка поиска", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopDataCollection();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startDataCollection();
        }
    }
//    public static class SimpleDataSender {
//        private static final String TAG = "SimpleDataSender";
//
//        public void sendRoadData(RoadData roadData, String apiUrl) {
//            new Thread(() -> {
//                try {
//                    com.example.fastandfury.SimpleDataSender.sendRoadDat(roadData, apiUrl);
//                    Log.d(TAG, "Данные отправлены: " + roadData.getStreetName());
//                } catch (Exception e) {
//                    Log.e(TAG, "Ошибка: " + e.getMessage());
//                }
//            }).start();
//        }
//    }

}
