package com.example.replate;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
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
import com.google.android.gms.tasks.Task;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChooseLocationRangeActivity extends AppCompatActivity implements OnMapReadyCallback {

    private PlacesClient placesClient;
    private AutoCompleteTextView searchCity;
    private CardView showResultBtn;
    private MapView mapView;
    private GoogleMap googleMap;
    private String selectedPlaceId = null;
    private TextView usecurrentlocation;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private static final String MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_choose_location_range);

        searchCity = findViewById(R.id.searchcity);
        showResultBtn = findViewById(R.id.showresultbtn);
        mapView = findViewById(R.id.mapView);
        ImageButton closebtn = findViewById(R.id.closebtn);
        closebtn.setOnClickListener(v -> finish());

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "AIzaSyAwKhbg7_QX7gYKwuLRuaLYf6GXz2lAu0E");
        }
        placesClient = Places.createClient(this);

        // === MapView ===
        Bundle mapViewBundle = savedInstanceState != null ? savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY) : null;
        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);

        // === Autocomplete TextWatcher ===
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
                            selectedPlaceId = predictions.get(0).getPlaceId(); // zapamiętaj pierwszy
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

        // === Obecna lokalizacja
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        TextView useCurrentLocation = findViewById(R.id.usecurrentlocation);
        useCurrentLocation.setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
                return;
            }

            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null && googleMap != null) {
                    LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 12f));
                    googleMap.addMarker(new MarkerOptions().position(userLocation).title("You're here"));
                }
            });
        });


        // === Show Results Button ===
        showResultBtn.setOnClickListener(v -> {
            if (selectedPlaceId != null) {
                FetchPlaceRequest placeRequest = FetchPlaceRequest.builder(
                        selectedPlaceId,
                        Arrays.asList(Place.Field.LAT_LNG, Place.Field.NAME)
                ).build();

                placesClient.fetchPlace(placeRequest).addOnSuccessListener(response -> {
                    Place place = response.getPlace();
                    if (place.getLatLng() != null && googleMap != null) {
                        LatLng location = place.getLatLng();
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 12f));
                        googleMap.addMarker(new MarkerOptions().position(location).title(place.getName()));
                    }
                }).addOnFailureListener(e -> Log.e("PlacesAPI", "Place fetch failed", e));
            }
        });

        // === Distance Slider ===
        Slider distanceSlider = findViewById(R.id.distanceSlider);
        int savedKm = getSharedPreferences("replate_prefs", MODE_PRIVATE).getInt("selected_distance_km", 5);
        distanceSlider.setValue(savedKm);

        distanceSlider.setTrackActiveTintList(ContextCompat.getColorStateList(this, R.color.green));
        distanceSlider.setTrackInactiveTintList(ColorStateList.valueOf(Color.LTGRAY));
        distanceSlider.setThumbTintList(ContextCompat.getColorStateList(this, R.color.black));
        distanceSlider.setHaloTintList(ContextCompat.getColorStateList(this, R.color.green));
        distanceSlider.setTickTintList(ColorStateList.valueOf(Color.YELLOW));

        distanceSlider.setLabelFormatter(value -> ((int) value) + " km");

        distanceSlider.addOnChangeListener((slider, value, fromUser) -> {
            int selectedKm = Math.round(value);
            getSharedPreferences("replate_prefs", MODE_PRIVATE)
                    .edit()
                    .putInt("selected_distance_km", selectedKm)
                    .apply();
        });
    }

    // === MapView Lifecycle ===
    @Override public void onMapReady(GoogleMap map) { googleMap = map; }
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
