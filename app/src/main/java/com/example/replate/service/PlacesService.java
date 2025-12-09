package com.example.replate.service;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PlacesService {

    private final PlacesClient placesClient;
    private AutocompleteSessionToken token;


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


    public static class AddressSuggestion {
        public String fullAddress;
        public String placeId;

        public AddressSuggestion(String fullAddress, String placeId) {
            this.fullAddress = fullAddress;
            this.placeId = placeId;
        }
    }

    public interface OnAddressSuggestionsListener {
        void onSuccess(List<AddressSuggestion> suggestions);

        void onFailure(Exception e);
    }

    /**
     * Wyszukuje sugestie adresów na podstawie zapytania
     */
    public void searchAddresses(String query, OnAddressSuggestionsListener listener) {
        if (query == null || query.isEmpty()) {
            listener.onSuccess(new ArrayList<>());
            return;
        }

        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setTypeFilter(TypeFilter.ADDRESS)
                .setSessionToken(token)
                .setQuery(query)
                .build();

        placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener(response -> {
                    List<AddressSuggestion> suggestions = new ArrayList<>();

                    for (AutocompletePrediction prediction : response.getAutocompletePredictions()) {
                        String fullAddress = prediction.getFullText(null).toString();
                        String placeId = prediction.getPlaceId();
                        suggestions.add(new AddressSuggestion(fullAddress, placeId));
                    }

                    listener.onSuccess(suggestions);
                })
                .addOnFailureListener(listener::onFailure);
    }

    /**
     * Waliduje czy adres zawiera numer domu
     */
    public boolean validateAddress(String address) {
        if (address == null || address.isEmpty()) {
            return false;
        }
        // Sprawdza czy adres zawiera jakąkolwiek cyfrę (numer domu)
        return address.matches(".*\\d+.*");
    }

    /**
     * Odnawia token sesji (powinno się wywoływać po każdym wybraniu adresu)
     */
    public void renewSessionToken() {
        this.token = AutocompleteSessionToken.newInstance();
    }
}