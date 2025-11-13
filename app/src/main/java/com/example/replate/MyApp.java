package com.example.replate;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;

public class MyApp extends Application {
    public static PlacesClient placesClient;

    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        if (!Places.isInitialized()) {
            Places.initialize(this, "AIzaSyA7Mm6hV5yGvXd3sn8ZqM_z9nxcfxzc2y8");
        }
        placesClient = Places.createClient(this);
    }
}

