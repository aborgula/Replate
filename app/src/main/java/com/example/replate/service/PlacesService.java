package com.example.replate.service;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.Arrays;
import java.util.List;

public class PlacesService {

    private final PlacesClient placesClient;

    public PlacesService(PlacesClient placesClient) {
        this.placesClient = placesClient;
    }

    public interface OnPlaceFetchedListener {
        void onSuccess(LatLng latLng);
        void onFailure(Exception e);
    }

    public void fetchPlaceLatLng(String placeId, OnPlaceFetchedListener listener) {
        if (placeId == null || placeId.isEmpty()) {
            listener.onFailure(new IllegalArgumentException("PlaceId is null or empty"));
            return;
        }

        List<Place.Field> placeFields = Arrays.asList(Place.Field.LAT_LNG);
        FetchPlaceRequest request = FetchPlaceRequest.newInstance(placeId, placeFields);

        placesClient.fetchPlace(request)
                .addOnSuccessListener(response -> {
                    Place place = response.getPlace();
                    LatLng latLng = place.getLatLng();

                    if (latLng != null) {
                        listener.onSuccess(latLng);
                    } else {
                        listener.onFailure(new Exception("LatLng is null"));
                    }
                })
                .addOnFailureListener(listener::onFailure);
    }
}