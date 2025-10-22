package com.example.replate;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.gms.maps.model.LatLng;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class FragmentMap extends Fragment implements OnMapReadyCallback {

    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;
    private PlacesClient placesClient;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private List<String> currentSelectedChips = new ArrayList<>();
    private View layoutEmptyMap;
    private com.airbnb.lottie.LottieAnimationView emptyMapAnim;
    private SupportMapFragment mapFragment;



    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1; // Kod do żądania uprawnień

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Odczytaj selectedChips z SharedPreferences
        SharedPreferences prefs = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        Set<String> selectedChipsSet = prefs.getStringSet("selectedChips", null);
        currentSelectedChips = selectedChipsSet != null ? new ArrayList<>(selectedChipsSet) : new ArrayList<>();

        if (googleMap != null) {
            googleMap.clear();
        }
        loadParcelsAndShowMarkers();
        reloadMarkers();

        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), "AIzaSyA7Mm6hV5yGvXd3sn8ZqM_z9nxcfxzc2y8");
        }

        placesClient = Places.createClient(requireContext());
      //  placesClient = MyApp.placesClient;

        layoutEmptyMap = view.findViewById(R.id.layoutEmptyMap);
        emptyMapAnim = view.findViewById(R.id.emptyMapAnim);

        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.map_fragment);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;

        googleMap.setOnMarkerClickListener(marker -> {
            String clickedParcelId = (String) marker.getTag();
            if (clickedParcelId != null) {
                Intent intent = new Intent(requireContext(), ParcelActivity.class);
                intent.putExtra("parcelId", clickedParcelId);
                startActivity(intent);
            }
            return false;
        });

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        // SharedPreferences
        String lastPlaceId = requireContext()
                .getSharedPreferences("MyPrefs", 0)
                .getString("lastPlaceId", null);

        if (lastPlaceId != null && !lastPlaceId.isEmpty()) {
            List<Place.Field> placeFields = Arrays.asList(Place.Field.LAT_LNG);
            FetchPlaceRequest request = FetchPlaceRequest.newInstance(lastPlaceId, placeFields);

            placesClient.fetchPlace(request).addOnSuccessListener(response -> {
                Place place = response.getPlace();
                LatLng latLng = place.getLatLng();
                if (latLng != null) {
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12));
                }
            }).addOnFailureListener(exception -> {
                Log.e("FragmentMap", "Failed to fetch lastPlaceId: " + exception.getMessage());
                fallbackToCurrentLocation();
            });
        } else {
            fallbackToCurrentLocation();
        }

        loadParcelsAndShowMarkers();  // 🔁 to ładuje paczki i przypisuje parcelId do markerów
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onMapReady(googleMap);
            } else {
                Toast.makeText(requireContext(), "Permission denied. Unable to access location", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadParcelsAndShowMarkers() {

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (googleMap == null) {
            Log.e("FragmentMap", "googleMap is null, cannot load parcels");
            return;
        }

        // SharedPreferences
        SharedPreferences prefs = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        String selectedDateTmp = prefs.getString("selectedDate", "");

        if (selectedDateTmp == null || selectedDateTmp.isEmpty()) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
            java.util.Calendar calendar = java.util.Calendar.getInstance();
            String todayDate = sdf.format(calendar.getTime());

            prefs.edit().putString("selectedDate", todayDate).apply();
            selectedDateTmp = todayDate;

            Log.d("FragmentMap", "selectedDate was empty, set to today: " + selectedDateTmp);
        } else {
            Log.d("FragmentMap", "selectedDate from prefs: " + selectedDateTmp);
        }

        final String finalSelectedDate = selectedDateTmp; // finalna zmienna do lambdy

        db.collection("parcels")
                .whereEqualTo("status", "active")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d("FragmentMap", "Fetched " + queryDocumentSnapshots.size() + " parcels from Firestore");

                    boolean hasParcels = false;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String parcelId = document.getId();
                        String creatorId = document.getString("userId");
                        Log.d("FragmentMap", "Processing parcelId=" + parcelId + ", creatorId=" + creatorId);

                        // Pomijamy paczki użytkownika
                        if (creatorId != null && creatorId.equals(currentUserId)) {
                            Log.d("FragmentMap", "Skipping own parcel: " + parcelId);
                            continue;
                        }

                        List<String> tags = (List<String>) document.get("tags");
                        if (tags == null) tags = new ArrayList<>();
                        Log.d("FragmentMap", "Parcel tags: " + tags);

                        boolean showParcel = currentSelectedChips == null || currentSelectedChips.isEmpty();
                        Log.d("FragmentMap", "Initial showParcel: " + showParcel);

                        // Filtrowanie po chipach
                        if (!showParcel) {
                            for (String tag : tags) {
                                if (currentSelectedChips.contains(tag)) {
                                    showParcel = true;
                                    Log.d("FragmentMap", "Parcel " + parcelId + " passed chip filter with tag: " + tag);
                                    break;
                                }
                            }
                        }

                        // Filtrowanie po dacie
                        if (showParcel) {
                            String parcelDate = document.getString("date"); // np. "2025-06-17"
                            Log.d("FragmentMap", "Parcel date: " + parcelDate + ", selectedDate: " + finalSelectedDate);

                            if (!finalSelectedDate.equals(parcelDate)) {
                                showParcel = false;
                                Log.d("FragmentMap", "Parcel " + parcelId + " filtered out by date");
                            } else {
                                Log.d("FragmentMap", "Parcel " + parcelId + " passed date filter");
                            }
                        }

                        // Filtrowanie po czasie
                        if (showParcel) {
                            int filterStart = prefs.getInt("startTimeMinutes", 0);       // od 00:00
                            int filterEnd = prefs.getInt("endTimeMinutes", 1439);        // do 23:59

                            String parcelStartStr = document.getString("startTime");     // np. "20:23"
                            String parcelEndStr = document.getString("endTime");         // np. "22:00"

                            if (parcelStartStr != null && parcelEndStr != null
                                    && parcelStartStr.matches("\\d{2}:\\d{2}") && parcelEndStr.matches("\\d{2}:\\d{2}")) {

                                int parcelStartMin = timeStringToMinutes(parcelStartStr);
                                int parcelEndMin = timeStringToMinutes(parcelEndStr);

                                if (parcelEndMin < filterStart || parcelStartMin > filterEnd) {
                                    showParcel = false;
                                    Log.d("FragmentMap", "Parcel " + parcelId + " filtered out by time: " + parcelStartStr + "-" + parcelEndStr);
                                } else {
                                    Log.d("FragmentMap", "Parcel " + parcelId + " passed time filter: " + parcelStartStr + "-" + parcelEndStr);
                                }
                            } else {
                                Log.d("FragmentMap", "Parcel " + parcelId + " has invalid time format");
                            }
                        }

                        if (showParcel) {
                            hasParcels = true;
                            String placeId = document.getString("placeId");
                            String title = document.getString("title");
                            Log.d("FragmentMap", "Parcel " + parcelId + " will be shown on map with placeId=" + placeId + " and title=" + title);

                            if (placeId != null && !placeId.isEmpty()) {
                                fetchPlaceAndAddMarker(placeId, title, parcelId);
                            } else {
                                Log.d("FragmentMap", "Parcel " + parcelId + " has null/empty placeId");
                            }
                        }
                    }

                    if (!hasParcels) {
                        Log.d("FragmentMap", "No parcels to show after filtering");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to load parcels: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("FragmentMap", "Failed to load parcels: " + e.getMessage());
                });
    }


    private void fetchPlaceAndAddMarker(String placeId, String title, String parcelId) {
        Log.d("FragmentMap", "fetchPlaceAndAddMarker called: placeId=" + placeId + ", title=" + title + ", parcelId=" + parcelId);

        List<Place.Field> placeFields = Arrays.asList(Place.Field.LAT_LNG);
        FetchPlaceRequest request = FetchPlaceRequest.newInstance(placeId, placeFields);

        placesClient.fetchPlace(request)
                .addOnSuccessListener(response -> {
                    Place place = response.getPlace();
                    LatLng latLng = place.getLatLng();
                    Log.d("FragmentMap", "Place fetched: " + place + ", latLng=" + latLng);

                    if (latLng != null && googleMap != null) {
                        Marker marker = googleMap.addMarker(new MarkerOptions().position(latLng).title(title));
                        if (marker != null) {
                            Log.d("FragmentMap", "Marker added for parcelId=" + parcelId + " at " + latLng);
                            marker.setTag(parcelId);
                        } else {
                            Log.e("FragmentMap", "Failed to add marker for parcelId=" + parcelId);
                        }
                    } else {
                        Log.e("FragmentMap", "LatLng is null or googleMap is null for parcelId=" + parcelId);
                    }
                })
                .addOnFailureListener(exception -> {
                    Log.e("FragmentMap", "Place not found for placeId=" + placeId + ": " + exception.getMessage());
                });
    }


    private void fallbackToCurrentLocation() {
        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(requireActivity(), location -> {
                        if (location != null) {
                            LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            googleMap.addMarker(new MarkerOptions().position(currentLocation).title("Your Location"));
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 12));
                        } else {
                            Toast.makeText(requireContext(), "Unable to fetch location", Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (SecurityException e) {
            Toast.makeText(requireContext(), "Location permission not granted", Toast.LENGTH_SHORT).show();
            Log.e("FragmentMap", "SecurityException: " + e.getMessage());
        }
    }


    private int timeStringToMinutes(String timeStr) {
        String[] parts = timeStr.split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);
        return hour * 60 + minute;
    }

    public void reloadMarkers() {
        // Wczytaj selectedChips z prefs ponownie
        SharedPreferences prefs = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        Set<String> selectedChipsSet = prefs.getStringSet("selectedChips", null);
        Log.d("selectedtags", "w reloadmarkers w fragmentmap " + selectedChipsSet);
        currentSelectedChips = selectedChipsSet != null ? new ArrayList<>(selectedChipsSet) : new ArrayList<>();

        if (googleMap != null) {
            googleMap.clear();
            loadParcelsAndShowMarkers();
        }
    }

}
