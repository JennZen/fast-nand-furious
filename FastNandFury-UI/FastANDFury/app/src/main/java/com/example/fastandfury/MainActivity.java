package com.example.fastandfury;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int STORAGE_PERMISSION_REQUEST_CODE = 1003;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private GoogleMap mMap;
    private ImageButton trafficButton;
    private ImageButton locationButton;
    private ImageButton placeholderButton;
    private View vibrationCircle;
    private ImageButton cameraButton;
    private ImageButton expandMapButton;

    private boolean isMonitoringVibration = false;
    private Handler vibrationHandler;
    private Runnable vibrationRunnable;

    private FusedLocationProviderClient fusedLocationClient;
    private SimpleDataSender dataSender;
    private static final String PREFS_ROAD_DATA = "RoadData";
    private static final String KEY_PREFIX_ROAD = "road_";

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    handleGalleryResult(result.getData());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        SensorManagerHelper.getInstance(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        dataSender = new SimpleDataSender();

        // ИНИЦИАЛИЗАЦИЯ ВСЕХ КНОПОК
        trafficButton = findViewById(R.id.traffic_button);
        locationButton = findViewById(R.id.location_button);
        placeholderButton = findViewById(R.id.placeholder_button);
        vibrationCircle = findViewById(R.id.vibration_circle);
        cameraButton = findViewById(R.id.camera_button);
        expandMapButton = findViewById(R.id.expand_map_button);

        checkLocationPermission();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        vibrationHandler = new Handler();
        setupButtonListeners();
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(this, "Need storage permission for photos", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LatLng chisinau = new LatLng(47.02278, 28.83528);
        mMap.addMarker(new MarkerOptions().position(chisinau).title("Chișinău"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(chisinau, 12f));

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);

        Toast.makeText(this, "Карта загружена!", Toast.LENGTH_SHORT).show();
    }

    private void setupButtonListeners() {
        trafficButton.setOnClickListener(v -> {
            if (mMap != null) {
                boolean isTrafficEnabled = !mMap.isTrafficEnabled();
                mMap.setTrafficEnabled(isTrafficEnabled);
                Toast.makeText(this, isTrafficEnabled ? "Traffic enabled" : "Traffic disabled",
                        Toast.LENGTH_SHORT).show();
            }
        });

        locationButton.setOnClickListener(v -> {
            if (mMap != null) {
                mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
                Toast.makeText(this, "Zoom to location", Toast.LENGTH_SHORT).show();
            }
        });

        placeholderButton.setOnClickListener(v -> {
            Toast.makeText(this, "Placeholder button", Toast.LENGTH_SHORT).show();
        });

        vibrationCircle.setOnClickListener(v -> {
            toggleVibrationMonitoring();
        });

        cameraButton.setOnClickListener(v -> {
            checkStoragePermission();
        });

        expandMapButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MapsActivity.class);
            startActivity(intent);
        });
    }

    private void toggleVibrationMonitoring() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission required for monitoring", Toast.LENGTH_SHORT).show();
            checkLocationPermission();
            return;
        }

        isMonitoringVibration = !isMonitoringVibration;

        if (isMonitoringVibration) {
            enableLocationFeatures();
            startVibrationMonitoring();
            Toast.makeText(this, "Vibration monitoring STARTED", Toast.LENGTH_SHORT).show();
        } else {
            stopVibrationMonitoring();
            Toast.makeText(this, "Vibration monitoring STOPPED", Toast.LENGTH_SHORT).show();
        }

        updateVibrationCircleAppearance();
    }

    private void enableLocationFeatures() {
        try {
            if (mMap != null) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);

                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                        if (location != null) {
                            LatLng myLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 15f));
                        }
                    });
                }
            }
        } catch (SecurityException e) {
            Toast.makeText(this, "Location access error", Toast.LENGTH_SHORT).show();
        }
    }

    private void startVibrationMonitoring() {
        vibrationRunnable = new Runnable() {
            @Override
            public void run() {
                if (isMonitoringVibration) {
                    collectRoadData();
                    vibrationHandler.postDelayed(this, 5000);
                }
            }
        };
        vibrationHandler.post(vibrationRunnable);
    }

    private void stopVibrationMonitoring() {
        if (vibrationRunnable != null) {
            vibrationHandler.removeCallbacks(vibrationRunnable);
        }
        isMonitoringVibration = false;
    }

    private void checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_REQUEST_CODE
            );
        } else {
            openGallery();
        }
    }

    private void openGallery() {
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            galleryLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Error opening gallery", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleGalleryResult(Intent data) {
        try {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                Bitmap imageBitmap = BitmapFactory.decodeStream(inputStream);

                if (imageBitmap != null) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                            if (location != null) {
                                LatLng photoLocation = new LatLng(location.getLatitude(), location.getLongitude());
                                processAndSendPhoto(imageBitmap, photoLocation);
                            }
                        });
                    }
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
        }
    }

    private void processAndSendPhoto(Bitmap photo, LatLng location) {
        String photoBase64 = convertBitmapToBase64(photo);

        if (photoBase64 != null) {
            String address = getAddressFromLocation(location);

            PhotoData photoData = new PhotoData(
                    address,
                    location.latitude,
                    location.longitude,
                    photoBase64,
                    System.currentTimeMillis()
            );

            sendPhotoToServer(photoData);
            Toast.makeText(this, "Photo sent to server!", Toast.LENGTH_SHORT).show();
        }
    }

    private String convertBitmapToBase64(Bitmap bitmap) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            return Base64.encodeToString(byteArray, Base64.DEFAULT);
        } catch (Exception e) {
            return null;
        }
    }

    private String getAddressFromLocation(LatLng location) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(
                    location.latitude, location.longitude, 1
            );
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                if (address.getThoroughfare() != null) {
                    return address.getThoroughfare();
                } else if (address.getAddressLine(0) != null) {
                    return address.getAddressLine(0);
                }
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error getting address", e);
        }
        return "Unknown location";
    }

    private void sendPhotoToServer(PhotoData photoData) {
        if (photoData != null && dataSender != null) {
            String apiUrl = "https://febd658e146b.ngrok-free.app/report-photo/sent-photo";
            dataSender.sendPhotoData(photoData, apiUrl);
            Log.d("MainActivity", "✅ Photo sent to server: " + photoData.getAddress());
        }
    }

    private void collectRoadData() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
        ).addOnSuccessListener(location -> {
            if (location == null) return;

            LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
            double deviation = SensorManagerHelper.getInstance(this).getCurrentDeviation();

            if (mMap != null) {
                mMap.animateCamera(CameraUpdateFactory.newLatLng(currentLocation));
            }

            if (deviation > 1.0) {
                String streetName = "Unknown street";
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

                RoadData roadData = createRoadData(currentLocation, streetName, deviation);
                sendDataToServer(roadData);

                Toast.makeText(this, "🐛 Pothole detected! Deviation: " +
                        String.format(Locale.US, "%.2f", deviation), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private RoadData createRoadData(LatLng location, String streetName, double deviation) {
        String congestionLevel;
        if (deviation < 1.0) {
            congestionLevel = "free";
        } else if (deviation < 2.0) {
            congestionLevel = "moderate";
        } else {
            congestionLevel = "bad";
        }

        return new RoadData(
                streetName,
                "place_" + System.currentTimeMillis(),
                location.latitude,
                location.longitude,
                congestionLevel,
                deviation,
                4.5,
                0
        );
    }

    private void sendDataToServer(RoadData roadData) {
        if (roadData != null && dataSender != null) {
            String apiUrl = "https://febd658e146b.ngrok-free.app/report-road/sent-report";
            dataSender.sendRoadData(roadData, apiUrl);
            saveRoadData(roadData);
        }
    }

    private void saveRoadData(RoadData roadData) {
        SharedPreferences prefs = getSharedPreferences(PREFS_ROAD_DATA, MODE_PRIVATE);
        String key = KEY_PREFIX_ROAD + System.currentTimeMillis();
        String json = new Gson().toJson(roadData);
        prefs.edit().putString(key, json).apply();
    }

    private void updateVibrationCircleAppearance() {
        if (isMonitoringVibration) {
            vibrationCircle.setBackgroundResource(R.drawable.circle_background_active);
        } else {
            vibrationCircle.setBackgroundResource(R.drawable.circle_background);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopVibrationMonitoring();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateVibrationCircleAppearance();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (vibrationHandler != null) {
            vibrationHandler.removeCallbacksAndMessages(null);
        }
    }
}