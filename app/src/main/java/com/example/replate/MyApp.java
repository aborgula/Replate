package com.example.replate;

import android.app.Application;

import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;

public class MyApp extends Application {
    public static PlacesClient placesClient;

    @Override
    public void onCreate() {
        super.onCreate();
        if (!Places.isInitialized()) {
            Places.initialize(this, "API_KEY");
        }
        placesClient = Places.createClient(this);
    }
}

