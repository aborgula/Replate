package com.example.replate;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.material.slider.Slider;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ChooseLocationRangeActivity extends AppCompatActivity implements OnMapReadyCallback {

    private PlacesClient placesClient;
    private AutoCompleteTextView searchCity;
    private CardView showResultBtn;
    private MapView mapView;
    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private ImageView overlayImageView;
    private Slider distanceSlider;
    private int currentRadiusMeters = 5000; // domyślnie 5 km
    private ImageView centerPin;

    private static final String MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_choose_location_range);

        searchCity = findViewById(R.id.searchcity);
        showResultBtn = findViewById(R.id.showresultbtn);
        mapView = findViewById(R.id.mapView);
        distanceSlider = findViewById(R.id.distanceSlider);

        ImageButton closebtn = findViewById(R.id.closebtn);
        closebtn.setOnClickListener(v -> finish());

        // Inicjalizacja Places API
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "AIzaSyAwKhbg7_QX7gYKwuLRuaLYf6GXz2lAu0E");
        }
        placesClient = Places.createClient(this);

        // MapView
        Bundle mapViewBundle = savedInstanceState != null ? savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY) : null;
        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);

        // Overlay ImageView (dodaj do layoutu ponad MapView)
     //   overlayImageView = new ImageView(this);
        //overlayImageView.setScaleType(ImageView.ScaleType.FIT_XY);
      // addContentView(overlayImageView, mapView.getLayoutParams());
        overlayImageView = findViewById(R.id.overlayImageView);
        centerPin = findViewById(R.id.centerPin);


        // Autocomplete TextWatcher
        searchCity.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() >= 2) {
                    FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                            .setQuery(s.toString())
                            .build();

                    placesClient.findAutocompletePredictions(request).addOnSuccessListener(response -> {
                        List<String> suggestionList = new ArrayList<>();
                        List<AutocompletePrediction> predictions = response.getAutocompletePredictions();

                        for (AutocompletePrediction prediction : predictions) {
                            suggestionList.add(prediction.getFullText(null).toString());
                        }

                        if (!predictions.isEmpty()) {
                            String placeId = predictions.get(0).getPlaceId();

                            FetchPlaceRequest placeRequest = FetchPlaceRequest.builder(
                                    placeId,
                                    Arrays.asList(Place.Field.LAT_LNG, Place.Field.NAME)
                            ).build();

                            placesClient.fetchPlace(placeRequest).addOnSuccessListener(resp -> {
                                Place place = resp.getPlace();
                                if (place.getLatLng() != null && googleMap != null) {
                                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(), 12f));
                                }
                            });
                        }

                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                ChooseLocationRangeActivity.this,
                                android.R.layout.simple_dropdown_item_1line,
                                suggestionList
                        );
                        searchCity.setAdapter(adapter);
                        adapter.notifyDataSetChanged();
                    }).addOnFailureListener(e -> Log.e("PlacesAPI", "Autocomplete error", e));
                }
            }
        });

        // Obecna lokalizacja
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        TextView useCurrentLocation = findViewById(R.id.usecurrentlocation);
        useCurrentLocation.setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
                return;
            }

            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null && googleMap != null) {
                    LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 12f));

                    // Odwrócone geokodowanie
                    Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                    try {
                        List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                        if (addresses != null && !addresses.isEmpty()) {
                            Address address = addresses.get(0);
                            String fullAddress = "";
                            if (address.getThoroughfare() != null) fullAddress += address.getThoroughfare();
                            if (address.getLocality() != null) {
                                if (!fullAddress.isEmpty()) fullAddress += ", ";
                                fullAddress += address.getLocality();
                            }
                            if (!fullAddress.isEmpty()) searchCity.setText(fullAddress);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        });

        // Distance Slider
        SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        int savedKm = prefs.getInt("selected_distance_km", 5);
        distanceSlider.setValue(savedKm);
        currentRadiusMeters = savedKm * 1000;

        distanceSlider.setTrackActiveTintList(ContextCompat.getColorStateList(this, R.color.green));
        distanceSlider.setTrackInactiveTintList(ColorStateList.valueOf(Color.LTGRAY));
        distanceSlider.setThumbTintList(ContextCompat.getColorStateList(this, R.color.black));
        distanceSlider.setHaloTintList(ContextCompat.getColorStateList(this, R.color.green));
        distanceSlider.setLabelFormatter(value -> ((int) value) + " km");

        distanceSlider.addOnChangeListener((slider, value, fromUser) -> {
            currentRadiusMeters = Math.round(value) * 1000;
            updateOverlay();
        });

        // Show Results Button
        showResultBtn.setOnClickListener(v -> {
            if (googleMap != null) {
                LatLng centerLocation = googleMap.getCameraPosition().target;
                int selectedKm = Math.round(distanceSlider.getValue());

                SharedPreferences.Editor editor = prefs.edit();
                editor.putLong("lastLat", Double.doubleToLongBits(centerLocation.latitude));
                editor.putLong("lastLng", Double.doubleToLongBits(centerLocation.longitude));
                editor.putInt("selected_distance_km", selectedKm);

                // Odwrócone geokodowanie, aby zapisać nazwę miejsca dla pina
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                try {
                    List<Address> addresses = geocoder.getFromLocation(centerLocation.latitude, centerLocation.longitude, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        String fullAddress = "";
                        if (address.getThoroughfare() != null) fullAddress += address.getThoroughfare();
                        if (address.getLocality() != null) {
                            if (!fullAddress.isEmpty()) fullAddress += ", ";
                            fullAddress += address.getLocality();
                        }
                        if (!fullAddress.isEmpty()) {
                            editor.putString("lastPlaceName", fullAddress);
                            Log.d("PinLocation", "Pin place name: " + fullAddress);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                editor.apply();

                startActivity(new Intent(ChooseLocationRangeActivity.this, HomeActivity.class));
                finish();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap map) {
        this.googleMap = map;

        SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);

        // Załaduj zapisaną lokalizację
        long lastLatBits = prefs.getLong("lastLat", 0);
        long lastLngBits = prefs.getLong("lastLng", 0);

        if (lastLatBits != 0 && lastLngBits != 0) {
            double lat = Double.longBitsToDouble(lastLatBits);
            double lng = Double.longBitsToDouble(lastLngBits);
            LatLng savedLocation = new LatLng(lat, lng);
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(savedLocation, 12f));
        } else {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(52.2297, 21.0122), 12f));
        }

        // Listener do aktualizacji overlay przy przesuwaniu mapy
        map.setOnCameraIdleListener(this::updateOverlay);

        googleMap.setOnCameraMoveStartedListener(reason -> {
            centerPin.animate().translationY(-20f).setDuration(150).start();
        });
        googleMap.setOnCameraIdleListener(() -> {
            centerPin.animate().translationY(0f).setDuration(150).start();
            updateOverlay();
        });


        // Załaduj paczki z Firestore i pokaż jako małe zielone kółka
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        db.collection("parcels")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d("Firestore", "Total parcels fetched: " + queryDocumentSnapshots.size());
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String placeId = document.getString("placeId");
                        String title = document.getString("title");
                        String userId = document.getString("userId");

                        Log.d("Firestore", "Parcel: title=" + title + ", userId=" + userId + ", placeId=" + placeId);

                        if (currentUser != null && currentUser.getUid().equals(userId)) {
                            Log.d("Firestore", "Skipping own parcel: " + title);
                            continue;
                        }

                        if (placeId != null && !placeId.isEmpty()) {
                            Log.d("Firestore", "Adding parcel to map: " + title);
                            fetchPlaceAndAddCircle(placeId, title);
                        } else {
                            Log.d("Firestore", "No placeId, skipping parcel: " + title);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error loading parcels", e));

// Pierwszy overlay
        updateOverlay();
    }

    private void updateOverlay() {
        if (mapView == null || mapView.getWidth() == 0 || mapView.getHeight() == 0) return;

        int width = mapView.getWidth();
        int height = mapView.getHeight();

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint fillPaint = new Paint();
        fillPaint.setColor(Color.parseColor("#66000000"));
        fillPaint.setStyle(Paint.Style.FILL);
        canvas.drawRect(0, 0, width, height, fillPaint);

        Paint clearPaint = new Paint();
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        clearPaint.setAntiAlias(true);

        if (googleMap != null) {
            LatLng center = googleMap.getCameraPosition().target;
            float zoomLevel = googleMap.getCameraPosition().zoom;

            // Przybliżone przeliczenie metrów na piksele
            double metersPerPixel = 156543.03392 * Math.cos(Math.toRadians(center.latitude)) / Math.pow(2, zoomLevel);
            float radiusInPixels = (float) (currentRadiusMeters / metersPerPixel) * 2;


            // Narysuj przezroczysty okrąg w centrum ekranu
            float centerX = width / 2f;
            float centerY = height / 2f;
            canvas.drawCircle(centerX, centerY, radiusInPixels, clearPaint);
        }

        overlayImageView.setImageBitmap(bitmap);
    }


    private void fetchPlaceAndAddCircle(String placeId, String title) {
        List<Place.Field> placeFields = Arrays.asList(Place.Field.LAT_LNG, Place.Field.NAME);

        FetchPlaceRequest request = FetchPlaceRequest.builder(placeId, placeFields).build();
        placesClient.fetchPlace(request)
                .addOnSuccessListener(response -> {
                    Place place = response.getPlace();
                    LatLng location = place.getLatLng();
                    if (location != null && googleMap != null) {
                        googleMap.addCircle(new CircleOptions()
                                .center(location)
                                .radius(20)
                                .fillColor(Color.parseColor("#006400"))
                                .strokeWidth(1f));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("PlacesAPI", "Failed to fetch place for parcel: " + placeId, e);
                });
    }


    // MapView Lifecycle
    @Override protected void onResume() { super.onResume(); mapView.onResume(); }
    @Override protected void onStart() { super.onStart(); mapView.onStart(); }
    @Override protected void onStop() { mapView.onStop(); super.onStop(); }
    @Override protected void onPause() { mapView.onPause(); super.onPause(); }
    @Override protected void onDestroy() { mapView.onDestroy(); super.onDestroy(); }
    @Override protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Bundle mapViewBundle = new Bundle();
        mapView.onSaveInstanceState(mapViewBundle);
        outState.putBundle(MAP_VIEW_BUNDLE_KEY, mapViewBundle);
    }
}
